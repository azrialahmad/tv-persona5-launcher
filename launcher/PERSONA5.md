# Persona 5 style

Default look. Original Apple TV style:
- no rebuild: put `"style": "tvos"` in config.json on the TV (`/sdcard/Android/data/com.gathin.tvhome/files/`), restart Home;
  use `"style": "persona5"` to come back.
- at build time: `gradlew assembleRelease -Pstyle=tvos`.

All art is vector code (P5.java, P5Views.java). Fonts (OFL, licences in fonts-license/): Anton, Bebas Neue,
Alfa Slab One, Abril Fatface. The calendar corner, ransom titles and screensaver use English text (the fonts are Latin only).
