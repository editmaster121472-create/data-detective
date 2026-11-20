import io
import unittest

import server


class CsvAnalysisTests(unittest.TestCase):
    def setUp(self):
        server.sessions.clear()
        self.client = server.app.test_client()
        self.csv_text = (
            "Region,Sales,Units,Product\n"
            "North,1200,4,Alpha\n"
            "South,800,2,Beta\n"
            "East,600,3,Gamma\n"
            "West,1500,5,Alpha\n"
            "North,950,3,Beta\n"
        )
        self.rows, self.columns = server.parse_csv_text(self.csv_text)
        self.profile = server.profile_data(self.rows, self.columns)

    def test_profile_counts_rows_columns_and_numeric_totals(self):
        self.assertEqual(self.profile["rows"], 5)
        self.assertEqual(self.profile["columns"], 4)
        self.assertEqual(self.profile["numeric"]["Sales"]["total"], 5050.0)
        self.assertEqual(self.profile["numeric"]["Units"]["average"], 3.4)

    def test_grouped_answer_finds_highest_region(self):
        answer = server.answer_question(
            self.rows, self.columns, "Show sales by region and highest", self.profile
        )
        self.assertIn("North (2150.0)", answer)

    def test_chart_is_grouped_and_ranked(self):
        chart = server.build_chart_data(self.rows, self.columns, self.profile)
        self.assertEqual(chart["metric"], "Sales")
        self.assertEqual(chart["category"], "Region")
        self.assertEqual(chart["labels"][0], "North")
        self.assertEqual(chart["values"][0], 2150.0)

    def test_analyze_endpoint_returns_report_and_chart(self):
        response = self.client.post(
            "/api/analyze",
            data={
                "file": (io.BytesIO(self.csv_text.encode("utf-8")), "sales.csv"),
                "question": "Show sales by region and highest",
            },
            content_type="multipart/form-data",
        )
        self.assertEqual(response.status_code, 200)
        payload = response.get_json()
        self.assertIn("id", payload)
        self.assertEqual(payload["chart"]["labels"][0], "North")
        report = self.client.get(payload["report_url"])
        self.assertEqual(report.status_code, 200)
        self.assertIn("application/json", report.content_type)

    def test_empty_upload_is_rejected(self):
        response = self.client.post("/api/analyze", data={}, content_type="multipart/form-data")
        self.assertEqual(response.status_code, 400)


if __name__ == "__main__":
    unittest.main()
