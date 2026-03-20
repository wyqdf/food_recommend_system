from __future__ import annotations

from contextlib import contextmanager
from datetime import datetime
from typing import Any, Iterable, Iterator

import pymysql
from pymysql.cursors import DictCursor

from .config import AppConfig


class DatabaseClient:
    def __init__(self, config: AppConfig) -> None:
        self._config = config

    @contextmanager
    def connection(self) -> Iterator[pymysql.connections.Connection]:
        conn = pymysql.connect(
            host=self._config.database.host,
            port=self._config.database.port,
            user=self._config.database.username,
            password=self._config.database.password,
            database=self._config.database.database,
            charset=self._config.database.charset,
            cursorclass=DictCursor,
            autocommit=False,
        )
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    def fetch_all(self, sql: str, params: tuple[Any, ...] | list[Any] | None = None) -> list[dict[str, Any]]:
        with self.connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(sql, params or ())
                return list(cursor.fetchall())

    def fetch_one(self, sql: str, params: tuple[Any, ...] | list[Any] | None = None) -> dict[str, Any] | None:
        rows = self.fetch_all(sql, params)
        return rows[0] if rows else None

    def execute(self, sql: str, params: tuple[Any, ...] | list[Any] | None = None) -> int:
        with self.connection() as conn:
            with conn.cursor() as cursor:
                return cursor.execute(sql, params or ())

    def executemany(self, sql: str, rows: Iterable[tuple[Any, ...]]) -> int:
        batch = list(rows)
        if not batch:
            return 0
        with self.connection() as conn:
            with conn.cursor() as cursor:
                return cursor.executemany(sql, batch)

    def ensure_daily_tables(self) -> None:
        self.execute(
            """
            CREATE TABLE IF NOT EXISTS daily_recipe_recommendations (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id INT NOT NULL,
                biz_date DATE NOT NULL,
                recipe_id INT NOT NULL,
                rank_no INT NOT NULL,
                selected_for_delivery TINYINT DEFAULT 0,
                model_score DECIMAL(16,8) DEFAULT 0,
                reason_json JSON NULL,
                model_version VARCHAR(64) NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
                UNIQUE KEY uk_daily_reco_user_date_rank (user_id, biz_date, rank_no),
                UNIQUE KEY uk_daily_reco_user_date_recipe (user_id, biz_date, recipe_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """
        )
        self.execute(
            """
            CREATE TABLE IF NOT EXISTS daily_recommend_job_runs (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                job_date DATE NOT NULL,
                phase VARCHAR(32) NOT NULL,
                model_version VARCHAR(64) NOT NULL,
                affected_users INT DEFAULT 0,
                affected_recipes INT DEFAULT 0,
                status VARCHAR(20) NOT NULL DEFAULT 'running',
                error_message TEXT NULL,
                started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                finished_at DATETIME NULL,
                KEY idx_daily_job_runs_date_phase (job_date, phase, status),
                KEY idx_daily_job_runs_started (started_at DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """
        )
        if not self._has_index("daily_recipe_recommendations", "idx_daily_reco_user_date_delivery"):
            self.execute(
                "CREATE INDEX idx_daily_reco_user_date_delivery ON daily_recipe_recommendations(user_id, biz_date, selected_for_delivery, rank_no)"
            )
        if not self._has_index("daily_recipe_recommendations", "idx_daily_reco_date_version"):
            self.execute(
                "CREATE INDEX idx_daily_reco_date_version ON daily_recipe_recommendations(biz_date, model_version)"
            )

    def _has_index(self, table_name: str, index_name: str) -> bool:
        row = self.fetch_one(
            """
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = %s
              AND table_name = %s
              AND index_name = %s
            LIMIT 1
            """,
            (self._config.database.database, table_name, index_name),
        )
        return row is not None

    def latest_successful_finished_at(self) -> datetime | None:
        row = self.fetch_one(
            """
            SELECT finished_at
            FROM daily_recommend_job_runs
            WHERE status = 'success'
            ORDER BY finished_at DESC, id DESC
            LIMIT 1
            """
        )
        return row["finished_at"] if row and row.get("finished_at") else None

    def start_job_run(self, job_date: str, phase: str, model_version: str) -> int:
        with self.connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO daily_recommend_job_runs (
                        job_date, phase, model_version, status, started_at
                    ) VALUES (%s, %s, %s, 'running', NOW())
                    """,
                    (job_date, phase, model_version),
                )
                return int(cursor.lastrowid)

    def finish_job_run(
        self,
        run_id: int,
        status: str,
        affected_users: int = 0,
        affected_recipes: int = 0,
        error_message: str | None = None,
    ) -> None:
        self.execute(
            """
            UPDATE daily_recommend_job_runs
            SET status = %s,
                affected_users = %s,
                affected_recipes = %s,
                error_message = %s,
                finished_at = NOW()
            WHERE id = %s
            """,
            (status, affected_users, affected_recipes, error_message, run_id),
        )
