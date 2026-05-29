"""
可视化模型评估结果
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.font_manager as fm
from matplotlib import rcParams
import os

# 配置中文字体
def configure_fonts():
    """配置Matplotlib以支持中文显示"""
    font_paths = [
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
    ]
    
    found_font = None
    for path in font_paths:
        if os.path.exists(path):
            fm.fontManager.addfont(path)
            found_font = fm.FontProperties(fname=path).get_name()
            break
    
    if found_font:
        rcParams['font.sans-serif'] = [found_font, 'DejaVu Sans', 'Arial Unicode MS']
        print(f"✓ 已配置字体: {found_font}")
    else:
        rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial Unicode MS']
        print("✗ 未找到中文字体，使用默认字体")
    
    rcParams['axes.unicode_minus'] = False
    rcParams['font.size'] = 12
    rcParams['axes.titlesize'] = 16
    rcParams['axes.labelsize'] = 14


def plot_evaluation_metrics(eval_file, output_dir):
    """绘制评估指标"""
    df = pd.read_csv(eval_file)
    
    # 创建图形
    fig, axes = plt.subplots(2, 2, figsize=(15, 12))
    fig.suptitle('统一UEBA模型性能评估', fontsize=20, fontweight='bold', y=0.98)
    
    # 1. 混淆矩阵热图
    ax = axes[0, 0]
    cm = np.array([
        [df['true_negative'].values[0], df['false_positive'].values[0]],
        [df['false_negative'].values[0], df['true_positive'].values[0]]
    ])
    
    im = ax.imshow(cm, cmap='Blues', aspect='auto')
    ax.set_xticks([0, 1])
    ax.set_yticks([0, 1])
    ax.set_xticklabels(['预测正常', '预测异常'], fontsize=12)
    ax.set_yticklabels(['实际正常', '实际异常'], fontsize=12)
    ax.set_title('混淆矩阵', fontsize=14, fontweight='bold', pad=10)
    
    # 添加数值标注
    for i in range(2):
        for j in range(2):
            text = ax.text(j, i, f'{cm[i, j]}',
                          ha="center", va="center", color="black", fontsize=16, fontweight='bold')
    
    plt.colorbar(im, ax=ax)
    
    # 2. 主要性能指标柱状图
    ax = axes[0, 1]
    metrics = ['准确率', '精确率', '召回率', 'F1分数']
    values = [
        df['accuracy'].values[0],
        df['precision'].values[0],
        df['recall'].values[0],
        df['f1_score'].values[0]
    ]
    
    colors = ['#2ecc71', '#3498db', '#e74c3c', '#f39c12']
    bars = ax.bar(metrics, values, color=colors, alpha=0.8, edgecolor='black', linewidth=1.5)
    ax.set_ylabel('分数', fontsize=12, fontweight='bold')
    ax.set_title('性能指标对比', fontsize=14, fontweight='bold', pad=10)
    ax.set_ylim(0, 1.0)
    ax.axhline(y=0.5, color='gray', linestyle='--', linewidth=1, alpha=0.5)
    ax.grid(axis='y', alpha=0.3)
    
    # 添加数值标注
    for bar in bars:
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height,
               f'{height:.2%}',
               ha='center', va='bottom', fontsize=11, fontweight='bold')
    
    # 3. 检测结果饼图
    ax = axes[1, 0]
    sizes = [
        df['true_negative'].values[0],
        df['false_positive'].values[0],
        df['false_negative'].values[0],
        df['true_positive'].values[0]
    ]
    labels = [
        f'真负例 (TN)\n{sizes[0]}',
        f'假正例 (FP)\n{sizes[1]}',
        f'假负例 (FN)\n{sizes[2]}',
        f'真正例 (TP)\n{sizes[3]}'
    ]
    colors_pie = ['#2ecc71', '#f39c12', '#e74c3c', '#3498db']
    explode = (0, 0.05, 0.05, 0.1)
    
    wedges, texts, autotexts = ax.pie(sizes, labels=labels, autopct='%1.1f%%',
                                        colors=colors_pie, explode=explode,
                                        shadow=True, startangle=90)
    
    for text in texts:
        text.set_fontsize(11)
        text.set_fontweight('bold')
    for autotext in autotexts:
        autotext.set_color('white')
        autotext.set_fontsize(10)
        autotext.set_fontweight('bold')
    
    ax.set_title('检测结果分布', fontsize=14, fontweight='bold', pad=10)
    
    # 4. 数据集统计信息
    ax = axes[1, 1]
    ax.axis('off')
    
    total_users = df['total_users'].values[0]
    actual_insiders = df['actual_insiders'].values[0]
    detected_anomalies = df['detected_anomalies'].values[0]
    
    stats_text = f"""
数据集统计信息

