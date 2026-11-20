# Data Detective AI — Personal Assistant Build Plan

## Product goal
Turn the existing Data Detective website into a private, voice-enabled personal AI workspace for its owner. Preserve the existing CSV analysis feature.

## Current baseline
- Flask application with a general chat endpoint backed by Groq.
- Local CSV analysis, chart data, follow-up questions, and downloadable reports.
- Browser-side chat history and speech features.
- No owner authentication, durable task store, Gmail OAuth, YouTube OAuth, background job runner, or durable long-term memory yet.

## Build principles
1. Build for one owner first; do not expose private email, tasks, or tokens publicly.
2. Keep API keys, OAuth client secrets, refresh tokens, and passwords out of Git.
3. Use least-privilege Google OAuth scopes and let the owner disconnect integrations.
4. Require a review/confirmation before sending emails, publishing videos, deleting files, or other externally visible/high-impact actions. Allow narrowly scoped pre-approvals only when explicitly configured.
5. Record task status and action history so the owner can see what ran, when, and what failed.
6. Keep existing CSV analysis working.
7. Prefer free/open options where practical, but clearly show quotas and service limits. Do not promise unlimited free usage.

## Phased implementation

### Phase 1 — Private assistant foundation
- Add owner-only authentication and secure session handling.
- Add a persistent database for chat/task metadata and preferences.
- Add a task inbox: create, view, edit, cancel, and mark tasks complete.
- Add reminders and a task activity log.
- Add clear permission controls and a safe approval queue.
- Protect all private endpoints; no credentials or personal task data in browser local storage.

**Acceptance:** unauthenticated visitors cannot read or change private assistant data; tasks survive app reloads; the CSV analyzer still works.

### Phase 2 — Reliable scheduling and memory
- Add a durable job queue and scheduled task runner suitable for the selected hosting plan.
- Support one-time and recurring reminders with timezone-aware scheduling.
- Add owner-controlled memory: view, edit, and delete saved preferences/facts.
- Notify the owner about completed, failed, or approval-required tasks.

**Acceptance:** scheduled jobs survive web worker restarts and failures are visible; do not claim a task ran unless the runner confirms it.

### Phase 3 — Gmail integration
- Add Google OAuth with minimal Gmail scopes.
- Start with inbox summaries, search, labels, and draft replies.
- Add send capability only behind explicit approval or a narrowly configured allowlist/rule.
- Keep an audit log and provide disconnect/revoke controls.

**Acceptance:** no email is sent merely because the model generated a draft; every send is authorized under the owner's chosen policy.

### Phase 4 — YouTube integration
- Add Google OAuth and YouTube Data API integration.
- Support video upload metadata, thumbnails where supported, privacy settings, and scheduled publishing.
- Default to private/unlisted draft or approval before public publishing.
- Show upload progress, quota errors, and final video link.

**Acceptance:** publishing is only performed after explicit approval or a narrowly defined pre-approval; quota and upload failures are reported honestly.

### Phase 5 — Advanced AI workspace
- Improve chat UX, conversation search, file handling, web research, and model/provider selection.
- Add image understanding/generation and audio/video workflows only through providers that support them.
- Add voice conversation controls, transcription, spoken responses, and interruption/stop controls.
- Add CSV/data analysis enhancements and export formats.

**Acceptance:** each feature shows its provider, permission needs, and usage limits; unsupported features are clearly labelled rather than simulated.

## Hosting and cost constraints
- PythonAnywhere web requests alone are not a reliable background scheduler. Select a supported scheduled-task/worker approach before promising unattended execution.
- Groq and other hosted model providers have quotas and can change free-tier limits.
- Google APIs require project setup, OAuth consent, account authorization, and may have quotas or verification requirements.
- The owner must authorize Gmail and YouTube separately for this custom website; connecting apps inside ChatGPT does not connect them to this Flask app.

## Immediate next engineering step
Implement Phase 1 in a separate Git branch, beginning with owner authentication and persistent task storage. Do not add Gmail/YouTube actions until private access control and approval logging are in place.
