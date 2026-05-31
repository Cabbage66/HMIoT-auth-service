# voiceprint-engine

本目录用于声纹模型本地验证与后续服务化。

## 1. 安装依赖

```bash
conda activate voiceprint
cd /Users/caitingsen/ServiceProject/mwih/trust/voiceprint-engine
pip install -r requirements.txt
```

## 2. 脚本化冒烟测试（保留）

```bash
python tools/smoke_test.py \
  --model ./models/wespeaker_zh_cnceleb_resnet34.onnx \
  --enroll /path/to/enroll.wav \
  --probe /path/to/probe.wav \
  --threshold 0.58
```

说明：
- `enroll.wav` 与 `probe.wav` 需为单声道语音文件。
- 推荐输入规格：16kHz, mono, PCM16/WAV。

## 3. 启动最小服务（M0 骨架）

```bash
export VOICEPRINT_MODEL_PATH=./models/wespeaker_zh_cnceleb_resnet34.onnx
export VOICEPRINT_THRESHOLD=0.58
uvicorn app:app --host 0.0.0.0 --port 5003 --reload
```

健康检查（后端对接使用）：

```bash
curl http://127.0.0.1:5003/health
```

后端协议-声纹注册：

```bash
curl -X POST http://127.0.0.1:5003/voiceprint/enroll \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": 1001,
    "username": "zhangsan",
    "audio_base64": "<wav-base64>",
    "metadata": {"scene":"REGISTRATION_SUBMIT"}
  }'
```

后端协议-声纹验证：

```bash
curl -X POST http://127.0.0.1:5003/voiceprint/verify \
  -H "Content-Type: application/json" \
  -d '{
    "template_ref":"<from-enroll>",
    "template_digest":"<from-enroll>",
    "audio_base64":"<wav-base64>",
    "threshold":0.58
  }'
```

兼容接口（保留）：

```bash
curl -X POST http://127.0.0.1:5003/verify \
  -F enroll_audio=@/path/to/enroll.wav \
  -F probe_audio=@/path/to/probe.wav \
  -F threshold=0.58
```

提取 embedding 摘要：

```bash
curl -X POST http://127.0.0.1:5003/embedding \
  -F audio=@/path/to/audio.wav
```

## 4. 目录说明

- `models/`: ONNX 模型
- `core.py`: 推理核心（embedding + cosine）
- `tools/smoke_test.py`: 脚本化测试入口
- `app.py`: FastAPI 最小服务骨架
