# Task Inbox

The Task Inbox is a private, single-owner task list backed by SQLite.

## Current capabilities

- Create tasks with a required title, optional notes, and optional due date/time.
- List tasks after owner sign-in.
- Edit task title and notes.
- Mark tasks complete and reopen them.
- Delete tasks permanently.
- Persist tasks across browser refreshes and web-app restarts.
- Keep task rows scoped to the signed-in owner username.
- Require the owner session and a per-session CSRF token for writes.

## Storage

By default, the SQLite database is created at `instance/tasks.sqlite3` inside the application folder. To store it elsewhere, set `TASK_DATABASE_PATH` in the private server environment to an absolute writable path. Back up this database file as part of the site's data backups.

## Security

Task API routes return an error until owner authentication is configured. They are not enabled by the demo-mode authentication bypass. Never put the database file, owner password, password hash, Flask secret, or API keys in Git.

## Not yet included

A due date is saved and displayed; it does not itself send a notification. Recurring reminders, email/push notifications, reliable background execution, task-to-Gmail/YouTube actions, and an activity/audit history require the later scheduler and integrations phases. Do not tell the owner a scheduled task ran unless a job runner confirms it.
