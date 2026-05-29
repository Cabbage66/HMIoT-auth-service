#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json

from core import VoiceprintEngine


def get_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Voiceprint local smoke test: compare two wav files"
    )
    parser.add_argument("--model", required=True, help="Path to ONNX model")
    parser.add_argument("--enroll", required=True, help="Enrollment wav path")
    parser.add_argument("--probe", required=True, help="Probe wav path")
    parser.add_argument("--threshold", type=float, default=0.58, help="Decision threshold")
    parser.add_argument("--provider", default="cpu", help="cpu/cuda/coreml")
    parser.add_argument("--num-threads", type=int, default=1)
    return parser.parse_args()


def main() -> None:
    args = get_args()
    engine = VoiceprintEngine(
        model_path=args.model,
        threshold=args.threshold,
        provider=args.provider,
        num_threads=args.num_threads,
    )
    result = engine.verify_files(
        enroll_wav_path=args.enroll,
        probe_wav_path=args.probe,
    )
    print(
        json.dumps(
            {
                "model": engine.model_path,
                "embedding_dim": engine.embedding_dim,
                "enroll": args.enroll,
                "probe": args.probe,
                "score": round(result.score, 6),
                "threshold": result.threshold,
                "decision": result.decision,
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
