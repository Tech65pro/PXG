# PXGV Recorder – Cross‑Platform (Flutter) Starter

> One codebase targeting: **Android (APK)**, **Windows (.exe)**, **Linux (.AppImage)**, **macOS (.app)**, **Web**. 
>
> Notes:
> - **Audio + Camera recording**: Android, iOS, Desktop (camera preview), Web (limited by browser permissions)
> - **Screen recording**: **Android** via MediaProjection plugin; **Web** via `getDisplayMedia` page (included); **Desktop** full video recording is not yet mature in Flutter; we include **screenshot** capture for desktop and recommend a Tauri/Electron path for true desktop screen recording later. **iOS** screen record requires ReplayKit (native plugin – TODO).
>
> This starter gives you: app tabs (Audio / Camera / Screen / Settings), theme toggle, pause/resume/stop controls, file saving, and CI to build **APK** and **Windows .exe** online.

---

## Project structure
```
pxgv_recorder/
  lib/
    main.dart
    pages/
      audio_page.dart
      camera_page.dart
      screen_page.dart
      settings_page.dart
    services/
      audio_service.dart
      camera_service.dart
      screen_service.dart
      file_service.dart
  web/
    screen_web.html
    index.html
  pubspec.yaml
  android/app/src/main/AndroidManifest.xml
  windows/runner/Runner.rc  (auto)
  macos/Runner/Configs/*    (auto)
  linux/*                   (auto)
  .github/workflows/
    android.yml
    windows.yml
  codemagic.yaml
```

---

## pubspec.yaml
```yaml
name: pxgv_recorder
publish_to: 'none'
version: 1.0.0+1
environment:
  sdk: ">=3.3.0 <4.0.0"

dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.6
  provider: ^6.0.5
  permission_handler: ^11.3.1
  path_provider: ^2.1.3
  record: ^5.0.4              # audio recorder (Android/iOS/desktop/web via WASM mic if browser allows)
  camera: ^0.11.0             # video recording (Android/iOS; desktop preview, record varies)
  flutter_screen_recording: ^1.0.15   # ANDROID ONLY
  screen_capturer: ^0.2.4     # Desktop screenshots (Windows/macOS/Linux)

  # Web helpers (screen capture via JS interop)
  flutter_web_plugins:
    sdk: flutter

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.1

flutter:
  uses-material-design: true
  assets:
    - web/screen_web.html
```

---

## lib/main.dart
```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'pages/audio_page.dart';
import 'pages/camera_page.dart';
import 'pages/screen_page.dart';
import 'pages/settings_page.dart';

void main() {
  runApp(const PXGVApp());
}

class ThemeModel extends ChangeNotifier {
  ThemeMode mode = ThemeMode.system;
  void toggle(bool isDark) {
    mode = isDark ? ThemeMode.dark : ThemeMode.light;
    notifyListeners();
  }
}

class PXGVApp extends StatelessWidget {
  const PXGVApp({super.key});
  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => ThemeModel(),
      child: Consumer<ThemeModel>(
        builder: (context, t, _) => MaterialApp(
          title: 'PXGV Recorder',
          debugShowCheckedModeBanner: false,
          themeMode: t.mode,
          theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.blue),
          darkTheme: ThemeData.dark(useMaterial3: true),
          home: const Home(),
        ),
      ),
    );
  }
}

class Home extends StatefulWidget {
  const Home({super.key});
  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  int index = 0;
  final pages = const [AudioPage(), CameraPage(), ScreenPage(), SettingsPage()];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('PXGV Recorder')),
      body: pages[index],
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        destinations: const [
          NavigationDestination(icon: Icon(Icons.mic_none), label: 'Audio'),
          NavigationDestination(icon: Icon(Icons.videocam_outlined), label: 'Camera'),
          NavigationDestination(icon: Icon(Icons.monitor), label: 'Screen'),
          NavigationDestination(icon: Icon(Icons.settings), label: 'Settings'),
        ],
        onDestinationSelected: (i) => setState(() => index = i),
      ),
    );
  }
}
```

---

