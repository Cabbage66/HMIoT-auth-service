#!/bin/bash
# UEBA API服务启动脚本

echo "================================"
echo "启动UEBA异常检测API服务"
echo "================================"

# 检查模型文件是否存在
MODEL_FILE="result/incremental_ocsvm_model.joblib"

if [ ! -f "$MODEL_FILE" ]; then
    echo "错误: 模型文件不存在: $MODEL_FILE"
    echo "请先运行 model/incremental_ocsvm_detector.py 训练模型"
    exit 1
fi

# 检查Python环境
if ! command -v python3 &> /dev/null; then
    echo "错误: 未找到python3"
    exit 1
fi

# 检查依赖
echo "检查依赖..."
python3 -c "import flask" 2>/dev/null || {
    echo "安装Flask..."
    pip install flask
}

# 启动服务
echo ""
echo "启动API服务..."
python3 ueba_api_service.py \
    --model "$MODEL_FILE" \
    --host 0.0.0.0 \
    --port 5004
