# OoT3D Save Editor — Android Studio project

A native Android wrapper around the same OoT3D save editor tested in the browser
version, adapted for real on-device file access:

- **Open Save File** launches Android's document picker once. After that the app
  remembers the exact file (via a persisted URI permission) and reopens it
  automatically the next time you launch the app.
- **Write Changes** writes the edited bytes straight back into that same file,
  in place. No downloading, renaming, or copying files around.
- The checksum, dungeon-completion panel, inventory editor, story-progress
  flags, and everything else are the exact same tested logic from the browser
  version (`../oot3d-save-editor.html`) — only the file I/O was changed. This
  copy is kept in sync with that one; if you update one, update the other.

## What I could not do in the sandbox that built this

I don't have the Android SDK, Gradle, or network access to Google's build
servers in the environment I'm working in, so **I could not compile this into
an actual `.apk` file or test it on a real device.** What I *did* verify:

- All Kotlin compiles conceptually against standard AndroidX/SAF APIs (I can't
  run `javac`/`kotlinc` against `android.jar` here, since I don't have it).
- The web UI's logic (`editor.html`) was tested in a real headless browser with
  a JavaScript stub standing in for the native bridge, using your actual save
  file. The full open → edit → checksum → save round trip produced correct,
  checksum-valid output, and only the intended bytes changed.

The one part that's genuinely untested is the Kotlin side talking to Android's
real Storage Access Framework and a real WebView on real hardware. That code
follows standard, well-documented Android APIs, but "should work" isn't the
same as "tested," and I want to be upfront about that distinction.

## If Android blocks browsing to Azahar's folder

Android 11+ restricts general access to other apps' `Android/data` folders in
some cases, and this varies by manufacturer/skin. If the picker won't let you
navigate into Azahar's save folder directly, use a file manager app to copy
the save file to a normal folder (like Downloads) first, edit it there with
this app, then copy the edited file back over the original.

## Project layout

- `app/src/main/java/com/oot3d/saveeditor/MainActivity.kt` — WebView host,
  file picker, and the JS bridge (`window.AndroidBridge`) exposed to the page.
- `app/src/main/assets/editor.html` — the save editor itself.
- `app/src/main/res/` — layout, theme, and generated launcher icon.
