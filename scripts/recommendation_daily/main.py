from __future__ import annotations

import argparse
import json

from .pipeline import DailyRecommendationPipeline


def main() -> None:
    parser = argparse.ArgumentParser(description="CKE daily recommendation pipeline")
    parser.add_argument("command", choices=["bootstrap-baseline", "run-daily", "use-existing-model"], help="pipeline command")
    args = parser.parse_args()

    pipeline = DailyRecommendationPipeline()
    if args.command == "bootstrap-baseline":
        result = pipeline.bootstrap_baseline()
    elif args.command == "run-daily":
        result = pipeline.run_daily()
    else:
        result = pipeline.use_existing_model()
    print(
        json.dumps(
            {
                "checkpoint_path": str(result.checkpoint_path),
                "dataset_dir": str(result.dataset_dir),
                "metrics_path": str(result.metrics_path),
                "affected_users": result.affected_users,
                "affected_recipes": result.affected_recipes,
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