总用户数: {total_users}
真实内部威胁用户数: {actual_insiders}
检测为异常的用户数: {detected_anomalies}
内部威胁比例: {actual_insiders/total_users*100:.2f}%

详细分析

真正例 (TP): {df['true_positive'].values[0]}
  正确识别的内部威胁

假正例 (FP): {df['false_positive'].values[0]}
  误判为异常的正常用户

真负例 (TN): {df['true_negative'].values[0]}
  正确识别的正常用户

假负例 (FN): {df['false_negative'].values[0]}
  漏检的内部威胁

性能总结

准确率: {df['accuracy'].values[0]:.2%}
精确率: {df['precision'].values[0]:.2%}
召回率: {df['recall'].values[0]:.2%}
F1分数: {df['f1_score'].values[0]:.4f}
"""
    
    ax.text(0.1, 0.95, stats_text, transform=ax.transAxes,
           fontsize=11, verticalalignment='top',
           bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.3),
           family='monospace')
    
    plt.tight_layout()
    
    # 保存图像
    output_file = f'{output_dir}/evaluation_visualization.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✓ 评估可视化已保存: {output_file}")
    plt.close()


def plot_risk_distribution(results_file, insiders_file, output_dir):
    """绘制风险等级分布"""
    # 加载数据
    results = pd.read_csv(results_file)
    insiders_df = pd.read_csv(insiders_file)
    insiders = set(insiders_df[insiders_df['dataset'] == 4.2]['user'])
    
    # 统计每个风险等级的内部威胁数量
    risk_levels = ['正常', '低风险', '中风险', '高风险']
    insider_counts = []
    total_counts = []
    
    for level in risk_levels:
        level_users = results[results['risk_level'] == level]['user']
        insiders_in_level = sum(1 for u in level_users if u in insiders)
        insider_counts.append(insiders_in_level)
        total_counts.append(len(level_users))
    
    # 创建图形
    fig, axes = plt.subplots(1, 2, figsize=(16, 6))
    fig.suptitle('风险等级与内部威胁分布分析', fontsize=18, fontweight='bold')
    
    # 1. 堆叠柱状图
    ax = axes[0]
    normal_counts = [total - insider for total, insider in zip(total_counts, insider_counts)]
    
    x = np.arange(len(risk_levels))
    width = 0.6
    
    bars1 = ax.bar(x, normal_counts, width, label='正常用户', color='#2ecc71', alpha=0.8)
    bars2 = ax.bar(x, insider_counts, width, bottom=normal_counts,
                   label='内部威胁', color='#e74c3c', alpha=0.8)
    
    ax.set_ylabel('用户数量', fontsize=12, fontweight='bold')
    ax.set_title('各风险等级用户分布', fontsize=14, fontweight='bold')
    ax.set_xticks(x)
    ax.set_xticklabels(risk_levels, fontsize=12)
    ax.legend(fontsize=11)
    ax.grid(axis='y', alpha=0.3)
    
    # 添加数值标注
    for i, (bar1, bar2) in enumerate(zip(bars1, bars2)):
        height1 = bar1.get_height()
        height2 = bar2.get_height()
        if height1 > 0:
            ax.text(bar1.get_x() + bar1.get_width()/2., height1/2,
                   f'{int(height1)}',
                   ha='center', va='center', fontsize=10, fontweight='bold', color='white')
        if height2 > 0:
            ax.text(bar2.get_x() + bar2.get_width()/2., height1 + height2/2,
                   f'{int(height2)}',
                   ha='center', va='center', fontsize=10, fontweight='bold', color='white')
    
    # 2. 内部威胁检测率
    ax = axes[1]
    detection_rates = [insider/total*100 if total > 0 else 0 
                      for insider, total in zip(insider_counts, total_counts)]
    
    colors_gradient = ['#95a5a6', '#f39c12', '#e67e22', '#c0392b']
    bars = ax.bar(risk_levels, detection_rates, color=colors_gradient, alpha=0.8,
                  edgecolor='black', linewidth=1.5)
    
    ax.set_ylabel('内部威胁占比 (%)', fontsize=12, fontweight='bold')
    ax.set_title('各风险等级的内部威胁检出率', fontsize=14, fontweight='bold')
    ax.grid(axis='y', alpha=0.3)
    
    # 添加数值标注
    for bar, rate, insider, total in zip(bars, detection_rates, insider_counts, total_counts):
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height,
               f'{rate:.1f}%\n({insider}/{total})',
               ha='center', va='bottom', fontsize=10, fontweight='bold')
    
    plt.tight_layout()
    
    # 保存图像
    output_file = f'{output_dir}/risk_distribution.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✓ 风险分布可视化已保存: {output_file}")
    plt.close()


def plot_score_distribution(results_file, insiders_file, output_dir):
    """绘制异常分数分布"""
    # 加载数据
    results = pd.read_csv(results_file)
    insiders_df = pd.read_csv(insiders_file)
    insiders = set(insiders_df[insiders_df['dataset'] == 4.2]['user'])
    
    # 分离正常用户和内部威胁的异常分数
    results['is_insider'] = results['user'].apply(lambda x: x in insiders)
    normal_scores = results[~results['is_insider']]['anomaly_score']
    insider_scores = results[results['is_insider']]['anomaly_score']
    
    # 创建图形
    fig, axes = plt.subplots(2, 1, figsize=(14, 10))
    fig.suptitle('异常分数分布分析', fontsize=18, fontweight='bold')
    
    # 1. 直方图对比
    ax = axes[0]
    bins = 50
    ax.hist(normal_scores, bins=bins, alpha=0.6, label='正常用户', 
           color='#2ecc71', edgecolor='black', linewidth=0.5)
    ax.hist(insider_scores, bins=bins, alpha=0.8, label='内部威胁',
           color='#e74c3c', edgecolor='black', linewidth=0.5)
    
    ax.set_xlabel('异常分数', fontsize=12, fontweight='bold')
    ax.set_ylabel('用户数量', fontsize=12, fontweight='bold')
    ax.set_title('异常分数直方图分布', fontsize=14, fontweight='bold')
    ax.legend(fontsize=12)
    ax.grid(axis='y', alpha=0.3)
    
    # 2. 箱线图对比
    ax = axes[1]
    data_to_plot = [normal_scores, insider_scores]
    bp = ax.boxplot(data_to_plot, labels=['正常用户', '内部威胁'],
                    patch_artist=True, widths=0.6,
                    boxprops=dict(facecolor='lightblue', alpha=0.7),
                    medianprops=dict(color='red', linewidth=2),
                    whiskerprops=dict(linewidth=1.5),
                    capprops=dict(linewidth=1.5))
    
    # 设置箱体颜色
    colors = ['#2ecc71', '#e74c3c']
    for patch, color in zip(bp['boxes'], colors):
        patch.set_facecolor(color)
        patch.set_alpha(0.6)
    
    ax.set_ylabel('异常分数', fontsize=12, fontweight='bold')
    ax.set_title('异常分数箱线图对比', fontsize=14, fontweight='bold')
    ax.grid(axis='y', alpha=0.3)
    
    # 添加统计信息
    stats_text = f"""正常用户: 均值={normal_scores.mean():.4f}, 中位数={normal_scores.median():.4f}