## lib/pages/audio_page.dart
```dart
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:record/record.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:io';

class AudioPage extends StatefulWidget { const AudioPage({super.key}); @override State<AudioPage> createState() => _AudioPageState(); }

class _AudioPageState extends State<AudioPage> {
  final _rec = AudioRecorder();
  bool _isRecording = false;
  bool _isPaused = false;
  String? _path;

  Future<String> _filePath() async {
    final dir = await getApplicationDocumentsDirectory();
    final f = File('${dir.path}/pxgv_audio_${DateTime.now().millisecondsSinceEpoch}.m4a');
    return f.path;
  }

  Future<void> _start() async {
    if (await Permission.microphone.request().isGranted) {
      final path = await _filePath();
      await _rec.start(
        const RecordConfig(encoder: AudioEncoder.aacLc, bitRate: 128000, sampleRate: 44100),
        path: path,
      );
      setState(() { _isRecording = true; _isPaused = false; _path = path; });
    }
  }

  Future<void> _pause() async { await _rec.pause(); setState(() => _isPaused = true); }
  Future<void> _resume() async { await _rec.resume(); setState(() => _isPaused = false); }
  Future<void> _stop() async { final p = await _rec.stop(); setState(() { _isRecording = false; _isPaused = false; _path = p; }); }

  @override
  void dispose() { _rec.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Audio Recorder', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          Wrap(spacing: 8, children: [
            FilledButton.icon(onPressed: _isRecording ? null : _start, icon: const Icon(Icons.fiber_manual_record), label: const Text('Start')),
            FilledButton.icon(onPressed: (_isRecording && !_isPaused) ? _pause : null, icon: const Icon(Icons.pause), label: const Text('Pause')),
            FilledButton.icon(onPressed: (_isRecording && _isPaused) ? _resume : null, icon: const Icon(Icons.play_arrow), label: const Text('Resume')),
            FilledButton.icon(onPressed: _isRecording ? _stop : null, icon: const Icon(Icons.stop), label: const Text('Stop')),
          ]),
          const SizedBox(height: 16),
          Text(_path == null ? 'No file yet.' : 'Saved: $_path'),
        ],
      ),
    );
  }
}
```

---

## lib/pages/camera_page.dart
```dart
import 'package:flutter/material.dart';
import 'package:camera/camera.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:path_provider/path_provider.dart';

class CameraPage extends StatefulWidget { const CameraPage({super.key}); @override State<CameraPage> createState() => _CameraPageState(); }

class _CameraPageState extends State<CameraPage> {
  CameraController? _ctrl;
  List<CameraDescription> _cams = [];
  bool _isRecording = false;

  @override
  void initState() { super.initState(); _init(); }

  Future<void> _init() async {
    if (await Permission.camera.request().isGranted) {
      _cams = await availableCameras();
      if (_cams.isNotEmpty) {
        _ctrl = CameraController(_cams.first, ResolutionPreset.medium, enableAudio: true);
        await _ctrl!.initialize();
        setState(() {});
      }
    }
  }

  Future<void> _switchCam() async {
    if (_cams.length < 2) return; 
    final current = _ctrl!.description;
    final next = _cams.firstWhere((c) => c.lensDirection != current.lensDirection, orElse: () => _cams.first);
    await _ctrl?.dispose();
    _ctrl = CameraController(next, ResolutionPreset.medium, enableAudio: true);
    await _ctrl!.initialize();
    setState(() {});
  }

  Future<void> _start() async {
    final dir = await getApplicationDocumentsDirectory();
    final path = '${dir.path}/pxgv_video_${DateTime.now().millisecondsSinceEpoch}.mp4';
    await _ctrl?.startVideoRecording();
    _isRecording = true; setState(() {});
  }

  Future<void> _stop() async {
    final file = await _ctrl?.stopVideoRecording();
    _isRecording = false; setState(() {});
    if (file != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Saved: ${file.path}')));
    }
  }

  @override
  void dispose() { _ctrl?.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Expanded(child: _ctrl == null || !_ctrl!.value.isInitialized
          ? const Center(child: Text('Camera not ready'))
          : CameraPreview(_ctrl!)),
      const SizedBox(height: 8),
      Wrap(spacing: 8, children: [
        FilledButton.icon(onPressed: _isRecording ? null : _start, icon: const Icon(Icons.fiber_manual_record), label: const Text('Start')),
        FilledButton.icon(onPressed: _isRecording ? _stop : null, icon: const Icon(Icons.stop), label: const Text('Stop')),
        OutlinedButton.icon(onPressed: _switchCam, icon: const Icon(Icons.cameraswitch), label: const Text('Switch')),
      ]),
      const SizedBox(height: 12),
    ]);
  }
}
```

