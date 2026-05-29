"""
增量One-Class SVM异常检测器
使用SGDOneClassSVM实现增量学习，适配r4.2数据集
"""

import pandas as pd
import numpy as np
from sklearn.linear_model import SGDOneClassSVM
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score, confusion_matrix
import joblib
import warnings
import os
from datetime import datetime
warnings.filterwarnings('ignore')


class IncrementalOCSVMDetector:
    """增量One-Class SVM异常检测器"""
    
    def __init__(self, nu=0.5, learning_rate='constant', max_iter=1000, eta0=0.01):
        """
        初始化检测器
        
        参数:
            nu: 异常比例估计 (0-1之间，推荐0.5)
            learning_rate: 学习率策略
            max_iter: 每批次最大迭代次数
            eta0: 初始学习率
        """
        self.model = SGDOneClassSVM(
            nu=nu,
            shuffle=True,
            learning_rate=learning_rate,
            max_iter=max_iter,
            eta0=eta0,
            tol=1e-4,
            random_state=42,
            warm_start=True  # 允许增量训练
        )
        self.scaler = StandardScaler()
        self.feature_columns = []
        self.is_fitted = False
        self.batch_count = 0
        self.nu = nu
        
    def load_and_merge_data(self, data_dir):
        """
        加载并合并所有数据源
        
        参数:
            data_dir: 数据目录路径
        
        返回:
            合并后的特征DataFrame
        """
        print("=" * 70)
        print("加载数据...")
        print("=" * 70)
        
        # 1. 加载logon数据并提取特征
        print("\n1. 处理Logon数据...")
        logon_features = self._extract_logon_features(f'{data_dir}/logon.csv')
        
        # 2. 加载device数据并提取特征
        print("\n2. 处理Device数据...")
        device_features = self._extract_device_features(f'{data_dir}/device.csv')
        
        # 3. 加载file数据并提取特征
        print("\n3. 处理File数据...")
        file_features = self._extract_file_features(f'{data_dir}/file.csv')
        
        # 4. 加载http数据并提取特征
        print("\n4. 处理HTTP数据...")
        http_features = self._extract_http_features(f'{data_dir}/http.csv')
        
        # 5. 加载email数据并提取特征
        print("\n5. 处理Email数据...")
        email_features = self._extract_email_features(f'{data_dir}/email.csv')
        
        # 6. 合并所有特征
        print("\n6. 合并特征...")
        merged_df = logon_features.copy()
        
        for df in [device_features, file_features, http_features, email_features]:
            merged_df = merged_df.merge(df, on='user', how='outer')
        
        # 填充缺失值
        merged_df = merged_df.fillna(0)
        
        print(f"\n✓ 数据加载完成:")
        print(f"  - 总用户数: {len(merged_df)}")
        print(f"  - 总特征数: {len(merged_df.columns) - 1}")
        
        return merged_df
    
    def _extract_logon_features(self, file_path):
        """提取Logon特征（增强版）"""
        df = pd.read_csv(file_path)
        df['date'] = pd.to_datetime(df['date'])
        
        features_list = []
        for user, user_data in df.groupby('user'):
            if len(user_data) < 5:
                continue
            
            user_data['hour'] = user_data['date'].dt.hour
            user_data['day_of_week'] = user_data['date'].dt.dayofweek
            
            # 时间间隔特征
            time_diffs = user_data['date'].diff().dt.total_seconds() / 3600
            time_diffs = time_diffs[time_diffs > 0]
            
            # PC使用频率
            pc_counts = user_data['pc'].value_counts()
            
            features = {
                'user': user,
                'logon_count': len(user_data),
                'logon_unique_pcs': user_data['pc'].nunique(),
                'logon_night_ratio': ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean(),
                'logon_weekend_ratio': user_data['day_of_week'].isin([5, 6]).mean(),
                'logon_pc_diversity': user_data['pc'].nunique() / max(len(user_data), 1),
                'logon_avg_interval': time_diffs.mean() if len(time_diffs) > 0 else 0,
                'logon_std_interval': time_diffs.std() if len(time_diffs) > 0 else 0,
                'logon_max_pc_usage': pc_counts.max() / len(user_data) if len(pc_counts) > 0 else 0,
                'logon_after_hours_ratio': ((user_data['hour'] >= 18) | (user_data['hour'] < 9)).mean(),
            }
            features_list.append(features)
        
        print(f"  提取了 {len(features_list)} 个用户的Logon特征")
        return pd.DataFrame(features_list)
    
    def _extract_device_features(self, file_path):
        """提取Device特征（增强版）"""
        df = pd.read_csv(file_path)
        df['date'] = pd.to_datetime(df['date'])
        
        features_list = []
        for user, user_data in df.groupby('user'):
            if len(user_data) < 3:
                continue
            
            user_data['hour'] = user_data['date'].dt.hour
            connect_data = user_data[user_data['activity'] == 'Connect']
            connect_count = len(connect_data)
            
            # 时间间隔
            if len(connect_data) > 1:
                time_diffs = connect_data['date'].diff().dt.total_seconds() / 3600
                time_diffs = time_diffs[time_diffs > 0]
                avg_interval = time_diffs.mean() if len(time_diffs) > 0 else 0
                burst_ratio = (time_diffs < 1).mean() if len(time_diffs) > 0 else 0
            else:
                avg_interval = 0
                burst_ratio = 0
            
            features = {
                'user': user,
                'device_events': len(user_data),
                'device_connects': connect_count,
                'device_night_ratio': ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean(),
                'device_pc_count': user_data['pc'].nunique(),
                'device_avg_interval': avg_interval,
                'device_burst_ratio': burst_ratio,
                'device_after_hours': ((user_data['hour'] >= 18) | (user_data['hour'] < 9)).mean(),
            }
            features_list.append(features)
        
        print(f"  提取了 {len(features_list)} 个用户的Device特征")
        return pd.DataFrame(features_list)
    
    def _extract_file_features(self, file_path):
        """提取File特征（增强版）"""
        df = pd.read_csv(file_path)
        df['date'] = pd.to_datetime(df['date'])
        
        features_list = []
        for user, user_data in df.groupby('user'):
            if len(user_data) < 3:
                continue
            
            user_data['hour'] = user_data['date'].dt.hour
            
            # 文件扩展名分析
            user_data['extension'] = user_data['filename'].str.split('.').str[-1]
            
            features = {
                'user': user,
                'file_operations': len(user_data),
                'file_unique_files': user_data['filename'].nunique(),
                'file_night_ratio': ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean(),
                'file_pc_count': user_data['pc'].nunique(),
                'file_unique_extensions': user_data['extension'].nunique(),
                'file_ops_per_day': len(user_data) / max(user_data['date'].dt.date.nunique(), 1),
            }
            features_list.append(features)
        
        print(f"  提取了 {len(features_list)} 个用户的File特征")
        return pd.DataFrame(features_list)
    
    def _extract_http_features(self, file_path):
        """提取HTTP特征（增强版）"""
        df = pd.read_csv(file_path)
        df['date'] = pd.to_datetime(df['date'])
        
        features_list = []
        for user, user_data in df.groupby('user'):
            if len(user_data) < 3:
                continue
            
            user_data['hour'] = user_data['date'].dt.hour
            
            # URL访问频率
            url_counts = user_data['url'].value_counts()
            
            features = {
                'user': user,
                'http_requests': len(user_data),
                'http_unique_urls': user_data['url'].nunique(),
                'http_night_ratio': ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean(),
                'http_pc_count': user_data['pc'].nunique(),
                'http_url_diversity': user_data['url'].nunique() / max(len(user_data), 1),
                'http_max_url_freq': url_counts.max() / len(user_data) if len(url_counts) > 0 else 0,
                'http_requests_per_day': len(user_data) / max(user_data['date'].dt.date.nunique(), 1),
            }
            features_list.append(features)
        
        print(f"  提取了 {len(features_list)} 个用户的HTTP特征")
        return pd.DataFrame(features_list)
    
    def _extract_email_features(self, file_path):
        """提取Email特征（增强版）"""
        df = pd.read_csv(file_path)
        df['date'] = pd.to_datetime(df['date'])
        
        features_list = []
        for user, user_data in df.groupby('user'):
            if len(user_data) < 3:
                continue
            
            user_data['hour'] = user_data['date'].dt.hour
            sent = user_data[user_data['to'].notna()]
            received = user_data[user_data['to'].isna()]
            
            # 邮件大小分析
            size_stats = user_data['size'].describe() if 'size' in user_data.columns else None
            
            features = {
                'user': user,
                'email_count': len(user_data),
                'email_sent': len(sent),
                'email_received': len(received),
                'email_unique_to': user_data['to'].nunique() if len(sent) > 0 else 0,
                'email_night_ratio': ((user_data['hour'] >= 22) | (user_data['hour'] < 6)).mean(),
                'email_sent_ratio': len(sent) / max(len(user_data), 1),
                'email_per_day': len(user_data) / max(user_data['date'].dt.date.nunique(), 1),
            }
            
            if size_stats is not None:
                features['email_avg_size'] = size_stats['mean']
                features['email_max_size'] = size_stats['max']
            
            features_list.append(features)
        
        print(f"  提取了 {len(features_list)} 个用户的Email特征")
        return pd.DataFrame(features_list)
    
    def train_incremental(self, features_df, batch_size=100):
        """
        增量训练模型
        
        参数:
            features_df: 特征DataFrame
            batch_size: 每批次样本数量
        
        返回:
            users, scores, risk_levels
        """
        print("\n" + "=" * 70)
        print("增量训练One-Class SVM模型...")
        print("=" * 70)
        
        users = features_df['user'].values
        X = features_df.drop('user', axis=1).values
        
        self.feature_columns = features_df.columns.drop('user').tolist()
        print(f"特征维度: {X.shape[1]}")
        print(f"训练样本数: {X.shape[0]}")
        print(f"批次大小: {batch_size}")
        
        # 标准化特征
        print("\n标准化特征...")
        X_scaled = self.scaler.fit_transform(X)
        
        # 增量训练
        n_samples = X_scaled.shape[0]
        n_batches = (n_samples + batch_size - 1) // batch_size
        
        print(f"\n开始增量训练 ({n_batches} 个批次)...")
        
        for i in range(n_batches):
            start_idx = i * batch_size
            end_idx = min((i + 1) * batch_size, n_samples)
            X_batch = X_scaled[start_idx:end_idx]
            
            # 增量训练
            self.model.partial_fit(X_batch)
            self.batch_count += 1
            
            if (i + 1) % 5 == 0 or (i + 1) == n_batches:
                print(f"  批次 {i+1}/{n_batches} 完成 (样本 {end_idx}/{n_samples})")
        
        self.is_fitted = True
        print(f"\n✓ 模型训练完成 (共 {self.batch_count} 个批次)")
        
        # 预测
        print("\n计算异常分数...")
        scores = self.model.score_samples(X_scaled)
        predictions = self.model.predict(X_scaled)
        
        print(f"\n分数统计:")
        print(f"  最小分数: {scores.min():.4f}")
        print(f"  最大分数: {scores.max():.4f}")
        print(f"  平均分数: {scores.mean():.4f}")
        print(f"  标准差: {scores.std():.4f}")
        print(f"\nPredict结果统计:")
        print(f"  正常 (1): {(predictions == 1).sum()}")
        print(f"  异常 (-1): {(predictions == -1).sum()}")
        
        # 风险等级分类（使用分位数方法，不依赖predict结果）
        print("\n分类风险等级...")
        risk_levels = self._classify_risk_levels(scores, predictions)
        
        # 统计
        unique, counts = np.unique(risk_levels, return_counts=True)
        print(f"\n风险等级分布:")
        for level, count in zip(unique, counts):
            print(f"  {level}: {count} ({count/len(risk_levels)*100:.1f}%)")
        
        return users, scores, risk_levels
    
    def _classify_risk_levels(self, scores, predictions):
        """
        分类风险等级
        使用实验#10最优阈值配置
        """
        risk_levels = []
        
        # 实验#10最优阈值：4.5%/11%/24%
        # 较低的分数表示更异常
        q4_5 = np.percentile(scores, 4.5)   # 前4.5%最异常 -> 高风险
        q11 = np.percentile(scores, 11)     # 前11%异常 -> 中风险
        q24 = np.percentile(scores, 24)     # 前24%异常 -> 低风险
        
        print(f"\n分位数阈值 (实验#10最优配置):")
        print(f"  高风险阈值 (4.5%):  {q4_5:.4f}")
        print(f"  中风险阈值 (11%):   {q11:.4f}")
        print(f"  低风险阈值 (24%):   {q24:.4f}")
        
        for score in scores:
            if score < q4_5:
                risk_levels.append('高风险')
            elif score < q11:
                risk_levels.append('中风险')
            elif score < q24:
                risk_levels.append('低风险')
            else:
                risk_levels.append('正常')
        
        return np.array(risk_levels)
    
    def evaluate_model(self, results_df, insider_labels_path, output_dir):
        """
        评估模型性能
        
        参数:
            results_df: 检测结果DataFrame
            insider_labels_path: 真实标签文件路径
            output_dir: 输出目录
        """
        print("\n" + "=" * 70)
        print("模型性能评估")
        print("=" * 70)
        
        try:
            # 加载真实标签
            insiders_df = pd.read_csv(insider_labels_path)
            insiders = set(insiders_df[insiders_df['dataset'] == 4.2]['user'].unique())
            
            print(f"\n真实内部威胁用户数: {len(insiders)}")
            
            # 创建标签
            y_true = results_df['user'].apply(lambda x: 1 if x in insiders else 0).values
            y_pred = results_df['risk_level'].apply(lambda x: 1 if x != '正常' else 0).values
            
            # 计算指标
            accuracy = accuracy_score(y_true, y_pred)
            precision = precision_score(y_true, y_pred, zero_division=0)
            recall = recall_score(y_true, y_pred, zero_division=0)
            f1 = f1_score(y_true, y_pred, zero_division=0)
            
            print(f"\n性能指标:")
            print(f"  准确率 (Accuracy):  {accuracy:.4f} ({accuracy*100:.2f}%)")
            print(f"  精确率 (Precision): {precision:.4f} ({precision*100:.2f}%)")
            print(f"  召回率 (Recall):    {recall:.4f} ({recall*100:.2f}%)")
            print(f"  F1分数 (F1-Score):  {f1:.4f}")
            
            # 混淆矩阵
            cm = confusion_matrix(y_true, y_pred)
            tn, fp, fn, tp = cm.ravel()
            
            print(f"\n混淆矩阵:")
            print(f"  真正例 (TP): {tp}")
            print(f"  假正例 (FP): {fp}")
            print(f"  真负例 (TN): {tn}")
            print(f"  假负例 (FN): {fn}")
            
            # 保存评估结果
            eval_data = {
                'accuracy': [accuracy],
                'precision': [precision],
                'recall': [recall],
                'f1_score': [f1],
                'true_positive': [int(tp)],
                'false_positive': [int(fp)],
                'true_negative': [int(tn)],
                'false_negative': [int(fn)],
                'total_users': [len(results_df)],
                'actual_insiders': [int(np.sum(y_true))],
                'detected_anomalies': [int(np.sum(y_pred))]
            }
            
            eval_df = pd.DataFrame(eval_data)
            eval_file = f'{output_dir}/incremental_ocsvm_evaluation.csv'
            eval_df.to_csv(eval_file, index=False)
            print(f"\n✓ 评估结果已保存: {eval_file}")
            
            # 显示检测到的内部威胁
            detected_insiders = results_df[
                (results_df['user'].isin(insiders)) & 
                (results_df['risk_level'] != '正常')
            ].sort_values('anomaly_score')
            
            print(f"\n✓ 成功检测到的内部威胁用户 ({len(detected_insiders)}/{len(insiders)}):")
            if len(detected_insiders) > 0:
                print(detected_insiders[['user', 'anomaly_score', 'risk_level']].head(20).to_string(index=False))
            
            return eval_df
            
        except Exception as e:
            print(f"评估失败: {e}")
            import traceback
            traceback.print_exc()
            return None
    
    def save_model(self, filepath):
        """保存模型"""
        model_data = {
            'model': self.model,
            'scaler': self.scaler,
            'feature_columns': self.feature_columns,
            'batch_count': self.batch_count,
            'is_fitted': self.is_fitted
        }
        joblib.dump(model_data, filepath)
        print(f"\n✓ 模型已保存: {filepath}")
    
    def load_model(self, filepath):
        """加载模型"""
        model_data = joblib.load(filepath)
        self.model = model_data['model']
        self.scaler = model_data['scaler']
        self.feature_columns = model_data['feature_columns']
        self.batch_count = model_data['batch_count']
        self.is_fitted = model_data['is_fitted']
        print(f"✓ 模型已加载: {filepath}")


