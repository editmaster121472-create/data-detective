"""Authentication gate for Data Detective.

The public dashboard is intentionally accessible without an owner sign-in.
Private Task Inbox endpoints remain disabled until a secure owner-auth flow
is configured in a future update.
"""
from functools import wraps


def auth_is_configured():
    # Owner authentication is intentionally disabled for the public dashboard.
    return False


def owner_is_authenticated():
    return False


def owner_required(view):
    @wraps(view)
    def wrapped(*args, **kwargs):
        # Allow the public dashboard and its public assistant/data endpoints.
        return view(*args, **kwargs)
    return wrapped


def verify_owner_credentials(username, password):
    # Owner login is disabled. Do not accept credentials.
    return False
