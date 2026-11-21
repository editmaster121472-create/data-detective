package com.datadetective.ai;

import android.Manifest;
import android.app.Activity;
import android.provider.AlarmClock;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 41;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView conversation;
    private Button micButton, sendButton;
    private EditText questionInput, keyInput;
    private boolean ttsReady = false;
    private final ExecutorService network = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ur", "PK"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    tts.setLanguage(Locale.US);
                ttsReady = true;
            }
        });
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle p) { setStatus("سن رہا ہوں…"); }
                public void onBeginningOfSpeech() { setStatus("آپ بول رہے ہیں…"); }
                public void onRmsChanged(float r) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() { setStatus("آپ کی بات سمجھ رہا ہوں…"); }
                public void onError(int e) { setStatus("آواز واضح نہیں ہوئی۔ دوبارہ مائیک دبائیں۔"); }
                public void onResults(Bundle b) {
                    ArrayList<String> matches = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) handleCommand(matches.get(0));
                }
                public void onPartialResults(Bundle b) {}
                public void onEvent(int t, Bundle p) {}
            });
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 28, 22, 20);
        root.setBackgroundColor(0xFF081321);
        TextView title = new TextView(this);
        title.setText("DATA DETECTIVE AI");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("اردو اور English وائس + AI چیٹ");
        subtitle.setTextColor(0xFF9EDCF2);
        subtitle.setTextSize(15);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        TextView keyLabel = new TextView(this);
        keyLabel.setText("Pollinations API Key (اپنی key درج کریں)");
        keyLabel.setTextColor(0xFFB9C8D8);
        keyLabel.setPadding(0, 14, 0, 4);
        root.addView(keyLabel);
        keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setTextColor(0xFFFFFFFF);
        keyInput.setHintTextColor(0xFF8A9BAD);
        keyInput.setHint("sk_…");
        keyInput.setInputType(129);
        root.addView(keyInput, new LinearLayout.LayoutParams(-1, -2));

        conversation = new TextView(this);
        conversation.setTextColor(0xFFFFFFFF);
        conversation.setTextSize(16);
        conversation.setText("السلام علیکم! سوال لکھیں یا مائیک دبائیں۔\nفون کے کام مثلاً یوٹیوب، واٹس ایپ، سیٹنگز اور الارم بغیر AI key کے بھی چلتے ہیں۔\n");
        conversation.setPadding(0, 18, 0, 18);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(conversation);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        questionInput = new EditText(this);
        questionInput.setTextColor(0xFFFFFFFF);
        questionInput.setHintTextColor(0xFF8A9BAD);
        questionInput.setHint("اپنا سوال یہاں لکھیں…");
        questionInput.setMinLines(1);
        questionInput.setMaxLines(3);
        root.addView(questionInput, new LinearLayout.LayoutParams(-1, -2));

        sendButton = new Button(this);
        sendButton.setText("AI سے جواب پوچھیں");
        sendButton.setOnClickListener(v -> {
            String q = questionInput.getText().toString().trim();
            if (!q.isEmpty()) { questionInput.setText(""); askAI(q); }
        });
        root.addView(sendButton, new LinearLayout.LayoutParams(-1, -2));

        micButton = new Button(this);
        micButton.setText("🎙 بولنے کے لیے دبائیں");
        micButton.setTextSize(17);
        micButton.setOnClickListener(v -> listen());
        root.addView(micButton, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private void listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (recognizer == null) { say("Speech recognition دستیاب نہیں۔ Google Speech Services فعال کریں۔"); return; }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "اب بولیں");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try { recognizer.startListening(i); }
        catch (Exception ex) { setStatus("مائیک شروع نہیں ہو سکا۔"); }
    }

    private void handleCommand(String raw) {
        String q = raw.toLowerCase(Locale.ROOT);
        addMessage("آپ", raw);
        if (containsAny(q, "یوٹیوب", "youtube")) {
            String term = q.replace("youtube", "").replace("یوٹیوب", "").replace("کھولو", "").replace("اوپن", "").trim();
            if (term.isEmpty()) term = "YouTube";
            openUrl("https://www.youtube.com/results?search_query=" + Uri.encode(term), "YouTube سرچ کھول رہا ہوں۔");
        } else if (containsAny(q, "سیٹنگ", "settings")) {
            launch(new Intent(android.provider.Settings.ACTION_SETTINGS), "سیٹنگز کھول رہا ہوں۔");
        } else if (containsAny(q, "about phone", "فون کے بارے", "موبائل کی معلومات")) {
            launch(new Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS), "فون کی معلومات کھول رہا ہوں۔");
        } else if (containsAny(q, "chrome", "کروم", "browser", "براؤزر")) {
            openUrl("https://www.google.com", "براؤزر کھول رہا ہوں۔");
        } else if (containsAny(q, "whatsapp", "واٹس ایپ")) {
            openPackage("com.whatsapp", "WhatsApp کھول رہا ہوں۔");
        } else if (containsAny(q, "camera", "کیمرہ")) {
            launch(new Intent("android.media.action.IMAGE_CAPTURE"), "کیمرہ کھول رہا ہوں۔");
        } else if (containsAny(q, "alarm", "الارم")) {
            Intent a = new Intent(AlarmClock.ACTION_SET_ALARM);
            a.putExtra(AlarmClock.EXTRA_MESSAGE, "Data Detective");
            launch(a, "الارم سیٹ کرنے کی اسکرین کھول رہا ہوں۔");
        } else if (containsAny(q, "timer", "ٹائمر")) {
            launch(new Intent(AlarmClock.ACTION_SET_TIMER), "ٹائمر کھول رہا ہوں۔");
        } else if (containsAny(q, "وقت", "time")) {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("hh:mm a", new Locale("ur", "PK"));
            say("اب وقت ہے " + f.format(new java.util.Date()));
        } else if (containsAny(q, "hello", "سلام", "السلام", "ہیلو")) {
            say("وعلیکم السلام! میں آپ کی بات سن رہا ہوں۔");
        } else {
            askAI(raw);
        }
    }

    private void askAI(String question) {
        String key = keyInput.getText().toString().trim();
        if (key.isEmpty()) {
            say("AI سوال جواب کے لیے Pollinations کی اپنی API key درج کریں۔ API key کے بغیر فون کے بنیادی وائس کمانڈ کام کرتے ہیں۔");
            return;
        }
        addMessage("آپ", question);
        setBusy(true);
        network.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://gen.pollinations.ai/v1/chat/completions");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + key);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                JSONObject body = new JSONObject();
                body.put("model", "openai/gpt-oss-20b");
                JSONArray messages = new JSONArray();
                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content", "You are Data Detective AI, a helpful assistant. Reply in the same language as the user, especially Urdu or Roman Urdu. Be accurate, clear, and concise. You cannot claim to control the phone; phone actions are handled by the Android app.");
                messages.put(system);
                JSONObject user = new JSONObject();
                user.put("role", "user");
                user.put("content", question);
                messages.put(user);
                body.put("messages", messages);
                body.put("temperature", 0.6);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) { os.write(payload); }
                int code = conn.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                }
                if (code >= 200 && code < 300) {
                    JSONObject json = new JSONObject(response.toString());
                    String answer = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                    runOnUiThread(() -> { setBusy(false); say(answer); });
                } else {
                    String error = "AI سروس نے جواب نہیں دیا۔ HTTP " + code + "۔ اپنی API key اور اس کے balance/limits چیک کریں۔";
                    runOnUiThread(() -> { setBusy(false); say(error); });
                }
            } catch (Exception e) {
                runOnUiThread(() -> { setBusy(false); say("انٹرنیٹ یا AI سروس سے رابطہ نہیں ہو سکا۔ دوبارہ کوشش کریں۔"); });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void setBusy(boolean busy) {
        sendButton.setEnabled(!busy);
        micButton.setEnabled(!busy);
        sendButton.setText(busy ? "جواب آ رہا ہے…" : "AI سے جواب پوچھیں");
    }
    private void addMessage(String who, String text) {
        conversation.append("\n" + who + ": " + text + "\n");
        if (conversation.getParent() instanceof ScrollView) ((ScrollView) conversation.getParent()).post(() -> ((ScrollView) conversation.getParent()).fullScroll(View.FOCUS_DOWN));
    }
    private boolean containsAny(String s, String... words) {
        for (String w : words) if (s.contains(w)) return true;
        return false;
    }
    private void openPackage(String pkg, String message) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { say("یہ ایپ انسٹال نہیں ہے۔"); return; }
        launch(i, message);
    }
    private void openUrl(String url, String message) { launch(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), message); }
    private void launch(Intent i, String message) {
        try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); say(message); }
        catch (ActivityNotFoundException e) { say("یہ کام کرنے والی ایپ فون میں نہیں ملی۔"); }
        catch (Exception e) { say("فون نے اس کام کی اجازت نہیں دی۔"); }
    }
    private void say(String text) {
        addMessage("AI", text);
        setStatus(text.length() > 34 ? text.substring(0, 34) + "…" : text);
        if (tts != null && ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dd_reply");
    }
    private void setStatus(String s) { if (micButton != null) micButton.setText(s); }
    @Override public void onDestroy() {
        if (recognizer != null) { recognizer.destroy(); recognizer = null; }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        network.shutdownNow();
        super.onDestroy();
    }
}
