# Data Detective

A small Flask web application for free, local CSV inspection. It does not call the OpenAI API and does not require an API key or paid AI credits.

## Features
- Upload a CSV file (maximum 12 MB).
- Inspect row and column counts, missing cells, duplicate rows, and numeric summaries.
- Ask simple questions about totals, averages, minimums, maximums, and grouped totals.
- Ask follow-up questions about the same uploaded dataset.
- Download a JSON analysis report.
- Download a sample CSV from the application.

## Run locally
1. Install Python 3.10 or newer.
2. Create and activate a virtual environment.
3. Install dependencies: `pip install -r requirements.txt`
4. Start the app: `python server.py`
5. Open `http://127.0.0.1:5000`.

## Deploying
Deploy this Flask application on a Python-capable host such as PythonAnywhere. Configure the WSGI file to import `app` from `server.py`. Install dependencies from `requirements.txt`.

## Important limitations
This is a rule-based CSV analysis tool, not a general-purpose AI assistant. It cannot reliably answer unrelated general-knowledge questions or perform advanced statistical analysis. Uploaded data and sessions are held in server memory while the process runs; sessions may be lost after a restart. Do not upload sensitive or confidential data.

## Before public launch
Add user authentication, per-user data isolation, rate limiting, secure session storage, data-retention controls, and additional validation. The current in-memory session store is suitable only for a small demonstration.
