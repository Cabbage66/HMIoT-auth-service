"""
统一UEBA异常检测模型 - 多维度特征融合
使用单一Isolation Forest模型对所有数据维度进行联合异常检测
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import RobustScaler
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score, confusion_matrix, classification_report
from collections import Counter
import joblib
import os
import warnings
warnings.filterwarnings('ignore')


class UnifiedUEBADetector:
    """统一的UEBA异常检测器 - 融合logon、device、file、http、email五个维度"""
    
    def __init__(self):
        self.model = None
        self.scaler = RobustScaler()
        self.feature_columns = []
        self.thresholds = {'low': None, 'medium': None, 'high': None}
        
    def load_and_merge_data(self, data_dir, min_activity=5):
        """
        加载并合并所有数据源
        
        参数:
            data_dir: 数据目录路径
            min_activity: 最小活动次数阈值
        """
        print("\n" + "=" * 70)
        print("加载多维度数据...")
        print("=" * 70)
        
        # 1. 加载各维度数据
        logon_df = pd.read_csv(f'{data_dir}/logon.csv')
        device_df = pd.read_csv(f'{data_dir}/device.csv')
        file_df = pd.read_csv(f'{data_dir}/file.csv')
        
        print(f"✓ logon.csv: {len(logon_df)} 条记录")
        print(f"✓ device.csv: {len(device_df)} 条记录")
        print(f"✓ file.csv: {len(file_df)} 条记录")
        
        # HTTP和Email文件加载全部数据
        try:
            http_df = pd.read_csv(f'{data_dir}/http.csv')
            print(f"✓ http.csv: {len(http_df)} 条记录")
        except:
            print("✗ http.csv 加载失败，跳过")
            http_df = pd.DataFrame({'user': [], 'date': [], 'url': []})
        
        try:
            email_df = pd.read_csv(f'{data_dir}/email.csv')
            print(f"✓ email.csv: {len(email_df)} 条记录")
        except:
            print("✗ email.csv 加载失败，跳过")
            email_df = pd.DataFrame({'user': [], 'date': []})
        
        print("\n提取特征...")
        
        # 2. 提取各维度特征
        logon_features = self.extract_logon_features(logon_df, min_activity)
        print(f"✓ Logon特征: {len(logon_features)} 用户")
        
        device_features = self.extract_device_features(device_df, min_activity)
        print(f"✓ Device特征: {len(device_features)} 用户")
        
        file_features = self.extract_file_features(file_df, min_activity)
        print(f"✓ File特征: {len(file_features)} 用户")
        
        http_features = self.extract_http_features(http_df, min_activity)
        print(f"✓ HTTP特征: {len(http_features)} 用户")
        
        email_features = self.extract_email_features(email_df, min_activity)
        print(f"✓ Email特征: {len(email_features)} 用户")
        
        # 3. 按用户合并特征
        print("\n合并特征...")
        merged_features = logon_features.copy()
        
        for df in [device_features, file_features, http_features, email_features]:
            if len(df) > 0:
                merged_features = merged_features.merge(df, on='user', how='outer')
        
        # 4. 填充缺失值（某些用户在某些维度没有活动）
        merged_features = merged_features.fillna(0)
        
        print(f"\n✓ 合并完成: {len(merged_features)} 用户, {len(merged_features.columns)-1} 维特征")
        
        return merged_features
    
    def extract_logon_features(self, df, min_sessions=10):
        """提取登录行为特征"""
        df['date'] = pd.to_datetime(df['date'])
        df = df.sort_values(['user', 'date'])
        
        features_list = []
        
        for user, user_data in df.groupby('user'):
            if len(user_data) < min_sessions:
                continue
                
            features = {'user': user}
            
            # 基础统计
            features['logon_total'] = len(user_data)
            features['logon_unique_pcs'] = user_data['pc'].nunique()
            features['logon_unique_days'] = user_data['date'].dt.date.nunique()
            features['logon_per_day'] = features['logon_total'] / max(features['logon_unique_days'], 1)
            
            # 时间模式
            user_data['hour'] = user_data['date'].dt.hour
            user_data['day_of_week'] = user_data['date'].dt.dayofweek
            
            features['logon_night_ratio'] = ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean()
            features['logon_weekend_ratio'] = user_data['day_of_week'].isin([5, 6]).mean()
            features['logon_work_hours_ratio'] = ((user_data['hour'] >= 9) & (user_data['hour'] <= 17)).mean()
            
            # 时间熵
            hour_dist = user_data['hour'].value_counts(normalize=True)
            features['logon_hour_entropy'] = -np.sum(hour_dist * np.log2(hour_dist + 1e-10))
            
            # 时间间隔
            time_diffs = user_data['date'].diff().dt.total_seconds() / 3600
            time_diffs = time_diffs[time_diffs > 0]
            
            if len(time_diffs) > 0:
                features['logon_avg_interval'] = time_diffs.mean()
                features['logon_std_interval'] = time_diffs.std()
                features['logon_short_interval_ratio'] = (time_diffs < 0.5).mean()
                features['logon_long_interval_ratio'] = (time_diffs > 48).mean()
            else:
                features['logon_avg_interval'] = 0
                features['logon_std_interval'] = 0
                features['logon_short_interval_ratio'] = 0
                features['logon_long_interval_ratio'] = 0
            
            # PC使用模式
            pc_counts = user_data['pc'].value_counts()
            features['logon_primary_pc_ratio'] = pc_counts.iloc[0] / len(user_data) if len(pc_counts) > 0 else 1.0
            features['logon_pc_diversity'] = user_data['pc'].nunique() / len(user_data)
            
            features_list.append(features)
        
        return pd.DataFrame(features_list)
    
    def extract_device_features(self, df, min_events=5):
        """提取设备使用特征"""
        df['date'] = pd.to_datetime(df['date'])
        features_list = []
        
        for user, user_data in df.groupby('user'):
            if len(user_data) < min_events:
                continue
                
            features = {'user': user}
            
            # 基础统计
            features['device_total_events'] = len(user_data)
            features['device_unique_days'] = user_data['date'].dt.date.nunique()
            features['device_per_day'] = features['device_total_events'] / max(features['device_unique_days'], 1)
            
            # Connect/Disconnect统计
            connect_count = (user_data['activity'] == 'Connect').sum()
            disconnect_count = (user_data['activity'] == 'Disconnect').sum()
            features['device_connect_count'] = connect_count
            features['device_connect_ratio'] = connect_count / max(len(user_data), 1)
            
            # 时间模式
            user_data['hour'] = user_data['date'].dt.hour
            features['device_night_ratio'] = ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean()
            features['device_weekend_ratio'] = user_data['date'].dt.dayofweek.isin([5, 6]).mean()
            
            # 时间间隔
            connect_times = user_data[user_data['activity'] == 'Connect']['date']
            if len(connect_times) > 1:
                time_diffs = connect_times.diff().dt.total_seconds() / 3600
                time_diffs = time_diffs[time_diffs > 0]
                if len(time_diffs) > 0:
                    features['device_avg_interval'] = time_diffs.mean()
                    features['device_burst_score'] = (time_diffs < 1).sum()
                else:
                    features['device_avg_interval'] = 0
                    features['device_burst_score'] = 0
            else:
                features['device_avg_interval'] = 0
                features['device_burst_score'] = 0
            
            features_list.append(features)
        
        return pd.DataFrame(features_list)
    
    def extract_file_features(self, df, min_files=5):
        """提取文件操作特征"""
        df['date'] = pd.to_datetime(df['date'])
        features_list = []
        
        for user, user_data in df.groupby('user'):
            if len(user_data) < min_files:
                continue
                
            features = {'user': user}
            
            # 基础统计
            features['file_total'] = len(user_data)
            features['file_unique_days'] = user_data['date'].dt.date.nunique()
            features['file_per_day'] = features['file_total'] / max(features['file_unique_days'], 1)
            
            # 文件类型
            if 'filename' in user_data.columns:
                user_data['file_type'] = user_data['filename'].str.split('.').str[-1].str.lower()
                file_types = user_data['file_type'].value_counts()
                features['file_unique_types'] = len(file_types)
                features['file_type_diversity'] = len(file_types) / max(len(user_data), 1)
            else:
                features['file_unique_types'] = 0
                features['file_type_diversity'] = 0
            
            # 时间模式
            user_data['hour'] = user_data['date'].dt.hour
            features['file_night_ratio'] = ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean()
            features['file_weekend_ratio'] = user_data['date'].dt.dayofweek.isin([5, 6]).mean()
            
            # 时间间隔
            time_diffs = user_data['date'].diff().dt.total_seconds() / 60
            time_diffs = time_diffs[time_diffs > 0]
            if len(time_diffs) > 0:
                features['file_avg_interval'] = time_diffs.mean()
                features['file_burst_score'] = (time_diffs < 5).sum()
            else:
                features['file_avg_interval'] = 0
                features['file_burst_score'] = 0
            
            features_list.append(features)
        
        return pd.DataFrame(features_list)
    
    def extract_http_features(self, df, min_visits=10):
        """提取HTTP访问特征"""
        if len(df) == 0:
            return pd.DataFrame({'user': []})
            
        df['date'] = pd.to_datetime(df['date'])
        features_list = []
        
        for user, user_data in df.groupby('user'):
            if len(user_data) < min_visits:
                continue
                
            features = {'user': user}
            
            # 基础统计
            features['http_total_visits'] = len(user_data)
            features['http_unique_days'] = user_data['date'].dt.date.nunique()
            features['http_per_day'] = features['http_total_visits'] / max(features['http_unique_days'], 1)
            
            # URL多样性
            if 'url' in user_data.columns:
                features['http_unique_urls'] = user_data['url'].nunique()
                features['http_url_diversity'] = features['http_unique_urls'] / max(len(user_data), 1)
            else:
                features['http_unique_urls'] = 0
                features['http_url_diversity'] = 0
            
            # 时间模式
            user_data['hour'] = user_data['date'].dt.hour
            features['http_night_ratio'] = ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean()
            features['http_weekend_ratio'] = user_data['date'].dt.dayofweek.isin([5, 6]).mean()
            
            # 访问频率
            time_diffs = user_data['date'].diff().dt.total_seconds()
            time_diffs = time_diffs[time_diffs > 0]
            if len(time_diffs) > 0:
                features['http_avg_interval'] = time_diffs.mean()
                features['http_rapid_browsing_ratio'] = (time_diffs < 10).mean()
            else:
                features['http_avg_interval'] = 0
                features['http_rapid_browsing_ratio'] = 0
            
            features_list.append(features)
        
        return pd.DataFrame(features_list)
    
    def extract_email_features(self, df, min_emails=5):
        """提取邮件行为特征"""
        if len(df) == 0:
            return pd.DataFrame({'user': []})
            
        df['date'] = pd.to_datetime(df['date'])
        features_list = []
        
        for user, user_data in df.groupby('user'):
            if len(user_data) < min_emails:
                continue
                
            features = {'user': user}
            
            # 基础统计
            features['email_total'] = len(user_data)
            features['email_unique_days'] = user_data['date'].dt.date.nunique()
            features['email_per_day'] = features['email_total'] / max(features['email_unique_days'], 1)
            
            # 邮件属性
            if 'size' in user_data.columns:
                features['email_avg_size'] = user_data['size'].mean()
                features['email_total_size'] = user_data['size'].sum()
            else:
                features['email_avg_size'] = 0
                features['email_total_size'] = 0
            
            if 'attachment_count' in user_data.columns:
                features['email_attachment_ratio'] = (user_data['attachment_count'] > 0).mean()
                features['email_avg_attachments'] = user_data['attachment_count'].mean()
            else:
                features['email_attachment_ratio'] = 0
                features['email_avg_attachments'] = 0
            
            # 时间模式
            user_data['hour'] = user_data['date'].dt.hour
            features['email_night_ratio'] = ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean()
            features['email_weekend_ratio'] = user_data['date'].dt.dayofweek.isin([5, 6]).mean()
            
            features_list.append(features)
        
        return pd.DataFrame(features_list)
    
    def train(self, features_df, contamination=0.1):
        """训练统一模型"""
        print("\n" + "=" * 70)
        print("训练统一UEBA异常检测模型...")
        print("=" * 70)
        
        # 分离用户ID和特征
        users = features_df['user'].values
        X = features_df.drop('user', axis=1)
        self.feature_columns = X.columns.tolist()
        
        # 处理无穷值和缺失值
        X = X.replace([np.inf, -np.inf], np.nan)
        X = X.fillna(0)
        
        print(f"\n特征维度: {X.shape}")
        print(f"用户数量: {len(users)}")
        
        # 标准化
        print("标准化特征...")
        X_scaled = self.scaler.fit_transform(X)
        
        # 训练Isolation Forest
        print("训练Isolation Forest模型...")
        self.model = IsolationForest(
            contamination=contamination,
            n_estimators=300,
            max_samples='auto',
            max_features=0.8,
            random_state=42,
            n_jobs=-1,
            verbose=0
        )
        
        self.model.fit(X_scaled)
        
        # 计算异常分数
        print("计算异常分数...")
        anomaly_scores = self.model.score_samples(X_scaled)
        
        # 设置阈值
        self.thresholds['low'] = np.percentile(anomaly_scores, 25)
        self.thresholds['medium'] = np.percentile(anomaly_scores, 10)
        self.thresholds['high'] = np.percentile(anomaly_scores, 5)
        
        print("\n风险阈值:")
        print(f"  正常: > {self.thresholds['low']:.6f}")
        print(f"  低风险: {self.thresholds['medium']:.6f} ~ {self.thresholds['low']:.6f}")
        print(f"  中风险: {self.thresholds['high']:.6f} ~ {self.thresholds['medium']:.6f}")
        print(f"  高风险: < {self.thresholds['high']:.6f}")
        
        # 分类风险
        risk_levels = self.classify_risk(anomaly_scores)
        
        print("\n风险分布:")
        for level in ['正常', '低风险', '中风险', '高风险']:
            count = np.sum(risk_levels == level)
            percentage = count / len(risk_levels) * 100
            print(f"  {level}: {count} ({percentage:.1f}%)")
        
        return users, anomaly_scores, risk_levels
    
    def classify_risk(self, scores):
        """分类风险等级"""
        risk_levels = np.empty(len(scores), dtype=object)
        risk_levels[scores > self.thresholds['low']] = '正常'
        risk_levels[(scores <= self.thresholds['low']) & (scores > self.thresholds['medium'])] = '低风险'
        risk_levels[(scores <= self.thresholds['medium']) & (scores > self.thresholds['high'])] = '中风险'
        risk_levels[scores <= self.thresholds['high']] = '高风险'
        return risk_levels
    
    def save_model(self, filepath):
        """保存模型"""
        model_data = {
            'model': self.model,
            'scaler': self.scaler,
            'feature_columns': self.feature_columns,
            'thresholds': self.thresholds
        }
        joblib.dump(model_data, filepath)
        print(f"\n✓ 模型已保存: {filepath}")
    
    def load_model(self, filepath):
        """加载模型"""
        model_data = joblib.load(filepath)
        self.model = model_data['model']
        self.scaler = model_data['scaler']
        self.feature_columns = model_data['feature_columns']
        self.thresholds = model_data['thresholds']
        print(f"✓ 模型已加载: {filepath}")


def load_ground_truth(insiders_file, dataset='4.2'):
    """
    加载真实标签数据
    
    参数:
        insiders_file: insiders.csv文件路径
        dataset: 数据集版本
    
    返回:
        内部威胁用户ID集合
    """
    df = pd.read_csv(insiders_file)
    # 过滤出指定数据集的内部威胁
    insiders = df[df['dataset'] == float(dataset)]['user'].unique()
    return set(insiders)


def evaluate_model(results_df, insiders, output_dir):
    """
    评估模型性能
    
    参数:
        results_df: 检测结果DataFrame (user, anomaly_score, risk_level)
        insiders: 真实内部威胁用户ID集合
        output_dir: 输出目录
    """
    print("\n" + "=" * 70)
    print("模型性能评估")
    print("=" * 70)
    
    # 1. 创建真实标签
    y_true = results_df['user'].apply(lambda x: 1 if x in insiders else 0).values
    
    # 2. 创建预测标签（将非正常用户视为异常）
    y_pred = results_df['risk_level'].apply(lambda x: 1 if x != '正常' else 0).values
    
    # 3. 基本统计
    total_users = len(results_df)
    actual_insiders = np.sum(y_true)
    detected_anomalies = np.sum(y_pred)
    
    print(f"\n数据集统计:")
    print(f"  总用户数: {total_users}")
    print(f"  真实内部威胁用户数: {actual_insiders}")
    print(f"  检测为异常的用户数: {detected_anomalies}")
    print(f"  内部威胁比例: {actual_insiders/total_users*100:.2f}%")
    
    # 4. 计算评估指标
    accuracy = accuracy_score(y_true, y_pred)
    precision = precision_score(y_true, y_pred, zero_division=0)
    recall = recall_score(y_true, y_pred, zero_division=0)
    f1 = f1_score(y_true, y_pred, zero_division=0)
    
    print(f"\n性能指标:")
    print(f"  准确率 (Accuracy):  {accuracy:.4f} ({accuracy*100:.2f}%)")
    print(f"  精确率 (Precision): {precision:.4f} ({precision*100:.2f}%)")
    print(f"  召回率 (Recall):    {recall:.4f} ({recall*100:.2f}%)")
    print(f"  F1分数 (F1-Score):  {f1:.4f}")
    
    # 5. 混淆矩阵
    cm = confusion_matrix(y_true, y_pred)
    tn, fp, fn, tp = cm.ravel()
    
    print(f"\n混淆矩阵:")
    print(f"  {'':>15} {'预测正常':>12} {'预测异常':>12}")
    print(f"  {'实际正常':>15} {tn:>12} {fp:>12}")
    print(f"  {'实际异常':>15} {fn:>12} {tp:>12}")
    
    print(f"\n详细分析:")
    print(f"  真正例 (TP): {tp} - 正确识别的内部威胁")
    print(f"  假正例 (FP): {fp} - 误判为异常的正常用户")
    print(f"  真负例 (TN): {tn} - 正确识别的正常用户")
    print(f"  假负例 (FN): {fn} - 漏检的内部威胁")
    
    # 6. 不同风险等级的检测效果
    print(f"\n不同风险等级的内部威胁分布:")
    for risk_level in ['高风险', '中风险', '低风险', '正常']:
        level_users = results_df[results_df['risk_level'] == risk_level]['user']
        insiders_in_level = sum(1 for u in level_users if u in insiders)
        total_in_level = len(level_users)
        if total_in_level > 0:
            percentage = insiders_in_level / total_in_level * 100
            print(f"  {risk_level}: {insiders_in_level}/{total_in_level} ({percentage:.1f}%)")
    
    # 7. 检测到的内部威胁详情
    detected_insiders = results_df[
        (results_df['user'].isin(insiders)) & 
        (results_df['risk_level'] != '正常')
    ].sort_values('anomaly_score')
    
    print(f"\n✓ 成功检测到的内部威胁用户 ({len(detected_insiders)}/{actual_insiders}):")
    print("-" * 70)
    if len(detected_insiders) > 0:
        print(detected_insiders[['user', 'anomaly_score', 'risk_level']].to_string(index=False))
    
    # 8. 漏检的内部威胁
    missed_insiders = results_df[
        (results_df['user'].isin(insiders)) & 
        (results_df['risk_level'] == '正常')
    ]
    
    if len(missed_insiders) > 0:
        print(f"\n✗ 漏检的内部威胁用户 ({len(missed_insiders)}/{actual_insiders}):")
        print("-" * 70)
        print(missed_insiders[['user', 'anomaly_score', 'risk_level']].to_string(index=False))
    
    # 9. 保存评估报告
    eval_report = {
        'total_users': total_users,
        'actual_insiders': actual_insiders,
        'detected_anomalies': detected_anomalies,
        'accuracy': accuracy,
        'precision': precision,
        'recall': recall,
        'f1_score': f1,
        'true_positive': int(tp),
        'false_positive': int(fp),
        'true_negative': int(tn),
        'false_negative': int(fn)
    }
    
    eval_df = pd.DataFrame([eval_report])
    eval_file = f'{output_dir}/model_evaluation.csv'
    eval_df.to_csv(eval_file, index=False)
    print(f"\n✓ 评估报告已保存: {eval_file}")
    
    # 10. 保存详细分类报告
    report = classification_report(y_true, y_pred, 
                                   target_names=['正常', '异常'],
                                   digits=4)
    report_file = f'{output_dir}/classification_report.txt'
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write("分类报告\n")
        f.write("=" * 70 + "\n\n")
        f.write(report)
        f.write("\n\n混淆矩阵:\n")
        f.write(f"{'':>15} {'预测正常':>12} {'预测异常':>12}\n")
        f.write(f"{'实际正常':>15} {tn:>12} {fp:>12}\n")
        f.write(f"{'实际异常':>15} {fn:>12} {tp:>12}\n")
    
    print(f"✓ 分类报告已保存: {report_file}")
    
    return eval_report


def main():
    """主函数"""
    import argparse

    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser = argparse.ArgumentParser(description='统一UEBA异常检测训练（Isolation Forest）')
    parser.add_argument('--data-dir', default=os.path.join(project_root, 'data', 'r4.2'),
                        help='r4.2数据目录（包含logon/device/file，http/email可选）')
    parser.add_argument('--insiders-file', default=os.path.join(project_root, 'data', 'answers', 'insiders.csv'),
                        help='真实标签insiders.csv路径')
    parser.add_argument('--output-dir', default=os.path.join(project_root, 'result2'),
                        help='输出目录（默认result2，避免覆盖历史结果）')
    parser.add_argument('--min-activity', type=int, default=5, help='最小活动次数阈值')
    parser.add_argument('--contamination', type=float, default=0.1, help='Isolation Forest异常比例')
    parser.add_argument('--skip-eval', action='store_true', help='跳过评估阶段')
    args = parser.parse_args()

    print("\n" + "=" * 70)
    print("        统一UEBA异常检测系统 - 多维度特征融合")
    print("=" * 70)
    print(f"数据目录: {args.data_dir}")
    print(f"输出目录: {args.output_dir}")
    
    # 1. 初始化检测器
    detector = UnifiedUEBADetector()
    
    # 2. 加载并合并数据
    features_df = detector.load_and_merge_data(args.data_dir, min_activity=args.min_activity)
    
    if len(features_df) == 0:
        print("\n错误: 没有足够的数据进行训练")
        return None, None
    
    # 3. 训练模型
    users, scores, risk_levels = detector.train(features_df, contamination=args.contamination)
    
    # 4. 创建输出目录
    output_dir = args.output_dir
    os.makedirs(output_dir, exist_ok=True)
    
    # 5. 保存结果
    results_df = pd.DataFrame({
        'user': users,
        'anomaly_score': scores,
        'risk_level': risk_levels
    })
    
    output_file = f'{output_dir}/unified_anomaly_results.csv'
    results_df.to_csv(output_file, index=False)
    print(f"\n✓ 检测结果已保存: {output_file}")
    
    # 6. 加载真实标签并评估模型
    eval_report = None
    if args.skip_eval:
        print("\n已跳过评估（--skip-eval）")
    elif os.path.exists(args.insiders_file):
        try:
            insiders = load_ground_truth(args.insiders_file, dataset='4.2')
            print(f"\n✓ 已加载真实标签: {len(insiders)} 个内部威胁用户")
            eval_report = evaluate_model(results_df, insiders, output_dir)
        except Exception as e:
            print(f"\n警告: 无法加载真实标签或评估失败: {e}")
            eval_report = None
    else:
        print(f"\n警告: 评估文件不存在，跳过评估: {args.insiders_file}")
    
    # 7. 保存模型
    model_file = f'{output_dir}/unified_anomaly_model.joblib'
    detector.save_model(model_file)
    
    # 8. 显示高风险用户
    high_risk = results_df[results_df['risk_level'].isin(['高风险', '中风险'])].sort_values('anomaly_score')
    print(f"\n高风险和中风险用户 (前20个):")
    print("-" * 70)
    print(high_risk.head(20).to_string(index=False))
    
    # 9. 生成详细统计
    print("\n" + "=" * 70)
    print("检测统计摘要")
    print("=" * 70)
    print(f"总用户数: {len(results_df)}")
    print(f"特征维度: {len(detector.feature_columns)}")
    print(f"异常用户数: {(results_df['risk_level'] != '正常').sum()}")
    print(f"高风险用户数: {(results_df['risk_level'] == '高风险').sum()}")
    
    if eval_report:
        print(f"\n模型性能:")
        print(f"  准确率: {eval_report['accuracy']*100:.2f}%")
        print(f"  精确率: {eval_report['precision']*100:.2f}%")
        print(f"  召回率: {eval_report['recall']*100:.2f}%")
        print(f"  F1分数: {eval_report['f1_score']:.4f}")
    
    print("\n" + "=" * 70)
    print("统一UEBA检测完成！")
    print("=" * 70)
    
    return detector, results_df


if __name__ == "__main__":
    detector, results = main()
