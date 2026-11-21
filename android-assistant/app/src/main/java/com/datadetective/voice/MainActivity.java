package com.datadetective.voice;

import android.Manifest;
import android.app.Activity;
import android.provider.AlarmClock;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQUEST_SPEECH = 1001;
    private static final int REQUEST_MIC = 1002;
    private TextToSpeech tts;
    private TextView conversation;
    private EditText commandInput;
    private boolean ttsReady = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        tts = new TextToSpeech(this, this);
        buildScreen();
        say("السلام علیکم! ڈیٹا ڈیٹیکٹو تیار ہے۔ مائیکروفون دبائیں اور حکم دیں۔");
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 24);
        root.setBackgroundColor(0xFF101827);

        TextView title = new TextView(this);
        title.setText("DATA DETECTIVE AI");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(23);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 8, 0, 8);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Voice assistant · Urdu + English");
        subtitle.setTextColor(0xFF9FB3C8);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 18);
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        conversation = new TextView(this);
        conversation.setTextColor(0xFFE5E7EB);
        conversation.setTextSize(16);
        conversation.setPadding(14, 14, 14, 14);
        conversation.setText("Assistant: میں آپ کی آواز سننے کے لیے تیار ہوں۔\n");
        scroll.addView(conversation);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        root.addView(scroll, scrollParams);

        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setHint("یہاں حکم لکھیں یا مائیک دبائیں");
        commandInput.setTextColor(0xFFFFFFFF);
        commandInput.setHintTextColor(0xFF9CA3AF);
        root.addView(commandInput);

        Button mic = new Button(this);
        mic.setText("🎙 بول کر حکم دیں");
        mic.setOnClickListener(v -> listen());
        root.addView(mic);

        Button run = new Button(this);
        run.setText("حکم چلائیں");
        run.setOnClickListener(v -> handleCommand(commandInput.getText().toString()));
        root.addView(run);

        Button stop = new Button(this);
        stop.setText("آواز بند کریں");
        stop.setOnClickListener(v -> { if (tts != null) tts.stop(); });
        root.addView(stop);
        setContentView(root);
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("ur", "PK"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }
            tts.setSpeechRate(0.92f);
            ttsReady = true;
        }
    }

    private void say(String text) {
        addLine("Assistant: " + text);
        if (ttsReady && tts != null) {
            Locale locale = containsUrdu(text) ? new Locale("ur", "PK") : Locale.US;
            int result = tts.setLanguage(locale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dd_reply");
        }
    }

    private boolean containsUrdu(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) >= 0x0600 && value.charAt(i) <= 0x06FF) return true;
        }
        return false;
    }

    private void addLine(String line) {
        if (conversation != null) conversation.append("\n" + line + "\n");
    }

    private void listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return;
        }
        startSpeech();
    }

    private void startSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "اپنا حکم بولیں");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        try {
            startActivityForResult(intent, REQUEST_SPEECH);
        } catch (ActivityNotFoundException ex) {
            say("اس موبائل میں تقریر پہچاننے کی سروس دستیاب نہیں۔ Google Speech Services انسٹال یا فعال کریں۔");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String heard = results.get(0);
                commandInput.setText(heard);
                addLine("You: " + heard);
                handleCommand(heard);
            }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == REQUEST_MIC && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) startSpeech();
        else if (requestCode == REQUEST_MIC) say("مائیکروفون کی اجازت کے بغیر میں آپ کی آواز نہیں سن سکتا۔");
    }

    private void handleCommand(String raw) {
        String q = raw == null ? "" : raw.trim();
        String lower = q.toLowerCase(Locale.ROOT);
        if (q.isEmpty()) { say("براہ کرم کوئی حکم دیں۔"); return; }

        if (lower.contains("youtube") || q.contains("یوٹیوب")) {
            String search = q.replaceAll("(?i)youtube|یوٹیوب|کھولو|کھولیں|پر|سर्च|سرچ|search|چلاؤ|لگاؤ", " ").trim();
            if (search.isEmpty()) {
                launchApp("com.google.android.youtube", "YouTube");
            } else {
                try {
                    String url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(search, "UTF-8");
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    say("یوٹیوب پر " + search + " کی تلاش کھول دی ہے۔");
                } catch (Exception e) { say("یوٹیوب سرچ نہیں کھل سکی۔"); }
            }
            return;
        }
        if (lower.contains("settings") || q.contains("سیٹنگ")) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
            say("موبائل کی سیٹنگز کھول دی ہیں۔");
            return;
        }
        if (lower.contains("about phone") || q.contains("فون کے بارے") || q.contains("اباؤٹ فون")) {
            try { startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)); say("About Phone کھول دیا ہے۔"); }
            catch (Exception e) { say("About Phone کی اسکرین اس موبائل پر دستیاب نہیں۔"); }
            return;
        }
        if (lower.contains("alarm") || q.contains("الارم")) {
            Intent alarm = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            if (alarm.resolveActivity(getPackageManager()) != null) startActivity(alarm);
            else say("الارم ایپ نہیں ملی۔");
            return;
        }
        if (lower.contains("timer") || q.contains("ٹائمر")) {
            Intent timer = new Intent(AlarmClock.ACTION_SET_TIMER);
            timer.putExtra(AlarmClock.EXTRA_LENGTH, 60);
            timer.putExtra(AlarmClock.EXTRA_MESSAGE, "Data Detective timer");
            if (timer.resolveActivity(getPackageManager()) != null) startActivity(timer);
            else say("ٹائمر ایپ نہیں ملی۔");
            return;
        }
        if (lower.contains("whatsapp") || q.contains("واٹس ایپ")) { launchApp("com.whatsapp", "WhatsApp"); return; }
        if (lower.contains("chrome") || q.contains("کروم")) { launchApp("com.android.chrome", "Chrome"); return; }
        if (lower.contains("camera") || q.contains("کیمرہ")) {
            Intent camera = new Intent("android.media.action.IMAGE_CAPTURE");
            if (camera.resolveActivity(getPackageManager()) != null) startActivity(camera);
            else say("کیمرہ ایپ نہیں ملی.");
            return;
        }

        if (lower.contains("hello") || lower.contains("hi") || q.contains("السلام") || q.contains("ہیلو")) {
            say("وعلیکم السلام! بتائیں، میں آپ کے موبائل میں کون سا کام کروں؟");
            return;
        }
        if (lower.contains("your name") || q.contains("تمہارا نام")) {
            say("میرا نام Data Detective AI ہے۔");
            return;
        }

        try {
            Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
            search.putExtra(android.app.SearchManager.QUERY, q);
            if (search.resolveActivity(getPackageManager()) != null) {
                startActivity(search);
                say("اس سوال کا تازہ جواب تلاش کرنے کے لیے ویب سرچ کھول دی ہے۔");
            } else {
                say("یہ حکم ابھی میرے بنیادی کمانڈز میں شامل نہیں۔");
            }
        } catch (Exception e) {
            say("یہ کام ابھی مکمل نہیں ہو سکا۔");
        }
    }

    private void launchApp(String packageName, String label) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) {
            startActivity(launch);
            say(label + " کھول رہا ہوں۔");
        } else {
            say(label + " اس موبائل میں انسٹال نہیں ہے۔");
        }
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
