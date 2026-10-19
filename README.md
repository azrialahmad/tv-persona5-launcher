# TV Persona 5 launcher

A Persona 5 inspired home screen for Android TV, in plain Java with custom Canvas drawing. It is a style added to the
[tv-vibehack](https://github.com/Gathin23/tv-vibehack) launcher (MIT, by Gathin), which keeps its original Apple TV look as an option.

Everything is drawn in code: red/black/white angular shapes, a jagged red selection shard, ransom-note lettering,
halftone dots, a diagonal wipe on return to Home, and a calling-card screensaver. No game art, logos or rips are included.
Built and tuned on a 1080p TCL 43Q6 (Android TV 11, 2 GB RAM): about 8 ms median frame time, D-pad only.

| Screen | Persona 5 equivalent |
|---|---|
| Dock (4 favourite apps) | Pause menu: big slanted words, red shard on the focused one, ransom-note title above |
| Top-left clock | Calendar corner: date, weekday, time of day |
| Inputs / Wi-Fi / Settings | Tilted tags that flip to red |
| App grid | Framed, slightly tilted banners |
| Clock screensaver | Calling card that moves every minute |

## Build and install

Needs JDK 17 and the Android SDK (`sdk.dir` in `launcher/local.properties`).

```
cd launcher
./gradlew assembleRelease            # Persona 5 look (default)
./gradlew assembleRelease -Pstyle=tvos   # original Apple TV look
adb connect <tv-ip>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell pm grant com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION
adb shell am start -n com.gathin.tvhome/.MainActivity
```

To make it the default Home app, see the upstream project's instructions. Edit `launcher/app/src/main/res/values/config.xml`
for the dock apps (package names), hidden apps and Live TV package.

## Switch style without rebuilding

Put a `config.json` in `/sdcard/Android/data/com.gathin.tvhome/files/` on the TV:

```
{ "style": "tvos" }        // or "persona5"
```

An optional `wallpaper.jpg` in the same folder becomes the background. Bring your own: wallpapers with game art are not
included here and should not be redistributed. See [launcher/PERSONA5.md](launcher/PERSONA5.md).

## Notes

- Text in the calendar, ransom titles and screensaver is English only (the bundled fonts cover Latin letters).
- The Shamsi date option is not drawn in the Persona 5 style.
- Fonts: Anton, Bebas Neue, Alfa Slab One, Abril Fatface, all SIL OFL 1.1 (licences in `launcher/fonts-license/`).
- Not affiliated with or endorsed by Atlus or Sega. Persona is a trademark of its owners.
