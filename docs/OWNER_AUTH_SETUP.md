# Private owner sign-in setup

The personal-assistant dashboard and its chat/CSV API routes can be restricted to one owner. Authentication is activated only when all three settings below exist in the server's private environment:

- `OWNER_USERNAME`: the owner's chosen login name.
- `OWNER_PASSWORD_HASH`: a Werkzeug password hash, never the plain password.
- `SECRET_KEY`: a long random Flask session-signing secret.

## Generate the values on the server

Run these commands in the PythonAnywhere Bash console. The password prompt uses hidden input:

```bash
python -c "from getpass import getpass; from werkzeug.security import generate_password_hash; print(generate_password_hash(getpass('Owner password: ')))"
python -c "import secrets; print(secrets.token_urlsafe(48))"
```

Copy the first command's output into `OWNER_PASSWORD_HASH` and the second command's output into `SECRET_KEY`. Choose your own username for `OWNER_USERNAME`.

## Add to the private .env file

Add these lines to `/home/Ali1214/mysite/.env` (do not add real values to GitHub):

```dotenv
OWNER_USERNAME=your-chosen-username
OWNER_PASSWORD_HASH=paste-the-generated-password-hash
SECRET_KEY=paste-the-generated-random-secret
SESSION_COOKIE_SECURE=1
```

Keep the existing `GROQ_API_KEY` line unchanged. Never paste passwords, hashes, or secrets into chat, GitHub, screenshots, or public issue comments.

After saving the file, reload the PythonAnywhere web app. The dashboard will then redirect signed-out visitors to `/login`; chat, CSV analysis, follow-ups, and reports will require the owner session.

## Current limitations

- If the three required settings are absent, the app preserves its existing demo behaviour so the owner is not accidentally locked out before setup.
- This is single-owner password authentication, not multi-factor authentication.
- Gmail, YouTube, persistent tasks, durable memory, and background scheduling are separate later stages and are not enabled by this sign-in foundation.
- Do not deploy this branch until the owner has added the private settings and tested sign-in on the live host.
