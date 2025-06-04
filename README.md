# Android Components

Android Components is an open-source library of prebuilt, plug-and-play modules for Android development. The goal is to provide commonly used features—like camera integration—with ready-to-use UIs and APIs that can be easily imported, used as-is, or extended in your own projects.

Current Available Module:
EasyCamera — CameraX made simple

## EasyCamera (CameraX Library for Android)

EasyCamera provides drop-in CameraX support for Android with a modern UI and all the essentials—take photos, record video, flash, lens flip, pause/resume recording, and timer overlay. All boilerplate is handled for you!

Features:

* One-tap capture (photo)
* Long-press capture (video)
* Pause/Resume recording
* Timer overlay (with pause blink)
* Flash toggle (auto-hide for front camera)
* Lens switch (front/back)
* Permission flow auto-handled

## Quick Start

1. Clone & Import

* Clone this repo.
* Import the easycamera module into your Android Studio project.

2. Add Dependencies

Project-level (settings.gradle):

```
alias(libs.plugins.android.library) apply false
```

Module-level (build.gradle):

```
plugins {
    alias(libs.plugins.kotlin.android)
    // ... other plugins
}

dependencies {
    implementation(libs.core.ktx)
    // ... other dependencies as needed
}
```

Version Catalog (libs.versions.toml):

```
[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "agp" }

[libraries]
camera-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "cameraCore" }
camera-core = { module = "androidx.camera:camera-core", version.ref = "cameraCore" }
camera-extensions = { module = "androidx.camera:camera-extensions", version.ref = "cameraCore" }
camera-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "cameraCore" }
camera-video = { module = "androidx.camera:camera-video", version.ref = "cameraCore" }
camera-view = { module = "androidx.camera:camera-view", version.ref = "cameraCore" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }
core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }

[versions]
kotlin = "2.1.21"
coreKtx = "1.16.0"
cameraCore = "1.4.2"
guava = "33.3.0-android"
```

3\. Permissions

Add these to your AndroidManifest.xml:

```
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

4\. Usage

To launch the sample camera UI:

```
Intent intent = new Intent(context, CameraUI.class);
startActivity(intent);
```

Or integrate CameraKit in your own Activity/Fragment.

## Sample UI


<img src="screenshots/camera_1.png" width="300"/>
<img src="screenshots/camera_2.png" width="300"/>
<img src="screenshots/camera_3.png" width="300"/>

## Project Structure

* /easycamera — CameraX-based camera module (drop-in or customizable)
* (More modules coming soon!)

## Contributing

* Fork the repo
* Make your changes
* Open a pull request

MIT License. Free to use, share, and modify!

Suggestions or Issues? Open an issue or submit a PR!
