package com.datadetective.ai;

import android.Manifest;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
    private final JSONArray chatHistory = new JSONArray();

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

    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (stroke != 0) d.setStroke(1, stroke);
        return d;
    }

    private TextView label(String text, int size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private void buildScreen() {
        getWindow().setStatusBarColor(0xFF07111F);
        getWindow().setNavigationBarColor(0xFF07111F);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 18, 20, 16);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF101E35, 0xFF07111F, 0xFF090F1D}));
        
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView mark = label("✦", 25, 0xFFFFFFFF);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(shape(0xFF6558E8, 0, 18));
        top.addView(mark, new LinearLayout.LayoutParams(48, 48));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(12, 0, 0, 0);
        TextView title = label("DATA DETECTIVE", 19, 0xFFFFFFFF);
        title.setTypeface(null, 1);
        brand.addView(title);
        TextView sub = label("YOUR SMART VOICE ASSISTANT", 9, 0xFF8FA8C8);
        sub.setLetterSpacing(0.12f);
        brand.addView(sub);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView live = label("● ONLINE", 10, 0xFF64E6C1);
        live.setPadding(10, 7, 10, 7);
        live.setBackground(shape(0x2238D6A7, 0x5546D9AD, 22));
        top.addView(live);
        root.addView(top);

        TextView heroTag = label("  AI • VOICE • DISCOVERY  ", 10, 0xFFB9B2FF);
        heroTag.setPadding(0, 22, 0, 7);
        root.addView(heroTag);
        TextView hero = label("آپ کا اپنا AI اسسٹنٹ", 25, 0xFFFFFFFF);
        hero.setTypeface(null, 1);
        root.addView(hero);
        TextView intro = label("پوچھیں، بولیں، یا فون سے کوئی کام کروائیں۔", 13, 0xFFA9BBD0);
        intro.setPadding(0, 5, 0, 16);
        root.addView(intro);

        LinearLayout keyCard = new LinearLayout(this);
        keyCard.setOrientation(LinearLayout.VERTICAL);
        keyCard.setPadding(15, 12, 15, 13);
        keyCard.setBackground(shape(0xFF14243B, 0xFF263B5B, 18));
        TextView keyTitle = label("🔐  AI CONNECTION", 11, 0xFF9DB7D8);
        keyTitle.setTypeface(null, 1);
        keyCard.addView(keyTitle);
        TextView keyHint = label("اپنی Pollinations Secret Key درج کریں", 12, 0xFFDBE6F5);
        keyHint.setPadding(0, 5, 0, 8);
        keyCard.addView(keyHint);
        keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setTextColor(0xFFFFFFFF);
        keyInput.setHintTextColor(0xFF7186A3);
        keyInput.setHint("sk_••••••••••");
        keyInput.setTextSize(13);
        keyInput.setPadding(13, 3, 13, 3);
        keyInput.setInputType(129);
        keyInput.setBackground(shape(0xFF0B1729, 0xFF344B6B, 12));
        keyCard.addView(keyInput, new LinearLayout.LayoutParams(-1, 48));
        root.addView(keyCard);

        LinearLayout chatHeader = new LinearLayout(this);
        chatHeader.setGravity(Gravity.CENTER_VERTICAL);
        chatHeader.setPadding(2, 16, 2, 9);
        TextView chatTitle = label("آپ کی گفتگو", 15, 0xFFFFFFFF);
        chatTitle.setTypeface(null, 1);
        chatHeader.addView(chatTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView chatHint = label("محفوظ چیٹ سیشن", 10, 0xFF829BB9);
        chatHeader.addView(chatHint);
        root.addView(chatHeader);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(shape(0xFF0D1A2C, 0xFF243752, 18));
        conversation = new TextView(this);
        conversation.setTextColor(0xFFE7EFFB);
        conversation.setTextSize(14);
        conversation.setLineSpacing(5, 1.08f);
        conversation.setPadding(16, 16, 16, 16);
        conversation.setText("السلام علیکم! میں Data Detective ہوں۔\n\nآپ مجھ سے اردو یا English میں سوال کر سکتے ہیں۔ مائیک دبائیں یا نیچے اپنا سوال لکھیں۔\n\nفون کمانڈز: YouTube، WhatsApp، Camera، Settings، Alarm اور Timer۔");
        scroll.addView(conversation);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView prompt = label("اپنا سوال لکھیں", 11, 0xFF9DB2CC);
        prompt.setPadding(3, 13, 0, 6);
        root.addView(prompt);
        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setPadding(8, 6, 6, 6);
        composer.setBackground(shape(0xFF14243A, 0xFF314968, 18));
        questionInput = new EditText(this);
        questionInput.setTextColor(0xFFFFFFFF);
        questionInput.setHintTextColor(0xFF7D91AC);
        questionInput.setHint("یہاں لکھیں…");
        questionInput.setTextSize(14);
        questionInput.setPadding(9, 3, 6, 3);
        questionInput.setMinLines(1);
        questionInput.setMaxLines(3);
        questionInput.setBackgroundColor(Color.TRANSPARENT);
        composer.addView(questionInput, new LinearLayout.LayoutParams(0, -2, 1));

        micButton = new Button(this);
        micButton.setText("🎙");
        micButton.setTextSize(17);
        micButton.setTextColor(0xFFFFFFFF);
        micButton.setAllCaps(false);
        micButton.setPadding(0, 0, 0, 0);
        micButton.setMinWidth(0);
        micButton.setBackground(shape(0xFF675BEA, 0, 15));
        LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams(48, 48);
        micLp.setMargins(5, 0, 5, 0);
        composer.addView(micButton, micLp);
        sendButton = new Button(this);
        sendButton.setText("➤");
        sendButton.setTextSize(20);
        sendButton.setTextColor(0xFFFFFFFF);
        sendButton.setAllCaps(false);
        sendButton.setPadding(0, 0, 0, 0);
        sendButton.setMinWidth(0);
        sendButton.setBackground(shape(0xFF16BFA5, 0, 15));
        composer.addView(sendButton, new LinearLayout.LayoutParams(48, 48));
        root.addView(composer);

        TextView footer = label("DATA DETECTIVE AI   •   MADE TO HELP YOU", 9, 0xFF647D9C);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 10, 0, 0);
        root.addView(footer);

        sendButton.setOnClickListener(v -> {
            String q = questionInput.getText().toString().trim();
            if (!q.isEmpty()) { questionInput.setText(""); askAI(q); }
        });
        micButton.setOnClickListener(v -> listen());
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
        String q = raw.toLowerCase(Locale.ROOT).trim();
        addMessage("آپ", raw);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(q);
        java.util.ArrayList<Double> n = new java.util.ArrayList<>();
        while (m.find() && n.size() < 2) n.add(Double.parseDouble(m.group()));
        if (n.size() == 2 && containsAny(q, "kitne", "how much", "how many", "ہوتے", "کتنے", "جمع", "plus", "add", "minus", "منفی", "multiply", "ضرب", "times", "divide", "تقسیم", "aur", "اور", "+", "-", "*", "/")) {
            double x=n.get(0), y=n.get(1), z;
            if (containsAny(q,"minus","منفی","subtract","-")) z=x-y;
            else if (containsAny(q,"multiply","ضرب","times","*")) z=x*y;
            else if (containsAny(q,"divide","تقسیم","/")) { if(y==0){say("صفر سے تقسیم نہیں ہو سکتی۔");return;} z=x/y; }
            else z=x+y;
            say("جواب ہے " + (z==Math.rint(z) ? String.valueOf((long)z) : String.valueOf(z))); return;
        }
        if (containsAny(q,"instagram","انسٹاگرام")) openPackage("com.instagram.android","Instagram کھول رہا ہوں۔");
        else if (containsAny(q,"gallery","گیلری","photos","تصاویر")) {
            Intent g=new Intent(Intent.ACTION_GET_CONTENT); g.setType("image/*"); g.addCategory(Intent.CATEGORY_OPENABLE); launch(g,"گیلری کھول رہا ہوں۔");
        } else if (containsAny(q,"google","گوگل")) {
            if (!openPackageIfInstalled("com.google.android.googlequicksearchbox")) openUrl("https://www.google.com","Google کھول رہا ہوں۔");
        } else if (containsAny(q,"youtube","یوٹیوب")) {
            if (containsAny(q,"search","سرچ","تلاش")) openUrl("https://www.youtube.com/results?search_query="+Uri.encode(raw),"YouTube پر تلاش کر رہا ہوں۔");
            else if (!openPackageIfInstalled("com.google.android.youtube")) openUrl("https://www.youtube.com","YouTube کھول رہا ہوں۔");
        } else if (containsAny(q,"chrome","کروم","browser","براؤزر")) {
            if (!openPackageIfInstalled("com.android.chrome")) openUrl("https://www.google.com","براؤزر کھول رہا ہوں۔");
        } else if (containsAny(q,"whatsapp","واٹس ایپ")) openPackage("com.whatsapp","WhatsApp کھول رہا ہوں۔");
        else if (containsAny(q,"camera","کیمرہ")) launch(new Intent("android.media.action.IMAGE_CAPTURE"),"کیمرہ کھول رہا ہوں۔");
        else if (containsAny(q,"سیٹنگ","settings")) launch(new Intent(android.provider.Settings.ACTION_SETTINGS),"سیٹنگز کھول رہا ہوں۔");
        else if (containsAny(q,"about phone","فون کے بارے","موبائل کی معلومات")) launch(new Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS),"فون کی معلومات کھول رہا ہوں۔");
        else if (containsAny(q,"alarm","الارم")) { Intent al=new Intent(AlarmClock.ACTION_SET_ALARM); al.putExtra(AlarmClock.EXTRA_MESSAGE,"Data Detective"); launch(al,"الارم کھول رہا ہوں۔"); }
        else if (containsAny(q,"timer","ٹائمر")) launch(new Intent(AlarmClock.ACTION_SET_TIMER),"ٹائمر کھول رہا ہوں۔");
        else if (containsAny(q,"وقت","time")) { java.text.SimpleDateFormat df=new java.text.SimpleDateFormat("hh:mm a",new Locale("ur","PK")); say("اب وقت ہے "+df.format(new java.util.Date())); }
        else if (containsAny(q,"hello","سلام","السلام","ہیلو")) say("وعلیکم السلام! میں آپ کی بات سن رہا ہوں۔");
        else askAI(raw,true);
    }
    private boolean openPackageIfInstalled(String pkg) { Intent i=getPackageManager().getLaunchIntentForPackage(pkg); if(i==null)return false; launch(i,"ایپ کھول رہا ہوں۔"); return true; }

    private void askAI(String question) { askAI(question, false); }

    private boolean answerMathFromText(String question) {
        String q=question.toLowerCase(Locale.ROOT);
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(q);
        java.util.ArrayList<Double> n=new java.util.ArrayList<>();
        while(m.find() && n.size()<2) n.add(Double.parseDouble(m.group()));
        if(n.size()!=2 || !containsAny(q,"kitne","how much","how many","ہوتے","کتنے","جمع","plus","add","minus","منفی","multiply","ضرب","times","divide","تقسیم","aur","اور","+","-","*","/")) return false;
        double x=n.get(0),y=n.get(1),z;
        if(containsAny(q,"minus","منفی","subtract","-")) z=x-y;
        else if(containsAny(q,"multiply","ضرب","times","*")) z=x*y;
        else if(containsAny(q,"divide","تقسیم","/")) { if(y==0){say("صفر سے تقسیم نہیں ہو سکتی۔");return true;} z=x/y; }
        else z=x+y;
        say("جواب ہے "+(z==Math.rint(z)?String.valueOf((long)z):String.valueOf(z))); return true;
    }

    private void askAI(String question, boolean alreadyShown) {
        String key = keyInput.getText().toString().trim();
        if (key.isEmpty()) {
            if (!alreadyShown) addMessage("آپ", question);
            if (answerMathFromText(question)) return;
            say("عام سوالات کے جواب کے لیے Pollinations کی اپنی Secret API Key درج کریں۔ حساب اور فون کمانڈز بغیر API Key بھی کام کرتے ہیں۔");
            return;
        }
        if (!alreadyShown) addMessage("آپ", question);
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
                if (chatHistory.length() == 0) {
                    JSONObject system = new JSONObject();
                    system.put("role", "system");
                    system.put("content", "You are Data Detective AI, a helpful assistant. Reply in the same language as the user, especially Urdu or Roman Urdu. Be accurate, clear, and concise. You cannot claim to control the phone; phone actions are handled by the Android app.");
                    chatHistory.put(system);
                }
                JSONObject user = new JSONObject();
                user.put("role", "user");
                user.put("content", question);
                chatHistory.put(user);
                body.put("messages", chatHistory);
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
                    JSONObject assistant = new JSONObject();
                    assistant.put("role", "assistant");
                    assistant.put("content", answer);
                    chatHistory.put(assistant);
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
