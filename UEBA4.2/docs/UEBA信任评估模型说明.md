# UEBA 信任评估模型说明（项目详解 + 训练/运行指南）

本文档基于当前代码实际实现（`model/*.py`、`ueba_api_service.py` 等），对“信任评估/UEBA 异常检测模型”做结构化讲解：每部分做什么、为什么这么做、优势是什么，以及如何训练/运行。

---

## 1. 总体流程（数据到风险）

整体是一个“多维行为日志 → 特征 → 无监督异常检测 → 风险分级 → API 输出”的流程：

1. 读取多维日志（`logon / device / file / http / email`）。
2. 对每个用户聚合特征（行为频率、时间模式、间隔、类型多样性等）。
3. 合并用户级特征为一个向量。
4. 模型输出 `anomaly_score`（异常分数，越低越异常）。
5. 通过分位数阈值划分风险等级（正常/低/中/高）。
6. 可选：结合真实标签 `insiders.csv` 评估性能。
7. 可选：通过 API 提供用户级/事件级检测结果。

优势：
- 无监督：不依赖大量标注样本，适合内部威胁这类“少数类 + 新型攻击”。
- 多维融合：将多个行为维度合并，有助于发现跨维度协同异常。
- 风险分级：比单纯二分类更贴合实际安全运营。

---

## 2. 核心模型概览

项目内主要有两套“用户级异常检测模型”：

### 2.1 统一 UEBA 模型（Isolation Forest）
文件：`model/unified_ueba_detector.py`

**做什么**
- 提取五个维度的用户特征并合并，使用单一 Isolation Forest 检测异常。 
- 用分位数设置风险阈值（25% / 10% / 5%）划分风险等级。 

**为什么这样做**
- Isolation Forest 适合高维无监督异常检测，对异常点更敏感。
- 单模型融合多维数据，避免多模型后期再融合的复杂度。
- RobustScaler 处理极端值（行为日志常有长尾分布）。

**优势**
- 训练快、易解释（异常分数+分位数阈值）。
- 对异常值鲁棒。
- 代码里对 `http/email` 做了可选容错（文件不存在可跳过）。

---

### 2.2 增量 OCSVM 模型（SGDOneClassSVM）
文件：`model/incremental_ocsvm_detector.py`

**做什么**
- 使用 `SGDOneClassSVM` 进行增量训练（`partial_fit`）。
- 对每个用户生成统一特征向量。 
- 采用**实验最优阈值**（4.5% / 11% / 24%）划分风险等级。 

**为什么这样做**
- OCSVM 是经典无监督异常检测器。
- SGD 版本支持**增量/批次训练**，适合大规模数据或持续更新。
- 分位数阈值稳定可控，不依赖模型 `predict` 的硬阈值。

**优势**
- 可在线学习，支持模型持续更新。
- 训练过程按批次推进，内存压力小。
- 已在 `实验结果.md` 中对参数做系统调优。

---

## 3. 特征工程（五维日志）

所有检测的本质是“用户级聚合特征”。以下是代码中提取的主要特征：

### 3.1 Logon（登录行为）
来源：`logon.csv`（要求字段：`user`、`date`、`pc`）

核心特征：
- 频率：`logon_count` / `logon_total`、`logon_per_day`
- 时间模式：深夜比例、周末比例、工作时段比例
- 时间间隔：平均间隔、标准差、短/长间隔比例
- 设备模式：登录 PC 数、主 PC 使用占比、PC 多样性

优势：能捕捉深夜登录、频繁切换设备、异常间隔等风险。

### 3.2 Device（设备/USB）
来源：`device.csv`（字段：`user`、`date`、`pc`、`activity`）

核心特征：
- 事件数量、连接/断开比例
- 深夜/周末比例
- 连接间隔、突发使用（burst）

优势：对“突然大量 USB 行为”“频繁插拔”等异常敏感。

### 3.3 File（文件操作）
来源：`file.csv`（字段：`user`、`date`、`pc`、`filename`）

核心特征：
- 文件操作总量、每天平均
- 文件类型数量、类型多样性
- 深夜/周末比例
- 文件操作间隔、突发操作

优势：可识别大量拷贝、深夜拷贝、异常文件类型。

### 3.4 HTTP（网络访问）
来源：`http.csv`（字段：`user`、`date`、`pc`、`url`）

核心特征：
- 访问量、访问域名/URL 多样性
- 深夜比例、周末比例
- 访问间隔、快速访问比例

优势：能捕捉爬虫式快速访问、异常访问量、非典型时间访问。

### 3.5 Email（邮件行为）
来源：`email.csv`（字段：`user`、`date`，可选 `to`、`size` 等）

核心特征：
- 邮件数量、发送/接收比例
- 邮件大小统计、附件比例
- 深夜/周末比例

优势：对大附件外发、深夜发信等异常敏感。

---

## 4. 评估与可视化

### 4.1 评估模块
文件：`evaluation_utils.py` / `model/unified_ueba_detector.py` / `model/incremental_ocsvm_detector.py`

评估逻辑：
- 从 `insiders.csv` 读取真实威胁用户（字段：`dataset`、`user`）。
- 将“非正常风险等级”视为异常（1），正常视为正常（0）。
- 计算 Accuracy / Precision / Recall / F1 / 混淆矩阵。

