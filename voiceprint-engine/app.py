from __future__ import annotations

import os
import time
from dataclasses import asdict

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel

from core import VoiceprintEngine


MODEL_PATH = os.getenv("VOICEPRINT_MODEL_PATH", "./models/wespeaker_zh_cnceleb_resnet34.onnx")
DEFAULT_THRESHOLD = float(os.getenv("VOICEPRINT_THRESHOLD", "0.58"))
PROVIDER = os.getenv("VOICEPRINT_PROVIDER", "cpu")
NUM_THREADS = int(os.getenv("VOICEPRINT_NUM_THREADS", "1"))

app = FastAPI(title="voiceprint-engine", version="0.1.0")


class HealthResponse(BaseModel):
    status: str
    model_path: str
    embedding_dim: int
    threshold: float
    provider: str
    num_threads: int


class VerifyResponse(BaseModel):
    score: float
    threshold: float
    decision: str
    latency_ms: int
    model_path: str


class EmbeddingResponse(BaseModel):
    featureDigest: str
    sampleRate: int
    durationMs: int
    embeddingDim: int
    modelPath: str


try:
    engine = VoiceprintEngine(
        model_path=MODEL_PATH,
        threshold=DEFAULT_THRESHOLD,
        provider=PROVIDER,
        num_threads=NUM_THREADS,
    )
except Exception as ex:
    engine = None
    init_error = str(ex)
else:
    init_error = None


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    if engine is None:
        raise HTTPException(status_code=500, detail=f"engine init failed: {init_error}")
    return HealthResponse(
        status="UP",
        model_path=engine.model_path,
        embedding_dim=engine.embedding_dim,
        threshold=engine.threshold,
        provider=engine.provider,
        num_threads=engine.num_threads,
    )


@app.post("/verify", response_model=VerifyResponse)
async def verify(
    enroll_audio: UploadFile = File(...),
    probe_audio: UploadFile = File(...),
    threshold: float | None = Form(default=None),
) -> VerifyResponse:
    if engine is None:
        raise HTTPException(status_code=500, detail=f"engine init failed: {init_error}")

    try:
        start = time.perf_counter()
        enroll_bytes = await enroll_audio.read()
        probe_bytes = await probe_audio.read()
        result = engine.verify_bytes(
            enroll_wav_bytes=enroll_bytes,
            probe_wav_bytes=probe_bytes,
            threshold=threshold,
        )
        latency_ms = int((time.perf_counter() - start) * 1000)
    except Exception as ex:
        raise HTTPException(status_code=400, detail=f"verify failed: {ex}") from ex

    payload = asdict(result)
    return VerifyResponse(
        **payload,
        latency_ms=latency_ms,
        model_path=engine.model_path,
    )


@app.post("/embedding", response_model=EmbeddingResponse)
async def embedding(audio: UploadFile = File(...)) -> EmbeddingResponse:
    if engine is None:
        raise HTTPException(status_code=500, detail=f"engine init failed: {init_error}")

    try:
        audio_bytes = await audio.read()
        info = engine.embedding_info_from_bytes(audio_bytes)
    except Exception as ex:
        raise HTTPException(status_code=400, detail=f"embedding failed: {ex}") from ex

    return EmbeddingResponse(
        featureDigest=info.feature_digest,
        sampleRate=info.sample_rate,
        durationMs=info.duration_ms,
        embeddingDim=info.embedding_dim,
        modelPath=engine.model_path,
    )
