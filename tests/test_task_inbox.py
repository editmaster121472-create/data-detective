import os
import tempfile
import unittest
from unittest.mock import patch

import server
from task_store import create_task, delete_task, list_tasks, update_task


class TaskStoreTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = os.path.join(self.temp_dir.name, "tasks.sqlite3")
        self.env = patch.dict(os.environ, {"TASK_DATABASE_PATH": self.db_path})
        self.env.start()

    def tearDown(self):
        self.env.stop()
        self.temp_dir.cleanup()

    def test_tasks_persist_and_are_scoped_to_owner(self):
        task = create_task("owner-a", "Prepare video", "Write the script")
        self.assertEqual(task["status"], "pending")
        self.assertEqual(len(list_tasks("owner-a")), 1)
        self.assertEqual(list_tasks("owner-b"), [])

    def test_task_can_be_updated_completed_and_deleted(self):
        task = create_task("owner-a", "Study biology")
        updated = update_task("owner-a", task["id"], {"title": "Study chemistry"})
        self.assertEqual(updated["title"], "Study chemistry")
        completed = update_task("owner-a", task["id"], {"status": "completed"})
        self.assertEqual(completed["status"], "completed")
        self.assertTrue(delete_task("owner-a", task["id"]))
        self.assertEqual(list_tasks("owner-a"), [])

    def test_invalid_due_date_and_title_are_rejected(self):
        with self.assertRaises(ValueError):
            create_task("owner-a", "   ")
        with self.assertRaises(ValueError):
            create_task("owner-a", "Task", due_at="2026-11-20T10:00:00")

    def test_task_cannot_be_changed_by_another_owner(self):
        task = create_task("owner-a", "Private task")
        self.assertIsNone(update_task("owner-b", task["id"], {"status": "completed"}))
        self.assertFalse(delete_task("owner-b", task["id"]))
        self.assertEqual(list_tasks("owner-a")[0]["status"], "pending")


class TaskApiTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = os.path.join(self.temp_dir.name, "api-tasks.sqlite3")
        self.env = patch.dict(os.environ, {
            "TASK_DATABASE_PATH": self.db_path,
            "OWNER_USERNAME": "test-owner",
            "OWNER_PASSWORD_HASH": "test-hash",
            "SECRET_KEY": "test-secret",
        })
        self.env.start()
        server.app.secret_key = "test-secret"
        server.app.config["SESSION_COOKIE_SECURE"] = False
        self.client = server.app.test_client()
        with self.client.session_transaction() as session:
            session["owner_username"] = "test-owner"
            session["task_csrf"] = "csrf-test-token"

    def tearDown(self):
        self.env.stop()
        self.temp_dir.cleanup()

    def test_task_api_requires_csrf_for_writes(self):
        denied = self.client.post("/api/tasks", json={"title": "No token"})
        self.assertEqual(denied.status_code, 403)

    def test_task_api_create_list_update_delete(self):
        headers = {"X-CSRF-Token": "csrf-test-token"}
        created = self.client.post(
            "/api/tasks", json={"title": "Prepare a short video"}, headers=headers
        )
        self.assertEqual(created.status_code, 201)
        task_id = created.get_json()["task"]["id"]

        listed = self.client.get("/api/tasks")
        self.assertEqual(listed.status_code, 200)
        self.assertEqual(len(listed.get_json()["tasks"]), 1)

        updated = self.client.patch(
            "/api/tasks/" + task_id,
            json={"status": "completed"},
            headers=headers,
        )
        self.assertEqual(updated.status_code, 200)
        self.assertEqual(updated.get_json()["task"]["status"], "completed")

        deleted = self.client.delete("/api/tasks/" + task_id, headers=headers)
        self.assertEqual(deleted.status_code, 200)
        self.assertEqual(self.client.get("/api/tasks").get_json()["tasks"], [])


if __name__ == "__main__":
    unittest.main()