优势：统一的无监督评估方式，便于不同模型对比。

### 4.2 可视化
文件：`model/visualize_evaluation.py`

生成：
- 混淆矩阵热图
- 指标柱状图
- 风险分布/分数分布

优势：快速展示检测效果与风险分布。

---

## 5. API 服务（线上检测入口）

文件：`ueba_api_service.py`

### 5.1 用户级检测
- 根据输入行为数据提取聚合特征，调用模型得到 `anomaly_score`。
- 根据阈值映射到风险等级。

### 5.2 事件级检测（规则/基线）
- 基于用户历史行为建立“典型时间/设备/文件类型”等基线。
- 对单个事件打分（rule-based），并解释原因（如“深夜登录”“陌生设备”）。

优势：
- 用户级检测提供总体风险。
- 事件级检测提供可解释的“异常原因”，便于运营排查。

### 5.3 API 端点
- `GET /health` 健康检查
- `GET /model/info` 模型信息
- `POST /detect/comprehensive` 用户级 + 事件级
- `POST /detect/user_level` 只返回用户级
- `POST /detect/event_level` 只返回事件级
- `POST /detect/single_event` 单事件检测

---

## 6. 如何训练与运行

> **注意**：代码中存在**硬编码数据路径**（例如 `/data/lmm/r6.2/r4.2`），本地运行需要改成你的实际数据路径。

### 6.1 安装依赖

```bash
pip install -r requirements.txt
```

### 6.2 数据准备

需要以下 CSV 文件（至少前三个）：

- `logon.csv`：`user`, `date`, `pc`
- `device.csv`：`user`, `date`, `pc`, `activity`
- `file.csv`：`user`, `date`, `pc`, `filename`
- `http.csv`：`user`, `date`, `pc`, `url`
- `email.csv`：`user`, `date`, `to`, `size`（可选列）

可选标签文件：
- `insiders.csv`：字段至少包含 `dataset`、`user`

### 6.3 训练增量 OCSVM（推荐）

编辑 `model/incremental_ocsvm_detector.py` 中 `main()` 的路径：

- `features_df = detector.load_and_merge_data('你的数据目录')`
- `insider_labels_path = '你的insiders.csv路径'`
- `output_dir = '你的输出目录'`

然后运行：

```bash
python model/incremental_ocsvm_detector.py
```

输出：
- `result/incremental_ocsvm_model.joblib`
- `result/incremental_ocsvm_results.csv`
- `result/incremental_ocsvm_evaluation.csv`

### 6.4 训练统一 Isolation Forest 模型

编辑 `model/unified_ueba_detector.py` 中 `main()` 的路径：

- `features_df = detector.load_and_merge_data('你的数据目录')`
- `insiders_file = '你的insiders.csv路径'`
- `output_dir = '你的输出目录'`

运行：

```bash
python model/unified_ueba_detector.py
```

输出：
- `result/unified_anomaly_model.joblib`
- `result/unified_anomaly_results.csv`
- `result/model_evaluation.csv`

### 6.5 启动 API 服务

1. 确保已有模型文件（推荐使用增量 OCSVM 训练出的 `joblib`）。
2. 启动服务：

```bash
python ueba_api_service.py --model result/incremental_ocsvm_model.joblib --host 0.0.0.0 --port 5000
```

也可直接运行脚本：

```bash
./start_api_service.sh
```

### 6.6 测试 API

```bash
python test_api.py
```

---

## 7. 代码结构速览（真实代码为准）

- `model/unified_ueba_detector.py`：统一 Isolation Forest 模型（离线训练）。
- `model/incremental_ocsvm_detector.py`：增量 OCSVM 模型（离线训练/增量）。
- `model/visualize_evaluation.py`：评估可视化。
- `evaluation_utils.py`：评估工具函数。
- `ueba_api_service.py`：Flask API 服务（用户级 + 事件级）。
- `test_api.py`：API 功能测试。
- `start_api_service.sh`：API 启动脚本。
- `result/`：输出结果（已有样例模型/结果文件）。

---

## 8. 使用建议与注意事项

1. **README 与代码不一致**：`README.md` 描述的是早期多模型结构，但当前实际实现以 `unified_ueba_detector.py` 与 `incremental_ocsvm_detector.py` 为主。
2. **路径硬编码**：训练脚本内有固定路径，建议统一改为本地路径或加参数。
3. **异常分数方向**：`score_samples` 越小越异常，不要反向解读。
4. **事件级检测是规则基线**：可解释但不如模型灵活，可考虑加入统计阈值或学习型评分。
5. **缺失维度容错不同**：统一模型允许缺失 `http/email`，增量 OCSVM 默认要求所有 CSV 存在。

---

## 9. 你可以先这样快速跑通

如果你想先快速验证整体流程：

1. 修改 `model/incremental_ocsvm_detector.py` 里的数据路径到本地。
2. 运行训练脚本生成模型。
3. 启动 `ueba_api_service.py`。
4. 运行 `test_api.py`。

这样可以最快得到“训练 → API → 测试”的完整闭环。
