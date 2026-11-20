# Installing SwadayaTV

This repository publishes the launcher as a ready-made APK on [Releases](https://github.com/frhnkrnwn/SwadayaTV/releases/latest); it does not ship a separate desktop setup app. The install pages cover both ways:

- [Windows](install-windows.md), [macOS](install-macos.md) and [Linux](install-linux.md): build from source and install over network ADB, or install the ready-made APK with only adb.
- On the TV itself, with no computer: copy `tvhome-<version>.apk` to a USB drive, or download it with the Downloader app, allow installing from that app, install it, then pick SwadayaTV as Home in the TV's own settings.

The APK carries every feature. The Net traffic monitor downloads its engine files from the same release the first time it starts, so nothing has to be pushed over ADB. **Settings → About** checks GitHub for a newer release and installs it in place; settings are kept.

USB debugging is optional. It is only needed for Now Playing, the Live log screen saver on Android 12 and older, switching the screen saver from Settings, restart and shut down, force-closing apps, and the phone remote's keyboard and screenshot.
