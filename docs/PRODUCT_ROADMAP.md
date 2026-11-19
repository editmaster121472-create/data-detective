# Data Detective AI — Product Roadmap

## Product direction

Build one platform with two connected experiences:

- **Chat Mode:** users ask questions and receive text answers.
- **Agent Mode:** users assign tasks that the system plans and performs with explicit permissions.

## Initial agent catalogue

The starter registry is in `agents/registry.py`. It includes chat, CSV data analysis, research, image generation, video generation, music generation, coding, writing, education, translation, planning, YouTube creation, charts, safety, and fact checking.

The registry is a catalogue and simple keyword router. It is not a general-purpose AI model. Provider-backed capabilities must be integrated and tested before being advertised as working.

## Pricing proposal

Configured in `config/pricing.py`:

- One Visit: USD $1
- Monthly Pro: USD $5
- Yearly Pro: USD $50
- Founder Premium: USD $0 subscription price

These are proposed display prices only. No checkout, payment verification, refunds, taxes, or recurring billing is implemented yet. External model, image, video, music, storage, and hosting providers may charge separately.

## Build sequence

1. Preserve the working CSV application and add modular agent foundations.
2. Add secure user accounts, sessions, founder role, and per-user access checks.
3. Add a real language-model provider for Chat Mode, with clear usage limits.
4. Connect research and fact-checking with source citations.
5. Add image, music, and video generation providers; show honest availability and quotas.
6. Add YouTube OAuth with least-privilege scopes. Require user review/approval before publishing by default.
7. Add scheduled jobs, retry handling, job history, and notifications.
8. Add a payment provider and verify payments server-side before granting Pro access.
9. Add charts and richer CSV analysis.
10. Test privacy, security, accessibility, mobile layout, and deployment.

## Security and trust requirements

- Never store passwords, API keys, OAuth tokens, or payment secrets in Git.
- Use environment variables or a secure secret store for credentials.
- Founder-only pages must be protected by server-side authentication and role checks, not just hidden buttons.
- Keep appropriate security and billing audit logs; do not promise invisible activity.
- Do not expose one user's files, prompts, reports, or account details to another user.
- Only automate actions on accounts the user owns or is authorized to manage.
- YouTube uploads must use the account owner's OAuth consent and respect platform policies.
- Do not provide instructions for unauthorized access, credential theft, or cracking another person's passwords.
- Do not promise that generated content will go viral.

## Deployment note

These files are additive foundations. They do not change the existing Flask routes, frontend, or PythonAnywhere deployment. The new modules can be integrated into `server.py` in a later, separately tested step.