---

## lib/pages/screen_page.dart
```dart
import 'dart:io' show Platform;
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_screen_recording/flutter_screen_recording.dart';
import 'package:screen_capturer/screen_capturer.dart';

class ScreenPage extends StatefulWidget { const ScreenPage({super.key}); @override State<ScreenPage> createState() => _ScreenPageState(); }

class _ScreenPageState extends State<ScreenPage> {
  bool _rec = false;
  String? _savedPath;

  Future<void> _startAndroid() async {
    try {
      await FlutterScreenRecording.startRecordScreen("pxgv_screen");
      setState(() => _rec = true);
    } catch (e) {
      _show('Failed: $e');
    }
  }

  Future<void> _stopAndroid() async {
    try {
      final path = await FlutterScreenRecording.stopRecordScreen;
      setState(() { _rec = false; _savedPath = path; });
      _show('Saved: $path');
    } catch (e) { _show('Stop failed: $e'); }
  }

  Future<void> _screenshotDesktop() async {
    try {
      final image = await ScreenCapturer.instance.capture();
      _show(image == null ? 'Cancelled' : 'Screenshot captured');
    } catch (e) { _show('Screenshot failed: $e'); }
  }

  void _openWebHelp() {
    showDialog(context: context, builder: (_) => AlertDialog(
      title: const Text('Web Screen Record'),
      content: const Text('On Web, use the special page that calls getDisplayMedia(). Open /web/screen_web.html and test in browser.'),
    ));
  }

  void _show(String msg) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));

  @override
  Widget build(BuildContext context) {
    final isAndroid = !kIsWeb && Platform.isAndroid;
    final isDesktop = !kIsWeb && (Platform.isWindows || Platform.isLinux || Platform.isMacOS);

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Screen Tools', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 12),
        if (isAndroid) Wrap(spacing: 8, children: [
          FilledButton.icon(onPressed: _rec ? null : _startAndroid, icon: const Icon(Icons.fiber_manual_record), label: const Text('Start (Android)')),
          FilledButton.icon(onPressed: _rec ? _stopAndroid : null, icon: const Icon(Icons.stop), label: const Text('Stop')),
        ]),
        if (isDesktop) Wrap(spacing: 8, children: [
          FilledButton.icon(onPressed: _screenshotDesktop, icon: const Icon(Icons.camera), label: const Text('Screenshot (Desktop)')),
        ]),
        if (kIsWeb) Wrap(spacing: 8, children: [
          FilledButton.icon(onPressed: _openWebHelp, icon: const Icon(Icons.language), label: const Text('How to record on Web')),
        ]),
        const SizedBox(height: 16),
        Text(_savedPath == null ? 'No file yet.' : 'Saved: $_savedPath'),
      ]),
    );
  }
}
```

---

## lib/pages/settings_page.dart
```dart
import 'package:flutter/material.dart';
import '../main.dart';
import 'package:provider/provider.dart';

class SettingsPage extends StatelessWidget { const SettingsPage({super.key});
  @override
  Widget build(BuildContext context) {
    final theme = context.watch<ThemeModel>();
    final isDark = theme.mode == ThemeMode.dark;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        SwitchListTile(
          title: const Text('Dark Mode'),
          value: isDark,
          onChanged: (v) => theme.toggle(v),
        ),
        const SizedBox(height: 8),
        const ListTile(
          title: Text('PXGV Recorder'),
          subtitle: Text('Simple, cross-platform recorder (audio/camera/screen)')
        )
      ],
    );
  }
}
```

---

