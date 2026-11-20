"""Optional owner-only authentication for the personal assistant.

Authentication becomes mandatory for the dashboard and private API routes only
when OWNER_USERNAME, OWNER_PASSWORD_HASH, and SECRET_KEY are configured.
Until then, the existing public demo behaviour is preserved for safe setup.
"""
import os
import secrets
from functools import wraps

from flask import jsonify, redirect, request, session, url_for
from werkzeug.security import check_password_hash


def auth_is_configured():
    return all(os.getenv(name, "").strip() for name in (
        "OWNER_USERNAME", "OWNER_PASSWORD_HASH", "SECRET_KEY"
    ))


def owner_is_authenticated():
    expected = os.getenv("OWNER_USERNAME", "").strip()
    actual = session.get("owner_username", "")
    return bool(expected and actual and secrets.compare_digest(actual, expected))


def owner_required(view):
    @wraps(view)
    def wrapped(*args, **kwargs):
        # Keep the current demo usable until the owner completes private setup.
        if not auth_is_configured():
            return view(*args, **kwargs)
        if owner_is_authenticated():
            return view(*args, **kwargs)
        if request.path.startswith("/api/"):
            return jsonify({"error": "Owner login required. Please sign in first."}), 401
        return redirect(url_for("login"))
    return wrapped


def verify_owner_credentials(username, password):
    configured_username = os.getenv("OWNER_USERNAME", "").strip()
    password_hash = os.getenv("OWNER_PASSWORD_HASH", "").strip()
    if not configured_username or not password_hash:
        return False
    username_ok = secrets.compare_digest(
        str(username or "").strip(), configured_username
    )
    try:
        password_ok = check_password_hash(password_hash, str(password or ""))
    except (ValueError, TypeError):
        password_ok = False
    return username_ok and password_ok
