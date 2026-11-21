package com.datadetective.ai;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmClock;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 41;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView conversation;
    private Button micButton;
    private boolean ttsReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildScreen();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ur", "PK"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
                ttsReady = true;
            }
        });
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("آپ کے فون میں speech recognition service دستیاب نہیں۔ Google Speech Services انسٹال یا فعال کریں۔");
        } else {
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
        root.setPadding(28, 36, 28, 28);
        root.setBackgroundColor(0xFF081321);
        TextView title = new TextView(this);
        title.setText("DATA DETECTIVE AI");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("اردو اور English وائس اسسٹنٹ");
        subtitle.setTextColor(0xFFB9C8D8);
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);
        conversation = new TextView(this);
        conversation.setTextColor(0xFFFFFFFF);
        conversation.setTextSize(17);
        conversation.setText("مائیک دبائیں اور کہیں: یوٹیوب کھولو، سیٹنگز کھولو، یا وقت بتاؤ۔\n");
        conversation.setPadding(0, 30, 0, 24);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(conversation);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        micButton = new Button(this);
        micButton.setText("🎙 بولنے کے لیے دبائیں");
        micButton.setTextSize(18);
        micButton.setOnClickListener(v -> listen());
        root.addView(micButton, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private void listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (recognizer == null) {
            say("Speech recognition دستیاب نہیں۔");
            return;
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "اب بولیں");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        try { recognizer.startListening(i); }
        catch (Exception ex) { setStatus("مائیک شروع نہیں ہو سکا۔"); }
    }

    private void handleCommand(String raw) {
        String q = raw.toLowerCase(Locale.ROOT);
        conversation.append("\nآپ: " + raw + "\n");
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
            say("وعلیکم السلام! میں آپ کی بات سن رہا ہوں۔ آپ کون سا کام کروانا چاہتے ہیں؟");
        } else {
            openUrl("https://www.google.com/search?q=" + Uri.encode(raw), "اس سوال کی ویب سرچ کھول رہا ہوں۔");
        }
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
    private void openUrl(String url, String message) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        launch(i, message);
    }
    private void launch(Intent i, String message) {
        try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); say(message); }
        catch (ActivityNotFoundException e) { say("یہ کام کرنے والی ایپ فون میں نہیں ملی۔"); }
        catch (Exception e) { say("فون نے اس کام کی اجازت نہیں دی۔"); }
    }
    private void say(String text) {
        conversation.append("AI: " + text + "\n");
        setStatus(text);
        if (tts != null && ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dd_reply");
    }
    private void setStatus(String s) { if (micButton != null) micButton.setText(s); }
    @Override public void onDestroy() {
        if (recognizer != null) { recognizer.destroy(); recognizer = null; }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}