# Data Detective AI — Android voice assistant

This is the native Android companion app. It is separate from the existing Flask website.

## First working scope
- Android speech recognition through the device speech service (Urdu-Pakistan requested first).
- Spoken replies through Android TextToSpeech (Urdu voice depends on installed TTS voice data).
- Real Android intents for YouTube search, Settings, About Phone, alarms, timer, WhatsApp, Chrome and camera.
- Unknown questions open Android web search; this first version does **not** pretend to be a full conversational AI model.

## Build
Open this folder as an Android Gradle project in Android Studio or a compatible Android IDE. JDK 17 and Android SDK 35 are required.

The app asks for microphone permission at runtime. Speech recognition may use Google speech services and may require internet. Android protects some settings and actions; the app cannot bypass those restrictions.
