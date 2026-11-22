"""fal.ai image and video generation routes for the Data Detective web studio."""
import json
import os
import urllib.error
import urllib.parse
import urllib.request
import uuid

from flask import Blueprint, jsonify, request, session

creative_bp = Blueprint("creative", __name__)
_jobs = {}

IMAGE_MODEL = os.getenv("FAL_IMAGE_MODEL", "google/nano-banana-2.1").strip()
VIDEO_MODEL = os.getenv("FAL_VIDEO_MODEL", "minimax/h3-max/text-to-video").strip()


def _fal_key():
    return os.getenv("FAL_KEY", "").strip()


def _fal_call(url, method="GET", payload=None, timeout=120):
    key = _fal_key()
    if not key:
        raise RuntimeError("fal.ai is not configured. Add FAL_KEY to the private PythonAnywhere .env file.")
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    headers = {"Authorization": "Key " + key, "Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            raw = response.read()
        return json.loads(raw.decode("utf-8")) if raw else {}
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(detail)
            message = parsed.get("detail") or parsed.get("message") or parsed.get("error") or detail
        except Exception:
            message = detail
        raise RuntimeError("fal.ai HTTP " + str(exc.code) + ": " + str(message)[:500])


def _fal_queue_url(url):
    parsed = urllib.parse.urlparse(str(url or ""))
    if parsed.scheme != "https" or parsed.hostname not in ("queue.fal.run", "fal.run"):
        raise RuntimeError("fal.ai returned an unexpected queue URL.")
    return url


def _owner():
    return session.get("owner_username", "local")


def _normalise_video_result(result):
    video = result.get("video") if isinstance(result, dict) else None
    if isinstance(video, dict) and video.get("url"):
        return [video["url"]]
    if isinstance(video, str) and video.startswith("https://"):
        return [video]
    videos = result.get("videos") if isinstance(result, dict) else None
    if isinstance(videos, list):
        return [item.get("url") for item in videos if isinstance(item, dict) and item.get("url")]
    return []


@creative_bp.get("/api/creative/config")
def creative_config():
    ready = bool(_fal_key())
    return jsonify({
        "configured": ready,
        "image_configured": ready,
        "video_configured": ready,
        "provider": "fal.ai",
        "image_provider": "fal.ai" if ready else "none",
        "image_model": IMAGE_MODEL,
        "video_model": VIDEO_MODEL,
    })


@creative_bp.post("/api/creative/image")
def generate_image():
    prompt = str(request.form.get("prompt", "")).strip()
    if not prompt or len(prompt) > 1800:
        return jsonify({"error": "Write an image prompt (maximum 1,800 characters)."}), 400
    if not _fal_key():
        return jsonify({"error": "fal.ai is not configured. Add FAL_KEY to the private PythonAnywhere .env file."}), 503
    try:
        ratio = str(request.form.get("aspect_ratio", "1:1")).strip()
        allowed = {"1:1", "16:9", "9:16", "4:3", "3:4"}
        if ratio not in allowed:
            ratio = "1:1"
        result = _fal_call(
            "https://fal.run/" + IMAGE_MODEL,
            method="POST",
            payload={"prompt": prompt, "aspect_ratio": ratio, "num_images": 1, "resolution": "1K", "output_format": "jpeg"},
            timeout=180,
        )
        images = result.get("images", []) if isinstance(result, dict) else []
        urls = [item.get("url") for item in images if isinstance(item, dict) and item.get("url")]
        if not urls:
            raise RuntimeError("fal.ai completed the image request but returned no image URL.")
        return jsonify({"images": urls, "provider": "fal.ai"}), 200
    except Exception as exc:
        return jsonify({"error": str(exc)}), 502


@creative_bp.post("/api/creative/video")
def generate_video():
    prompt = str(request.form.get("prompt", "")).strip()
    if not prompt or len(prompt) > 1200:
        return jsonify({"error": "Write a video prompt (maximum 1,200 characters)."}), 400
    if not _fal_key():
        return jsonify({"error": "fal.ai is not configured. Add FAL_KEY to the private PythonAnywhere .env file."}), 503
    try:
        if request.files.get("reference") and request.files["reference"].filename:
            return jsonify({"error": "This video mode currently creates video from text. Image-to-video will be added as a separate mode."}), 400
        duration = int(request.form.get("duration", "5"))
        duration = max(2, min(15, duration))
        ratio = str(request.form.get("aspect_ratio", "16:9")).strip()
        if ratio not in {"21:9", "16:9", "4:3", "1:1", "3:4", "9:16"}:
            ratio = "16:9"
        payload = {
            "prompt": prompt,
            "prompt_expansion_mode": "balanced",
            "duration": duration,
            "resolution": "768P",
            "aspect_ratio": ratio,
            "enable_safety_checker": True,
        }
        result = _fal_call("https://queue.fal.run/" + VIDEO_MODEL, method="POST", payload=payload, timeout=45)
        request_id = str(result.get("request_id") or result.get("id") or "")
        if not request_id or not result.get("status_url") or not result.get("response_url"):
            raise RuntimeError("fal.ai did not return the video queue details.")
        _jobs[request_id] = {
            "owner": _owner(),
            "endpoint_id": VIDEO_MODEL,
            "status_url": _fal_queue_url(result["status_url"]),
            "response_url": _fal_queue_url(result["response_url"]),
        }
        return jsonify({"prediction": {"id": request_id, "status": "starting"}}), 202
    except Exception as exc:
        return jsonify({"error": str(exc)}), 502


@creative_bp.get("/api/creative/status/<request_id>")
def creative_status(request_id):
    job = _jobs.get(request_id)
    if not job or job["owner"] != _owner():
        return jsonify({"error": "Video request not found in this browser session. Start a new request."}), 404
    try:
        state = _fal_call(job["status_url"], timeout=45)
        status = str(state.get("status", "IN_PROGRESS")).upper()
        if status in ("IN_QUEUE", "IN_PROGRESS", "QUEUED"):
            return jsonify({"prediction": {"id": request_id, "status": "processing"}})
        if status in ("FAILED", "CANCELLED", "CANCELED"):
            return jsonify({"prediction": {"id": request_id, "status": "failed", "error": state.get("error") or "fal.ai video generation failed."}})
        if status == "COMPLETED":
            result = _fal_call(job["response_url"], timeout=90)
            urls = _normalise_video_result(result)
            if not urls:
                raise RuntimeError("fal.ai completed the video request but returned no video URL.")
            return jsonify({"prediction": {"id": request_id, "status": "succeeded", "output": urls}})
        return jsonify({"prediction": {"id": request_id, "status": "processing"}})
    except Exception as exc:
        return jsonify({"error": str(exc)}), 502