## web/screen_web.html (Web-only screen record using JS)
```html
<!doctype html>
<html>
  <head>
    <meta charset="utf-8"/>
    <title>PXGV Web Screen Recorder</title>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
  </head>
  <body>
    <h3>PXGV Web Screen Recorder</h3>
    <button id="start">Start</button>
    <button id="stop" disabled>Stop</button>
    <video id="preview" controls style="max-width: 480px; display:block; margin-top:12px;"></video>
    <script>
      let mediaRecorder; let chunks=[];
      const startBtn = document.getElementById('start');
      const stopBtn = document.getElementById('stop');
      const vid = document.getElementById('preview');
      startBtn.onclick = async () => {
        try {
          const stream = await navigator.mediaDevices.getDisplayMedia({video:true, audio:true});
          mediaRecorder = new MediaRecorder(stream);
          mediaRecorder.ondataavailable = e => chunks.push(e.data);
          mediaRecorder.onstop = () => {
            const blob = new Blob(chunks, {type: 'video/webm'});
            chunks = [];
            const url = URL.createObjectURL(blob);
            vid.src = url;
            const a = document.createElement('a');
            a.href = url; a.download = 'pxgv_screen.webm'; a.click();
          };
          mediaRecorder.start();
          startBtn.disabled = true; stopBtn.disabled = false;
        } catch (e) { alert('Failed: '+e); }
      };
      stopBtn.onclick = () => { mediaRecorder?.stop(); startBtn.disabled = false; stopBtn.disabled = true; };
    </script>
  </body>
</html>
```

---

## android/app/src/main/AndroidManifest.xml (permissions)
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.pxgv.recorder">
  <uses-permission android:name="android.permission.RECORD_AUDIO" />
  <uses-permission android:name="android.permission.CAMERA" />
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28"/>
  <application android:label="PXGV Recorder" android:requestLegacyExternalStorage="true">
    <activity android:name="io.flutter.embedding.android.FlutterActivity" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
```

---

## .github/workflows/android.yml (build APK online with GitHub Actions)
```yaml
name: Android APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.22.0'
      - run: flutter pub get
      - run: flutter build apk --release
      - uses: actions/upload-artifact@v4
        with:
          name: pxgv-apk
          path: build/app/outputs/flutter-apk/app-release.apk
```

---

## .github/workflows/windows.yml (build Windows .exe online)
```yaml
name: Windows EXE
on: [push]
jobs:
  build:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v4
      - uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.22.0'
      - run: flutter config --enable-windows-desktop
      - run: flutter pub get
      - run: flutter build windows --release
      - uses: actions/upload-artifact@v4
        with:
          name: pxgv-windows
          path: build\windows\x64\runner\Release\
```

---

## codemagic.yaml (alternative: build APK/EXE on Codemagic)
```yaml
workflows:
  pxgv_release:
    name: PXGV Release
    max_build_duration: 60
    instance_type: mac_mini_m2
    environment:
      flutter: stable
    scripts:
      - flutter pub get
      - flutter build apk --release
      - flutter config --enable-windows-desktop
      - flutter build windows --release
    artifacts:
      - build/app/outputs/flutter-apk/app-release.apk
      - build/windows/x64/runner/Release/**
```

---

## How to use (quick)
1) Create a new Flutter project locally or online, then **replace** files with the above.
2) Commit to **GitHub**.
3) Use **GitHub Actions** (already provided) → after each push, download **APK** and **.exe** from the **Actions → Artifacts** tab.
4) Or connect the repo to **Codemagic** to get hosted builds and download links.

### Platform notes
- **Android:** Audio/Camera/Screen (screen via plugin). Tested on Android 10+.
- **Windows/macOS/Linux:** Audio/Camera preview; desktop **screenshot** capture. Full screen *video* record requires native code or Tauri/Electron solution.
- **Web:** Use `web/screen_web.html` page to record via browser permissions.
- **iOS:** Audio/Camera OK; for screen record integrate ReplayKit later.

```text
Brand: PXGV Recorder
Tabs: Audio | Camera | Screen | Settings
Controls: Start / Pause / Resume / Stop | Camera Switch | Theme toggle
