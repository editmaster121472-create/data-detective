# Feature Scope — Media, Creator Automation, and Plans

## Product features requested

The platform is intended to offer:

- Chat Mode for text questions and answers.
- Image generation.
- Original music generation.
- Video generation.
- Creator workflow: topic ideas, script, storyboard, video, music, thumbnail, title, description, and YouTube upload scheduling.
- CSV analysis, charts, coding, research, writing, education, translation, and other specialist agents.
- Agent Mode for tasks that can be planned and executed with user permissions.

## Founder and Pro access

The feature entitlements are listed in `config/feature_access.py`.

- Founder: subscription price $0 and access to all configured Pro features.
- Pro: paid access to image, music, video, creator automation, and advanced agents.
- Free: basic chat/data access, subject to whatever limits are configured later.

The proposed price catalogue is in `config/pricing.py`: $1 one visit, $5 monthly, and $50 yearly.

**Important:** pricing and access rules are not a payment system. Before launch, add secure authentication, server-side payment verification, subscription expiry handling, and provider usage limits. Founder status must be assigned from a trusted server-side setting, never from a user-editable form.

## Media provider requirements

Image, music, and video generation are represented by provider-neutral adapters in `services/media_generation.py`. They do not generate media yet. A real provider must be selected, integrated, tested, and configured with credentials stored outside Git. Some providers charge fees or impose quotas.

## Autonomous creator workflow

`services/creator_workflow.py` prepares a transparent workflow plan. It does not yet create or upload videos. YouTube automation requires the channel owner's OAuth consent and a working YouTube Data API integration. Publishing should require approval by default; unattended publishing can only be enabled after the owner explicitly configures and authorizes it.

No AI can guarantee that a video will go viral.

## Analytics and user-question monitoring

**Not included in this phase, as requested.**

Do not build a dashboard that counts users, records every question, or displays individual user conversations at this stage. If product analytics are added later, prefer aggregated, privacy-respecting metrics with clear notice, retention limits, and access controls.
