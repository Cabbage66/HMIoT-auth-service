from __future__ import annotations

import io
import hashlib
from dataclasses import dataclass
from typing import Tuple

import numpy as np
import sherpa_onnx
import soundfile as sf


@dataclass
class VerifyResult:
    score: float
    threshold: float
    decision: str


@dataclass
class EmbeddingResult:
    feature_digest: str
    sample_rate: int
    duration_ms: int
    embedding_dim: int


class VoiceprintEngine:
    def __init__(
        self,
        model_path: str,
        threshold: float = 0.58,
        provider: str = "cpu",
        num_threads: int = 1,
        debug: bool = False,
    ) -> None:
        config = sherpa_onnx.SpeakerEmbeddingExtractorConfig(
            model=model_path,
            provider=provider,
            num_threads=num_threads,
            debug=debug,
        )
        if not config.validate():
            raise ValueError(f"Invalid speaker embedding config: {config}")

        self.extractor = sherpa_onnx.SpeakerEmbeddingExtractor(config)
        self.model_path = model_path
        self.threshold = float(threshold)
        self.provider = provider
        self.num_threads = int(num_threads)

    @property
    def embedding_dim(self) -> int:
        return int(self.extractor.dim)

    def _load_audio_from_file(self, wav_path: str) -> Tuple[np.ndarray, int]:
        data, sample_rate = sf.read(wav_path, always_2d=True, dtype="float32")
        samples = np.ascontiguousarray(data[:, 0])
        return samples, int(sample_rate)

    def _load_audio_from_bytes(self, wav_bytes: bytes) -> Tuple[np.ndarray, int]:
        data, sample_rate = sf.read(io.BytesIO(wav_bytes), always_2d=True, dtype="float32")
        samples = np.ascontiguousarray(data[:, 0])
        return samples, int(sample_rate)

    def compute_embedding_from_file(self, wav_path: str) -> np.ndarray:
        samples, sample_rate = self._load_audio_from_file(wav_path)
        return self.compute_embedding(samples=samples, sample_rate=sample_rate)

    def compute_embedding_from_bytes(self, wav_bytes: bytes) -> np.ndarray:
        samples, sample_rate = self._load_audio_from_bytes(wav_bytes)
        return self.compute_embedding(samples=samples, sample_rate=sample_rate)

    def compute_embedding(self, samples: np.ndarray, sample_rate: int) -> np.ndarray:
        stream = self.extractor.create_stream()
        stream.accept_waveform(sample_rate=int(sample_rate), waveform=samples)
        stream.input_finished()
        if not self.extractor.is_ready(stream):
            raise RuntimeError("Speaker extractor stream is not ready")

        embedding = np.array(self.extractor.compute(stream), dtype=np.float32)
        return embedding

    @staticmethod
    def embedding_digest(embedding: np.ndarray) -> str:
        data = np.asarray(embedding, dtype=np.float32).tobytes()
        return hashlib.sha256(data).hexdigest()

    def embedding_info_from_bytes(self, wav_bytes: bytes) -> EmbeddingResult:
        samples, sample_rate = self._load_audio_from_bytes(wav_bytes)
        embedding = self.compute_embedding(samples=samples, sample_rate=sample_rate)
        duration_ms = int((len(samples) / float(sample_rate)) * 1000) if sample_rate > 0 else 0
        return EmbeddingResult(
            feature_digest=self.embedding_digest(embedding),
            sample_rate=sample_rate,
            duration_ms=duration_ms,
            embedding_dim=int(embedding.shape[0]),
        )

    @staticmethod
    def cosine_similarity(enroll_embedding: np.ndarray, probe_embedding: np.ndarray) -> float:
        a = np.asarray(enroll_embedding, dtype=np.float32)
        b = np.asarray(probe_embedding, dtype=np.float32)
        denom = (np.linalg.norm(a) * np.linalg.norm(b))
        if denom == 0:
            return 0.0
        return float(np.dot(a, b) / denom)

    def verify_embeddings(
        self,
        enroll_embedding: np.ndarray,
        probe_embedding: np.ndarray,
        threshold: float | None = None,
    ) -> VerifyResult:
        effective_threshold = self.threshold if threshold is None else float(threshold)
        score = self.cosine_similarity(enroll_embedding, probe_embedding)
        decision = "ACCEPT" if score >= effective_threshold else "REJECT"
        return VerifyResult(score=score, threshold=effective_threshold, decision=decision)

    def verify_files(
        self,
        enroll_wav_path: str,
        probe_wav_path: str,
        threshold: float | None = None,
    ) -> VerifyResult:
        enroll_emb = self.compute_embedding_from_file(enroll_wav_path)
        probe_emb = self.compute_embedding_from_file(probe_wav_path)
        return self.verify_embeddings(enroll_emb, probe_emb, threshold=threshold)

    def verify_bytes(
        self,
        enroll_wav_bytes: bytes,
        probe_wav_bytes: bytes,
        threshold: float | None = None,
    ) -> VerifyResult:
        enroll_emb = self.compute_embedding_from_bytes(enroll_wav_bytes)
        probe_emb = self.compute_embedding_from_bytes(probe_wav_bytes)
        return self.verify_embeddings(enroll_emb, probe_emb, threshold=threshold)