内部威胁: 均值={insider_scores.mean():.4f}, 中位数={insider_scores.median():.4f}"""
    
    ax.text(0.02, 0.98, stats_text, transform=ax.transAxes,
           fontsize=11, verticalalignment='top',
           bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5),
           family='monospace')
    
    plt.tight_layout()
    
    # 保存图像
    output_file = f'{output_dir}/score_distribution.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✓ 分数分布可视化已保存: {output_file}")
    plt.close()


def main():
    """主函数"""
    import argparse

    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser = argparse.ArgumentParser(description='UEBA评估结果可视化')
    parser.add_argument('--output-dir', default=os.path.join(project_root, 'result2'),
                        help='可视化输出目录')
    parser.add_argument('--eval-file', default=None, help='评估文件路径（默认: <output-dir>/model_evaluation.csv）')
    parser.add_argument('--results-file', default=None, help='检测结果路径（默认: <output-dir>/unified_anomaly_results.csv）')
    parser.add_argument('--insiders-file', default=os.path.join(project_root, 'data', 'answers', 'insiders.csv'),
                        help='真实标签insiders.csv路径')
    args = parser.parse_args()

    print("\n" + "=" * 70)
    print("        模型评估结果可视化")
    print("=" * 70)
    
    # 配置字体
    configure_fonts()
    
    # 设置路径
    output_dir = args.output_dir
    eval_file = args.eval_file or f'{output_dir}/model_evaluation.csv'
    results_file = args.results_file or f'{output_dir}/unified_anomaly_results.csv'
    insiders_file = args.insiders_file
    
    # 生成可视化
    print("\n生成评估指标可视化...")
    plot_evaluation_metrics(eval_file, output_dir)
    
    print("\n生成风险分布可视化...")
    plot_risk_distribution(results_file, insiders_file, output_dir)
    
    print("\n生成分数分布可视化...")
    plot_score_distribution(results_file, insiders_file, output_dir)
    
    print("\n" + "=" * 70)
    print("可视化完成！")
    print("=" * 70)
    print(f"\n生成的图像文件:")
    print(f"  1. {output_dir}/evaluation_visualization.png")
    print(f"  2. {output_dir}/risk_distribution.png")
    print(f"  3. {output_dir}/score_distribution.png")


if __name__ == "__main__":
    main()


