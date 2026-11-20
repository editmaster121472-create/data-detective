# Data Detective AI — Complete Personal Assistant Feature Catalogue

This is the product scope for the private, owner-operated assistant. Features are built in working stages; a tile or roadmap entry is not evidence that an integration is connected.

## Core assistant
- General chat, multi-turn context, conversation history and search
- Urdu and English support, translation, writing, tutoring and research
- Model/provider selection with visible availability and quota information
- File upload and analysis with safe size/type limits
- Web research with source links and uncertainty disclosure

## Voice and audio
- Push-to-talk and hands-free voice modes
- Separate speaker/read-aloud control, stop and interrupt controls
- Speech transcription, Urdu/English language selection and spoken replies
- Audio upload, transcription and summarization when a supported provider is configured

## Image Studio
- Image generation from text
- Image-to-image editing, background removal, resize/crop and format conversion
- Thumbnail, poster and social creative templates
- Image understanding and OCR
- Provider status, cost/quota display and safe upload handling

## Video Studio
- Text-to-video and image-to-video through a configured generation provider
- Browser/server editing tools: trim, crop, resize, captions, subtitles and title cards
- Voice-over, audio mix and export presets
- Thumbnail and metadata preparation
- Clear distinction between local editing tools and provider-based generation

## Documents and data
- PDF create, read, merge/split and summarize
- DOCX, XLSX, CSV and presentation creation/export
- Spreadsheet formulas, data cleaning, charts and report generation
- Preserve the existing CSV investigation and follow-up workflow

## Personal agent
- Private task inbox: create, edit, complete, cancel and review tasks
- One-time and recurring reminders with timezone awareness
- Durable scheduler and task execution history
- Owner-controlled memory with view/edit/delete controls
- Approval queue for external or consequential actions
- Notifications for completed, failed or approval-required tasks

## Connected services
- Gmail OAuth: search, summarize, label and draft; sending requires approval or an explicitly configured narrow rule
- YouTube OAuth: prepare uploads, metadata and thumbnails; publishing defaults to private/unlisted until approved
- Calendar integration when authorized
- Disconnect/revoke controls and least-privilege permissions

## Founder-only controls
- Owner authentication and secure sessions
- Account/security settings, provider configuration status and usage limits
- Audit log and integration permission review
- Private cybersecurity learning lab limited to lawful defensive education and testing on systems the owner is authorized to assess

## Engineering and release rules
- Never commit API keys, OAuth secrets, passwords or refresh tokens.
- Do not describe a planned tile as a working feature.
- Do not promise unlimited free AI generation or unattended execution on hosting that cannot reliably run scheduled jobs.
- Require confirmation for sending email, public publishing, deleting data or other externally visible/high-impact actions.
- Keep private tasks, memory and connected-account data behind owner authentication.
- Every stage must preserve existing chat, voice and CSV analysis behavior and include a practical test checklist.