def main():
    """主函数"""
    import argparse

    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser = argparse.ArgumentParser(description='增量One-Class SVM异常检测训练')
    parser.add_argument('--data-dir', default=os.path.join(project_root, 'data', 'r4.2'),
                        help='r4.2数据目录（包含logon/device/file/http/email.csv）')
    parser.add_argument('--insiders-file', default=os.path.join(project_root, 'data', 'answers', 'insiders.csv'),
                        help='真实标签insiders.csv路径')
    parser.add_argument('--output-dir', default=os.path.join(project_root, 'result2'),
                        help='输出目录（默认result2，避免覆盖历史结果）')
    parser.add_argument('--batch-size', type=int, default=38, help='增量训练批次大小')
    parser.add_argument('--skip-eval', action='store_true', help='跳过评估阶段')
    args = parser.parse_args()

    print("\n" + "=" * 70)
    print("  增量One-Class SVM异常检测系统 - 多维度特征融合")
    print("=" * 70)
    print(f"数据目录: {args.data_dir}")
    print(f"输出目录: {args.output_dir}")
    
    # 1. 初始化检测器（使用实验#10最优参数）
    detector = IncrementalOCSVMDetector(
        nu=0.215,            # 最优nu值
        learning_rate='constant',  
        max_iter=11500,      # 最优迭代次数
        eta0=0.00034         # 最优学习率
    )
    
    # 2. 加载并合并数据
    features_df = detector.load_and_merge_data(args.data_dir)
    
    # 3. 增量训练模型（保持最优批次大小）
    users, scores, risk_levels = detector.train_incremental(features_df, batch_size=args.batch_size)
    
    # 4. 保存结果
    results_df = pd.DataFrame({
        'user': users,
        'anomaly_score': scores,
        'risk_level': risk_levels
    })
    
    output_dir = args.output_dir
    os.makedirs(output_dir, exist_ok=True)
    
    output_file = f'{output_dir}/incremental_ocsvm_results.csv'
    results_df.to_csv(output_file, index=False)
    print(f"\n✓ 检测结果已保存: {output_file}")
    
    # 5. 加载真实标签并评估模型
    eval_df = None
    if args.skip_eval:
        print("\n已跳过评估（--skip-eval）")
    elif os.path.exists(args.insiders_file):
        eval_df = detector.evaluate_model(results_df, args.insiders_file, output_dir)
    else:
        print(f"\n警告: 评估文件不存在，跳过评估: {args.insiders_file}")
    
    # 6. 保存模型
    model_file = f'{output_dir}/incremental_ocsvm_model.joblib'
    detector.save_model(model_file)
    
    # 7. 显示高风险用户
    high_risk = results_df[results_df['risk_level'].isin(['高风险', '中风险'])].sort_values('anomaly_score')
    print(f"\n高风险和中风险用户 (前20个):")
    print(high_risk.head(20).to_string(index=False))
    
    # 8. 生成性能摘要
    if eval_df is not None:
        print("\n" + "=" * 70)
        print("性能摘要")
        print("=" * 70)
        print(f"准确率:   {eval_df['accuracy'].values[0]*100:.2f}%")
        print(f"精确率:   {eval_df['precision'].values[0]*100:.2f}%")
        print(f"召回率:   {eval_df['recall'].values[0]*100:.2f}%")
        print(f"F1分数:   {eval_df['f1_score'].values[0]:.4f}")
        print(f"训练批次: {detector.batch_count}")
    
    print("\n" + "=" * 70)
    print("增量One-Class SVM检测完成！")
    print("=" * 70)
    
    return detector, results_df


if __name__ == "__main__":
    detector, results = main()
