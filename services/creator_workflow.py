"""Plan a creator workflow; actual execution requires connected services.

The workflow intentionally stops for user approval before publishing.
"""

REQUIRED_STEPS = [
    "create_topic_ideas",
    "write_script",
    "prepare_storyboard",
    "generate_video",
    "generate_or_select_music",
    "create_thumbnail",
    "write_title_and_description",
    "review_content_and_rights",
    "request_owner_approval",
    "upload_via_youtube_oauth",
    "schedule_or_publish",
    "check_performance_metrics",
]


def build_creator_plan(topic, format_name="shorts", publish_mode="approval_required"):
    """Return a transparent task plan without pretending to execute it."""
    if not isinstance(topic, str) or not topic.strip():
        raise ValueError("A video topic is required.")
    if publish_mode not in ("approval_required", "auto_after_explicit_setup"):
        raise ValueError("Unsupported publishing mode.")
    return {
        "topic": topic.strip(),
        "format": format_name,
        "publish_mode": publish_mode,
        "steps": REQUIRED_STEPS.copy(),
        "execution_ready": False,
        "reason": (
            "Video, music, image, and YouTube OAuth providers must be connected "
            "before this workflow can execute. Publishing requires channel-owner authorization."
        ),
    }
