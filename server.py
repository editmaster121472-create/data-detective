import csv
import io
import json
import re
import uuid
from collections import Counter, defaultdict
from datetime import datetime
from flask import Flask, request, jsonify, render_template, send_file, abort

app = Flask(__name__, template_folder="templates", static_folder="static")
app.config["MAX_CONTENT_LENGTH"] = 12 * 1024 * 1024

# Free local CSV analyzer. No API key or paid AI service is required.
sessions = {}

SAMPLE_CSV = """Region,Sales,Units,Product
North,1200,4,Alpha
South,800,2,Beta
East,600,3,Gamma
West,1500,5,Alpha
North,950,3,Beta
"""

def parse_csv_text(text):
    sample = text[:4096]
    try:
        dialect = csv.Sniffer().sniff(sample, delimiters=",;\t|")
    except csv.Error:
        dialect = csv.excel
    rows = list(csv.DictReader(io.StringIO(text), dialect=dialect))
    if not rows:
        raise ValueError("CSV file is empty or has no header row.")
    return rows, list(rows[0].keys())

def numeric_values(rows, column):
    values = []
    for row in rows:
        raw = str(row.get(column, "")).strip().replace(",", "")
        raw = re.sub(r"^[\$£€₹₨]\s*", "", raw)
        try:
            if raw and raw.lower() not in ("na", "n/a", "none", "null"):
                values.append(float(raw))
        except ValueError:
            pass
    return values

def profile_data(rows, columns):
    numeric = {}
    for col in columns:
        vals = numeric_values(rows, col)
        if vals:
            numeric[col] = {
                "count": len(vals), "total": round(sum(vals), 2),
                "average": round(sum(vals) / len(vals), 2),
                "minimum": round(min(vals), 2), "maximum": round(max(vals), 2)
            }
    missing = sum(1 for row in rows for col in columns if not str(row.get(col, "")).strip())
    duplicate_count = len(rows) - len({tuple(str(row.get(c, "")) for c in columns) for row in rows})
    return {"rows": len(rows), "columns": len(columns), "column_names": columns,
            "missing_cells": missing, "duplicate_rows": duplicate_count, "numeric": numeric}

def build_chart_data(rows, columns, profile):
    """Build a small grouped bar-chart dataset without external services."""
    date_words = ("date", "time", "timestamp", "year", "month")
    category_col = None
    for col in columns:
        if col in profile["numeric"] or any(word in col.lower() for word in date_words):
            continue
        values = {str(row.get(col, "")).strip() or "(blank)" for row in rows}
        if 2 <= len(values) <= 12:
            category_col = col
            break
    if not category_col:
        return None

    preferred = ("sales", "revenue", "amount", "profit", "units", "quantity")
    metric_col = next(
        (c for name in preferred for c in profile["numeric"] if c.lower() == name),
        next(iter(profile["numeric"]), None)
    )
    if not metric_col:
        return None

    totals = defaultdict(float)
    for row in rows:
        label = str(row.get(category_col, "")).strip() or "(blank)"
        vals = numeric_values([row], metric_col)
        if vals:
            totals[label] += vals[0]
    if not totals:
        return None
    ranked = sorted(totals.items(), key=lambda item: item[1], reverse=True)
    return {
        "title": f"{metric_col} by {category_col}",
        "category": category_col,
        "metric": metric_col,
        "labels": [item[0] for item in ranked],
        "values": [round(item[1], 2) for item in ranked]
    }


