"""Feature entitlement rules for Data Detective AI.

This module describes product access only. It does not authenticate users,
verify payments, or grant access safely on its own. Call it only after a
trusted login and server-side payment verification.
"""

FOUNDER_USERNAME_ENV = "FOUNDER_USERNAME"

FEATURES = {
    "chat": {"label": "AI Chat", "plans": ["free", "pro", "founder"]},
    "data_analysis": {"label": "CSV Data Analysis", "plans": ["free", "pro", "founder"]},
    "image_generation": {"label": "Image Generation", "plans": ["pro", "founder"]},
    "music_generation": {"label": "Music Generation", "plans": ["pro", "founder"]},
    "video_generation": {"label": "Video Generation", "plans": ["pro", "founder"]},
    "youtube_automation": {"label": "YouTube Automation", "plans": ["pro", "founder"]},
    "scheduled_tasks": {"label": "Scheduled Tasks", "plans": ["pro", "founder"]},
    "advanced_agents": {"label": "Advanced AI Agents", "plans": ["pro", "founder"]},
}


def plan_for_user(username, configured_founder_username, paid_plan=None):
    """Return founder/pro/free after identity and payment are verified elsewhere."""
    if username and configured_founder_username and (
        username.strip().casefold() == configured_founder_username.strip().casefold()
    ):
        return "founder"
    if paid_plan in ("pro", "founder"):
        return paid_plan
    return "free"


def can_use_feature(feature_id, plan):
    """Check whether a verified account plan includes a feature."""
    feature = FEATURES.get(feature_id)
    if not feature:
        return False
    return plan in feature["plans"]
