# Data Detective

A Flask web app for CSV question-answering using the OpenAI Agents API and an OpenAI-hosted Python sandbox.

## What it includes
- CSV upload (10 MB application limit) and an included sample sales dataset.
- OpenAI Agents API session created with `environment.type = openai_hosted`.
- Python packages (pandas, numpy, matplotlib) installed in the hosted sandbox; network access disabled.
- Progress events collected from the streamed agent turn.
- Follow-up questions sent to the same Agents API session.
- Downloadable Markdown findings report.
- API key loaded only by the server from environment variables.

## Run locally
1. Install Python 3.10+.
2. `python -m venv .venv` and activate it.
3. `pip install -r requirements.txt`
4. Copy `.env.example` to `.env` and add your OpenAI API key.
5. `python server.py`
6. Open `http://127.0.0.1:5000`.

The API key needs Agents API session permissions (`api.agents.read`, `api.agents.write`) and Responses inference permission (`api.responses.write`). The Agents API quickstart documents these permissions. Usage of the model and hosted sandbox may incur API charges.

## Deploying
This is a server application, not a static GitHub Pages site. Deploy it to a Python-capable host (for example a service that supports Flask), set `OPENAI_API_KEY` as a server environment secret, and use the provider's HTTPS URL. Do not commit `.env`.

## Production hardening before public launch
- Replace the in-memory `sessions` dictionary with a database and add authentication/ownership checks.
- Add rate limiting, CSRF protection, upload scanning, retention/deletion controls, and per-user quotas.
- Add robust artifact listing/download handling for chart files and report files from the Agents API. The current report download contains the agent's text response in Markdown; the agent is instructed to write charts and a full report in its hosted workspace.
- Validate CSV encoding and structure more strictly, and set operational timeouts/observability.

## Notes
OpenAI-hosted sandbox sessions are temporary and may expire after inactivity. The sample CSV can be downloaded from the app.
