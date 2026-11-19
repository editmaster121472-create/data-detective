"""Provider-neutral interfaces for image, music, and video generation.

No generation provider is configured by default. Add a supported provider
adapter and store credentials in environment variables, never in Git.
"""

import os


class ProviderNotConfigured(RuntimeError):
    """Raised when a media-generation provider has not been configured."""


PROVIDER_ENV = {
    "image": "IMAGE_GENERATION_PROVIDER",
    "music": "MUSIC_GENERATION_PROVIDER",
    "video": "VIDEO_GENERATION_PROVIDER",
}


def provider_status(media_type):
    """Return safe configuration status without exposing secret values."""
    if media_type not in PROVIDER_ENV:
        raise ValueError("Unsupported media type.")
    provider = os.getenv(PROVIDER_ENV[media_type], "").strip()
    return {
        "media_type": media_type,
        "configured": bool(provider),
        "provider": provider if provider else None,
        "ready": False,
        "note": "A provider adapter and credentials are required before generation works.",
    }


def generate_media(media_type, prompt, **options):
    """Dispatch to a configured provider adapter.

    Deliberately fails closed until a real provider integration is implemented.
    This prevents the app from pretending that a video/image/song was created.
    """
    if media_type not in PROVIDER_ENV:
        raise ValueError("Unsupported media type.")
    if not isinstance(prompt, str) or not prompt.strip():
        raise ValueError("Please provide a prompt.")
    status = provider_status(media_type)
    if not status["configured"]:
        raise ProviderNotConfigured(
            f"{media_type.title()} generation is not configured yet. "
            "Choose and connect a provider first."
        )
    raise ProviderNotConfigured(
        f"The selected {media_type} provider is named but its adapter is not implemented yet."
    )


def generate_image(prompt, **options):
    return generate_media("image", prompt, **options)


def generate_music(prompt, **options):
    return generate_media("music", prompt, **options)


def generate_video(prompt, **options):
    return generate_media("video", prompt, **options)
