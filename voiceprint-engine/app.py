from __future__ import annotations

import base64
import binascii
import os
import time
import uuid
import zlib
from dataclasses import asdict
from pathlib import Path

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel

from core import VoiceprintEngine


MODEL_PATH = os.getenv("VOICEPRINT_MODEL_PATH", "./models/wespeaker_zh_cnceleb_resnet34.onnx")
DEFAULT_THRESHOLD = float(os.getenv("VOICEPRINT_THRESHOLD", "0.58"))
PROVIDER = os.getenv("VOICEPRINT_PROVIDER", "cpu")
NUM_THREADS = int(os.getenv("VOICEPRINT_NUM_THREADS", "1"))

app = FastAPI(title="voiceprint-engine", version="0.1.0")


class HealthResponse(BaseModel):
    healthy: bool
    model_version: str
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


class VoiceprintEnrollRequest(BaseModel):
    user_id: int
    username: str
    audio_base64: str
    metadata: dict | None = None


class VoiceprintEnrollResponse(BaseModel):
    template_ref: str
    template_digest: str
    model_source: str
    model_version: str
    threshold: float
    raw_response: dict


class VoiceprintVerifyRequest(BaseModel):
    template_ref: str
    template_digest: str
    audio_base64: str
    threshold: float | None = None
    metadata: dict | None = None


class VoiceprintVerifyResponse(BaseModel):
    request_id: str
    verified: bool
    score: float
    threshold: float
    fail_reason: str | None
    model_source: str
    model_version: str
    raw_response: dict


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


MODEL_VERSION = f"{Path(MODEL_PATH).name}:{PROVIDER}"
MODEL_SOURCE = "voiceprint-engine"
TEMPLATE_PREFIX = "vpemb-v1:"


def _decode_audio_base64(payload: str) -> bytes:
    try:
        return base64.b64decode(payload, validate=True)
    except binascii.Error as ex:
        raise HTTPException(status_code=400, detail=f"invalid audio_base64: {ex}") from ex


def _pack_embedding(embedding) -> str:
    emb_bytes = embedding.astype("float32").tobytes()
    packed = zlib.compress(emb_bytes, level=6)
    token = base64.urlsafe_b64encode(packed).decode("ascii")
    return f"{TEMPLATE_PREFIX}{token}"


def _unpack_embedding(template_ref: str):
    if not template_ref.startswith(TEMPLATE_PREFIX):
        raise HTTPException(status_code=400, detail="invalid template_ref format")
    token = template_ref[len(TEMPLATE_PREFIX):]
    try:
        packed = base64.urlsafe_b64decode(token.encode("ascii"))
        emb_bytes = zlib.decompress(packed)
    except Exception as ex:
        raise HTTPException(status_code=400, detail=f"invalid template_ref payload: {ex}") from ex
    import numpy as np  # local import to avoid unnecessary import when engine init fails
    embedding = np.frombuffer(emb_bytes, dtype=np.float32)
    if embedding.shape[0] != engine.embedding_dim:
        raise HTTPException(status_code=400, detail="template_ref embedding dimension mismatch")
    return embedding


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    if engine is None:
        raise HTTPException(status_code=500, detail=f"engine init failed: {init_error}")
    return HealthResponse(
        healthy=True,
        model_version=MODEL_VERSION,
        status="UP",
        model_path=engine.model_path,
        embedding_dim=engine.embedding_dim,
        threshold=engine.threshold,
        provider=engine.provider,
        num_threads=engine.num_threads,
    )


@app.post("/voiceprint/enroll", response_model=VoiceprintEnrollResponse)
async def voiceprint_enroll(request: VoiceprintEnrollRequest) -> VoiceprintEnrollResponse:
    if engine is None:
        raise HTTPException(status_code=500, detail=f"engine init failed: {init_error}")
    try:
        start = time.perf_counter()
        audio_bytes = _decode_audio_base64(request.audio_base64)
        embedding = engine.compute_embedding_from_bytes(audio_bytes)
        digest = engine.embedding_digest(embedding)
        samples, sample_rate = engine._load_audio_from_bytes(audio_bytes)
        duration_ms = int((len(samples) / float(sample_rate)) * 1000) if sample_rate > 0 else 0
        template_ref = _pack_embedding(embedding)
        latency_ms = int((time.perf_counter() - start) * 1000)
    except HTTPException:
        raise
    except Exception as ex:
        raise HTTPException(status_code=400, detail=f"enroll failed: {ex}") from ex
    return VoiceprintEnrollResponse(
        template_ref=template_ref,
        template_digest=digest,
        model_source=MODEL_SOURCE,
        model_version=MODEL_VERSION,
        threshold=engine.threshold,
        raw_response={
            "latency_ms": latency_ms,
            "sample_rate": sample_rate,
            "duration_ms": duration_ms,
            "embedding_dim": int(engine.embedding_dim),
            "user_id": request.user_id,
            "username": request.username,
        },
    )


@app.post("/voiceprint/verify", response_model=VoiceprintVerifyResponse)
async def voiceprint_verify(request: VoiceprintVerifyRequest) -> VoiceprintVerifyResponse:
    if engine is None:
        raise HTTPException(status_code=500, detail=f"engine init failed: {init_error}")
    try:
        start = time.perf_counter()
        enroll_embedding = _unpack_embedding(request.template_ref)
        digest = engine.embedding_digest(enroll_embedding)
        if digest != request.template_digest:
            return VoiceprintVerifyResponse(
                request_id=str(uuid.uuid4()),
                verified=False,
                score=0.0,
                threshold=request.threshold if request.threshold is not None else engine.threshold,
                fail_reason="模板摘要不匹配",
                model_source=MODEL_SOURCE,
                model_version=MODEL_VERSION,
                raw_response={"reason": "template_digest_mismatch"},
            )
        probe_bytes = _decode_audio_base64(request.audio_base64)
        probe_embedding = engine.compute_embedding_from_bytes(probe_bytes)
        result = engine.verify_embeddings(enroll_embedding, probe_embedding, threshold=request.threshold)
        latency_ms = int((time.perf_counter() - start) * 1000)
    except HTTPException:
        raise
    except Exception as ex:
        raise HTTPException(status_code=400, detail=f"verify failed: {ex}") from ex

    verified = result.decision == "ACCEPT"
    return VoiceprintVerifyResponse(
        request_id=str(uuid.uuid4()),
        verified=verified,
        score=result.score,
        threshold=result.threshold,
        fail_reason=None if verified else "声纹相似度低于阈值",
        model_source=MODEL_SOURCE,
        model_version=MODEL_VERSION,
        raw_response={
            "decision": result.decision,
            "latency_ms": latency_ms,
        },
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
