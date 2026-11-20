"""Replicate-backed image and video generation endpoints.

The Replicate token is read only on the server and is never sent to the browser.
"""
import base64
import json
import os
import re
import urllib.error
import urllib.request
import urllib.parse
import uuid

from flask import Blueprint, jsonify, request, session

creative_bp = Blueprint("creative", __name__)
_predictions = {}


def _token():
    return os.getenv("REPLICATE_API_TOKEN", "").strip()


def _pollinations_key():
    return os.getenv("POLLINATIONS_API_KEY", "").strip()


def _pollinations_image(prompt):
    api_key = _pollinations_key()
    if not api_key:
        raise RuntimeError("Pollinations API key is not configured. Add POLLINATIONS_API_KEY to the private .env file.")
    url = "https://gen.pollinations.ai/image/" + urllib.parse.quote(prompt, safe="") + "?model=flux"
    req = urllib.request.Request(url, headers={
        "Authorization": "Bearer " + api_key,
        "Accept": "image/*",
    }, method="GET")
    try:
        with urllib.request.urlopen(req, timeout=120) as response:
            image_bytes = response.read(12 * 1024 * 1024 + 1)
            content_type = response.headers.get("Content-Type", "image/jpeg").split(";")[0]
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError("Pollinations image service HTTP " + str(exc.code) + ": " + detail[:400])
    if len(image_bytes) > 12 * 1024 * 1024:
        raise RuntimeError("Generated image is larger than the 12 MB limit.")
    if not content_type.startswith("image/"):
        raise RuntimeError("Pollinations did not return an image.")
    return "data:" + content_type + ";base64," + base64.b64encode(image_bytes).decode("ascii")

def _model(env_name, default):
    value = os.getenv(env_name, default).strip()
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", value):
        raise ValueError("Invalid model setting: " + env_name)
    return value


def _replicate(method, url, payload=None, wait=5):
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    headers = {"Authorization": "Bearer " + _token(), "Content-Type": "application/json"}
    if method == "POST":
        headers["Prefer"] = "wait=" + str(wait)
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=90) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        try:
            message = json.loads(detail).get("detail") or json.loads(detail).get("title") or detail
        except Exception:
            message = detail
        raise RuntimeError("Replicate HTTP " + str(exc.code) + ": " + str(message)[:500])


def _create(model, inputs):
    if not _token():
        return None, (jsonify({"error": "Replicate token is missing. Add REPLICATE_API_TOKEN to the server .env file."}), 503)
    owner, name = model.split("/", 1)
    result = _replicate("POST", "https://api.replicate.com/v1/models/" + owner + "/" + name + "/predictions",
                        {"input": inputs}, wait=5)
    prediction_id = str(result.get("id", ""))
    if not prediction_id:
        raise RuntimeError("Replicate did not return a prediction ID.")
    _predictions[prediction_id] = {"owner": session.get("owner_username", "local"), "created": True}
    return result, None


def _image_data_url(upload):
    if not upload or not upload.filename:
        return None
    mime = (upload.mimetype or "").lower()
    if mime not in ("image/png", "image/jpeg", "image/webp"):
        raise ValueError("Reference image must be PNG, JPG or WEBP.")
    raw = upload.read(7 * 1024 * 1024 + 1)
    if len(raw) > 7 * 1024 * 1024:
        raise ValueError("Reference image must be smaller than 7 MB.")
    if not raw:
        raise ValueError("The selected image is empty.")
    return "data:" + mime + ";base64," + base64.b64encode(raw).decode("ascii")


@creative_bp.get("/api/creative/config")
def creative_config():
    pollinations_key = _pollinations_key()
    return jsonify({
        "configured": bool(_token() or pollinations_key),
        "image_configured": bool(pollinations_key),
        "image_provider": "pollinations" if pollinations_key else "none",
        "video_configured": bool(_token()),
        "image_model": "flux",
        "video_model": os.getenv("REPLICATE_VIDEO_MODEL", "minimax/video-01"),
    })


@creative_bp.post("/api/creative/image")
def generate_image():
    data = request.form
    prompt = str(data.get("prompt", "")).strip()
    if not prompt or len(prompt) > 1800:
        return jsonify({"error": "Write an image prompt (maximum 1,800 characters)."}), 400
    try:
        if not _pollinations_key():
            return jsonify({"error": "Pollinations image generation is not configured. Add POLLINATIONS_API_KEY to the private .env file. No paid Replicate image fallback will be used."}), 503
        if request.files.get("reference") and request.files["reference"].filename:
            return jsonify({"error": "Pollinations image generation currently supports text-to-image in this studio. Remove the reference image."}), 400
        image_data = _pollinations_image(prompt)
        return jsonify({"image_data": image_data, "provider": "pollinations"}), 200
    except Exception as exc:
        return jsonify({"error": str(exc)}), 502


@creative_bp.post("/api/creative/video")
def generate_video():
    data = request.form
    prompt = str(data.get("prompt", "")).strip()
    if not prompt or len(prompt) > 1200:
        return jsonify({"error": "Write a video prompt (maximum 1,200 characters)."}), 400
    try:
        inputs = {"prompt": prompt}
        reference = _image_data_url(request.files.get("reference"))
        if reference:
            inputs["first_frame_image"] = reference
        result, error = _create(_model("REPLICATE_VIDEO_MODEL", "minimax/video-01"), inputs)
        if error:
            return error
        return jsonify({"prediction": result}), 202
    except ValueError as exc:
        return jsonify({"error": str(exc)}), 400
    except Exception as exc:
        return jsonify({"error": str(exc)}), 502


@creative_bp.get("/api/creative/status/<prediction_id>")
def creative_status(prediction_id):
    record = _predictions.get(prediction_id)
    if not record or record["owner"] != session.get("owner_username", "local"):
        return jsonify({"error": "Prediction not found in this browser session."}), 404
    if not _token():
        return jsonify({"error": "Replicate token is not configured."}), 503
    try:
        result = _replicate("GET", "https://api.replicate.com/v1/predictions/" + prediction_id)
        return jsonify({"prediction": result})
    except Exception as exc:
        return jsonify({"error": str(exc)}), 502
