"""
通用评估工具模块
用于计算各个检测器的性能指标
"""

import pandas as pd
import numpy as np
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score, confusion_matrix, classification_report


def load_ground_truth(insiders_file='/data/lmm/r6.2/r4.2/answers/insiders.csv', dataset='4.2'):
    """
    加载真实标签数据
    
    参数:
        insiders_file: insiders.csv文件路径
        dataset: 数据集版本
    
    返回:
        内部威胁用户ID集合
    """
    try:
        df = pd.read_csv(insiders_file)
        # 过滤出指定数据集的内部威胁
        insiders = df[df['dataset'] == float(dataset)]['user'].unique()
        return set(insiders)
    except Exception as e:
        print(f"警告: 无法加载真实标签: {e}")
        return set()


def evaluate_detector(results_df, insiders, detector_name='Detector'):
    """
    评估检测器性能
    
    参数:
        results_df: 检测结果DataFrame (必须包含'user'和'risk_level'列)
        insiders: 真实内部威胁用户ID集合
        detector_name: 检测器名称
    
    返回:
        评估指标字典
    """
    print("\n" + "=" * 70)
    print(f"{detector_name} - 性能评估")
    print("=" * 70)
    
    # 确保results_df包含所需列
    if 'user' not in results_df.columns or 'risk_level' not in results_df.columns:
        print("错误: results_df必须包含'user'和'risk_level'列")
        return None
    
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
    if total_users > 0:
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
    if actual_insiders > 0:
        detected_insiders = results_df[
            (results_df['user'].isin(insiders)) & 
            (results_df['risk_level'] != '正常')
        ].copy()
        
        if 'anomaly_score' in results_df.columns:
            detected_insiders = detected_insiders.sort_values('anomaly_score')
        
        print(f"\n✓ 成功检测到的内部威胁用户 ({len(detected_insiders)}/{actual_insiders}):")
        print("-" * 70)
        if len(detected_insiders) > 0:
            cols_to_show = ['user', 'risk_level']
            if 'anomaly_score' in detected_insiders.columns:
                cols_to_show.insert(1, 'anomaly_score')
            print(detected_insiders[cols_to_show].head(20).to_string(index=False))
        
        # 8. 漏检的内部威胁
        missed_insiders = results_df[
            (results_df['user'].isin(insiders)) & 
            (results_df['risk_level'] == '正常')
        ]
        
        if len(missed_insiders) > 0:
            print(f"\n✗ 漏检的内部威胁用户 ({len(missed_insiders)}/{actual_insiders}):")
            print("-" * 70)
            cols_to_show = ['user', 'risk_level']
            if 'anomaly_score' in missed_insiders.columns:
                cols_to_show.insert(1, 'anomaly_score')
            print(missed_insiders[cols_to_show].head(20).to_string(index=False))
    
    # 9. 构建评估报告字典
    eval_report = {
        'detector_name': detector_name,
        'total_users': int(total_users),
        'actual_insiders': int(actual_insiders),
        'detected_anomalies': int(detected_anomalies),
        'accuracy': float(accuracy),
        'precision': float(precision),
        'recall': float(recall),
        'f1_score': float(f1),
        'true_positive': int(tp),
        'false_positive': int(fp),
        'true_negative': int(tn),
        'false_negative': int(fn)
    }
    
    return eval_report


def save_evaluation_report(eval_report, output_file):
    """
    保存评估报告到CSV文件
    
    参数:
        eval_report: 评估指标字典
        output_file: 输出文件路径
    """
    if eval_report is None:
        return
    
    eval_df = pd.DataFrame([eval_report])
    eval_df.to_csv(output_file, index=False)
    print(f"\n✓ 评估报告已保存: {output_file}")


def save_classification_report(y_true, y_pred, output_file):
    """
    保存详细分类报告到文本文件
    
    参数:
        y_true: 真实标签
        y_pred: 预测标签
        output_file: 输出文件路径
    """
    cm = confusion_matrix(y_true, y_pred)
    tn, fp, fn, tp = cm.ravel()
    
    report = classification_report(y_true, y_pred, 
                                   target_names=['正常', '异常'],
                                   digits=4)
    
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("分类报告\n")
        f.write("=" * 70 + "\n\n")
        f.write(report)
        f.write("\n\n混淆矩阵:\n")
        f.write(f"{'':>15} {'预测正常':>12} {'预测异常':>12}\n")
        f.write(f"{'实际正常':>15} {tn:>12} {fp:>12}\n")
        f.write(f"{'实际异常':>15} {fn:>12} {tp:>12}\n")
    
    print(f"✓ 分类报告已保存: {output_file}")


def compare_detectors(eval_reports):
    """
    比较多个检测器的性能
    
    参数:
        eval_reports: 评估报告列表
    
    返回:
        比较结果DataFrame
    """
    if not eval_reports:
        return None
    
    comparison_df = pd.DataFrame(eval_reports)
    
    # 按F1分数排序
    comparison_df = comparison_df.sort_values('f1_score', ascending=False)
    
    print("\n" + "=" * 70)
    print("检测器性能对比")
    print("=" * 70)
    print("\n")
    print(comparison_df[['detector_name', 'accuracy', 'precision', 'recall', 'f1_score']].to_string(index=False))
    
    return comparison_df