def answer_question(rows, columns, question, profile):
    q = (question or "").lower()
    parts = [f"Your dataset has {profile['rows']} rows and {profile['columns']} columns."]
    if any(w in q for w in ["missing", "empty", "blank", "خالی"]):
        parts.append(f"There are {profile['missing_cells']} missing cells.")
    if any(w in q for w in ["duplicate", "duplicated", "repeat"]):
        parts.append(f"There are {profile['duplicate_rows']} duplicate rows.")
    selected = None
    for col in columns:
        if col.lower() in q and col in profile["numeric"]:
            selected = col
            break
    if selected is None:
        for col in profile["numeric"]:
            if col.lower() in ("sales", "revenue", "amount", "price", "units", "quantity", "profit") and col.lower() in q:
                selected = col
                break
    if selected is None and len(profile["numeric"]) == 1:
        selected = next(iter(profile["numeric"]))
    if selected:
        s = profile["numeric"][selected]
        parts.append(f"{selected}: total {s['total']}, average {s['average']}, minimum {s['minimum']}, maximum {s['maximum']}.")
    # Detect common grouping requests, including "for each region" and "region-wise".
    group_col = None
    for col in columns:
        col_name = col.lower().strip()
        phrases = (
            "by " + col_name, "per " + col_name,
            "for each " + col_name, "each " + col_name,
            "for every " + col_name, col_name + "-wise",
            col_name + " wise"
        )
        if any(phrase in q for phrase in phrases):
            group_col = col
            break
    if group_col:
        sum_col = selected
        if not sum_col:
            preferred = ("units", "quantity") if any(w in q for w in ["product", "most sold", "units", "quantity"]) else ("sales", "revenue", "amount", "profit")
            sum_col = next((c for name in preferred for c in profile["numeric"] if c.lower() == name), None)
        if not sum_col:
            sum_col = next(iter(profile["numeric"]), None)
        if sum_col:
            grouped = defaultdict(float)
            for row in rows:
                key = str(row.get(group_col, "")).strip() or "(blank)"
                vals = numeric_values([row], sum_col)
                if vals:
                    grouped[key] += vals[0]
            if grouped:
                ranking = sorted(grouped.items(), key=lambda x: x[1], reverse=True)
                parts.append(f"{sum_col} grouped by {group_col}: " +
                             "; ".join(f"{k}: {round(v,2)}" for k, v in ranking) + ".")
                if any(w in q for w in ["highest", "most", "top", "maximum", "best"]):
                    top_name, top_value = ranking[0]
                    parts.append(f"Highest {sum_col} by {group_col}: {top_name} ({round(top_value,2)}).")
    if not question.strip():
        parts.append("Ask a question about totals, averages, missing values, duplicates, or a column.")
    elif len(parts) == 1:
        parts.append("I can inspect this CSV for totals, averages, minimums, maximums, missing values, duplicates, and simple group totals. I am a free local data-analysis tool, not a general-purpose AI chatbot.")
    return " ".join(parts)

@app.get("/")
def home():
    return render_template("index.html")

@app.get("/api/sample")
def sample():
    output = io.BytesIO(SAMPLE_CSV.encode("utf-8"))
    output.seek(0)
    return send_file(output, mimetype="text/csv", as_attachment=True,
                     download_name="sample_sales.csv")

@app.post("/api/analyze")
def analyze():
    uploaded = request.files.get("file")
    question = request.form.get("question", "")
    if not uploaded:
        return jsonify({"error": "Please choose a CSV file first."}), 400
    try:
        raw = uploaded.read().decode("utf-8-sig")
        rows, columns = parse_csv_text(raw)
        profile = profile_data(rows, columns)
        session_id = str(uuid.uuid4())
        sessions[session_id] = {"rows": rows, "columns": columns, "profile": profile,
                                "filename": uploaded.filename or "dataset.csv"}
        answer = answer_question(rows, columns, question, profile)
        return jsonify({"id": session_id, "session_id": session_id, "answer": answer,
                        "report_url": f"/api/report/{session_id}", "profile": profile,
                        "chart": build_chart_data(rows, columns, profile)})
    except Exception as exc:
        return jsonify({"error": f"Could not analyze this CSV: {exc}"}), 400

@app.post("/api/followup/<session_id>")
def followup(session_id):
    data = request.get_json(silent=True) or {}
    question = str(data.get("question", ""))
    session = sessions.get(session_id)
    if not session:
        return jsonify({"error": "This analysis session has expired. Please upload the CSV again."}), 404
    answer = answer_question(session["rows"], session["columns"], question, session["profile"])
    return jsonify({"id": session_id, "answer": answer, "report_url": f"/api/report/{session_id}",
                        "chart": build_chart_data(session["rows"], session["columns"], session["profile"])})

@app.get("/api/report/<session_id>")
def report(session_id):
    session = sessions.get(session_id)
    if not session:
        abort(404)
    report_data = {
        "created_at": datetime.utcnow().isoformat() + "Z",
        "filename": session["filename"],
        "profile": session["profile"],
        "note": "Generated by the free local CSV analyzer. No external AI API was used."
    }
    output = io.BytesIO(json.dumps(report_data, indent=2).encode("utf-8"))
    output.seek(0)
    return send_file(output, mimetype="application/json", as_attachment=True,
                     download_name="data_detective_report.json")

@app.errorhandler(413)
def too_large(_error):
    return jsonify({"error": "File is too large. Maximum size is 12 MB."}), 413

if __name__ == "__main__":
    app.run(debug=False)
