"""Persistent SQLite task inbox for the single-owner assistant."""
import os
import sqlite3
import uuid
from datetime import datetime, timezone


def database_path():
    configured = os.getenv("TASK_DATABASE_PATH", "").strip()
    if configured:
        return configured
    base = os.path.join(os.path.dirname(__file__), "instance")
    os.makedirs(base, exist_ok=True)
    return os.path.join(base, "tasks.sqlite3")


def connect():
    connection = sqlite3.connect(database_path(), timeout=15)
    connection.row_factory = sqlite3.Row
    return connection


def initialize_database():
    with connect() as db:
        db.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                owner_username TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                due_at TEXT,
                status TEXT NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending', 'completed', 'cancelled')),
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
        """)
        db.execute("""
            CREATE INDEX IF NOT EXISTS idx_tasks_owner_status_due
            ON tasks(owner_username, status, due_at)
        """)
        db.commit()


def _clean_due_at(value):
    if value in (None, ""):
        return None
    if not isinstance(value, str) or len(value) > 80:
        raise ValueError("Due date must be a valid date and time.")
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError("Due date must be a valid ISO date and time.") from exc
    if parsed.tzinfo is None:
        raise ValueError("Due date must include a timezone.")
    return parsed.astimezone(timezone.utc).isoformat(timespec="seconds")


def _serialize(row):
    return {
        "id": row["id"],
        "title": row["title"],
        "description": row["description"],
        "due_at": row["due_at"],
        "status": row["status"],
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
    }


def list_tasks(owner_username, include_cancelled=False):
    initialize_database()
    query = "SELECT * FROM tasks WHERE owner_username = ?"
    args = [owner_username]
    if not include_cancelled:
        query += " AND status != 'cancelled'"
    query += """
        ORDER BY
          CASE WHEN status = 'pending' THEN 0 ELSE 1 END,
          CASE WHEN due_at IS NULL THEN 1 ELSE 0 END,
          due_at ASC,
          created_at DESC
    """
    with connect() as db:
        rows = db.execute(query, args).fetchall()
    return [_serialize(row) for row in rows]


def create_task(owner_username, title, description="", due_at=None):
    title = str(title or "").strip()
    description = str(description or "").strip()
    if not title:
        raise ValueError("Please enter a task title.")
    if len(title) > 160:
        raise ValueError("Task title must be 160 characters or fewer.")
    if len(description) > 2000:
        raise ValueError("Task details must be 2,000 characters or fewer.")
    due_value = _clean_due_at(due_at)
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    task_id = str(uuid.uuid4())
    initialize_database()
    with connect() as db:
        db.execute(
            """INSERT INTO tasks
               (id, owner_username, title, description, due_at, status, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, 'pending', ?, ?)""",
            (task_id, owner_username, title, description, due_value, now, now),
        )
        db.commit()
        row = db.execute(
            "SELECT * FROM tasks WHERE id = ? AND owner_username = ?",
            (task_id, owner_username),
        ).fetchone()
    return _serialize(row)


def update_task(owner_username, task_id, changes):
    allowed = {"title", "description", "due_at", "status"}
    if not isinstance(changes, dict) or not changes or set(changes) - allowed:
        raise ValueError("Please provide valid task fields.")
    updates = {}
    if "title" in changes:
        title = str(changes["title"] or "").strip()
        if not title or len(title) > 160:
            raise ValueError("Task title is required and must be 160 characters or fewer.")
        updates["title"] = title
    if "description" in changes:
        description = str(changes["description"] or "").strip()
        if len(description) > 2000:
            raise ValueError("Task details must be 2,000 characters or fewer.")
        updates["description"] = description
    if "due_at" in changes:
        updates["due_at"] = _clean_due_at(changes["due_at"])
    if "status" in changes:
        status = str(changes["status"] or "")
        if status not in ("pending", "completed", "cancelled"):
            raise ValueError("Task status is not valid.")
        updates["status"] = status
    updates["updated_at"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
    assignments = ", ".join(f"{column} = ?" for column in updates)
    values = list(updates.values()) + [task_id, owner_username]
    initialize_database()
    with connect() as db:
        cursor = db.execute(
            f"UPDATE tasks SET {assignments} WHERE id = ? AND owner_username = ?",
            values,
        )
        if cursor.rowcount != 1:
            return None
        db.commit()
        row = db.execute(
            "SELECT * FROM tasks WHERE id = ? AND owner_username = ?",
            (task_id, owner_username),
        ).fetchone()
    return _serialize(row)


def delete_task(owner_username, task_id):
    initialize_database()
    with connect() as db:
        cursor = db.execute(
            "DELETE FROM tasks WHERE id = ? AND owner_username = ?",
            (task_id, owner_username),
        )
        db.commit()
        return cursor.rowcount == 1
