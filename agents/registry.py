"""Initial agent catalogue and lightweight task router.

This is a foundation only: provider-backed generation, research, and chat
must be connected separately before those capabilities become operational.
"""

AGENTS = [
    {
        "id": "main",
        "name": "Main Assistant",
        "description": "Understands the request and directs it to a specialist.",
        "status": "foundation",
        "keywords": [],
    },
    {
        "id": "general_chat",
        "name": "Chat and Answers Agent",
        "description": "General conversation and question answering through a configured language model.",
        "status": "provider_required",
        "keywords": ["question", "answer", "explain", "chat", "سوال", "جواب"],
    },
    {
        "id": "data",
        "name": "Data Analysis Agent",
        "description": "Inspects CSV data, calculates summaries, and supports data questions.",
        "status": "available_in_current_app",
        "keywords": ["csv", "data", "dataset", "sales", "spreadsheet", "ڈیٹا", "فائل"],
    },
    {
        "id": "research",
        "name": "Research Agent",
        "description": "Researches topics using an approved search or knowledge provider.",
        "status": "provider_required",
        "keywords": ["research", "search", "latest", "source", "تحقیق", "تلاش"],
    },
    {
        "id": "image",
        "name": "Image Generation Agent",
        "description": "Creates images after an image-generation provider is connected.",
        "status": "provider_required",
        "keywords": ["image", "picture", "photo", "thumbnail", "logo", "تصویر"],
    },
    {
        "id": "video",
        "name": "Video Generation Agent",
        "description": "Creates video drafts after a video-generation provider is connected.",
        "status": "provider_required",
        "keywords": ["video", "shorts", "reel", "clip", "ویڈیو"],
    },
    {
        "id": "music",
        "name": "Music Generation Agent",
        "description": "Creates original music after a licensed music provider is connected.",
        "status": "provider_required",
        "keywords": ["music", "song", "melody", "beat", "میوزک", "گانا"],
    },
    {
        "id": "coding",
        "name": "Coding Agent",
        "description": "Helps write, explain, and review code.",
        "status": "provider_required",
        "keywords": ["code", "coding", "python", "javascript", "program", "کوڈ"],
    },
    {
        "id": "writing",
        "name": "Writing Agent",
        "description": "Drafts posts, scripts, descriptions, and other text.",
        "status": "provider_required",
        "keywords": ["write", "writing", "script", "title", "description", "لکھ"],
    },
    {
        "id": "education",
        "name": "Education Agent",
        "description": "Explains educational topics and supports learning.",
        "status": "provider_required",
        "keywords": ["study", "biology", "chemistry", "math", "science", "تعلیم"],
    },
    {
        "id": "language",
        "name": "Language Agent",
        "description": "Translates text and explains vocabulary.",
        "status": "provider_required",
        "keywords": ["translate", "translation", "meaning", "synonym", "ترجمہ"],
    },
    {
        "id": "planning",
        "name": "Planning and Automation Agent",
        "description": "Plans approved tasks and scheduled workflows.",
        "status": "foundation",
        "keywords": ["schedule", "remind", "upload", "automate", "شیڈول"],
    },
    {
        "id": "youtube",
        "name": "YouTube Creator Agent",
        "description": "Prepares and, after OAuth permission, schedules YouTube uploads.",
        "status": "provider_and_oauth_required",
        "keywords": ["youtube", "channel", "upload video", "یوٹیوب", "اپلوڈ"],
    },
    {
        "id": "charts",
        "name": "Charts Agent",
        "description": "Builds charts from available structured data.",
        "status": "planned",
        "keywords": ["chart", "graph", "plot", "گراف", "چارٹ"],
    },
    {
        "id": "safety",
        "name": "Safety Agent",
        "description": "Checks requests and helps keep actions lawful and authorized.",
        "status": "foundation",
        "keywords": ["hack", "password", "wifi", "security", "ہیک", "پاس ورڈ"],
    },
    {
        "id": "fact_check",
        "name": "Fact Checking Agent",
        "description": "Checks factual claims against reliable sources when connected.",
        "status": "provider_required",
        "keywords": ["fact check", "verify", "source", "حقیقت", "تصدیق"],
    },
]


def get_agent(agent_id):
    """Return an agent definition by ID, or None."""
    return next((agent for agent in AGENTS if agent["id"] == agent_id), None)


def route_task(message):
    """Return the best keyword-based agent match.

    This is a simple starter router, not an AI model. The web app can call it
    after authentication and before dispatching to a real provider.
    """
    text = (message or "").strip().lower()
    if not text:
        return get_agent("main")

    matches = []
    for agent in AGENTS:
        score = sum(1 for word in agent["keywords"] if word in text)
        if score:
            matches.append((score, agent))

    if not matches:
        return get_agent("general_chat")

    matches.sort(key=lambda item: item[0], reverse=True)
    return matches[0][1]
