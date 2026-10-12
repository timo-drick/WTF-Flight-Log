# DJI WTF system

[Back to the main README](../README.md)

WTF Flight Log supports DJI recordings with separately recorded OSD data. For the original DJI FPV system, [WTFOS](https://fpv.wtf) and [msp-osd](https://github.com/fpv-wtf/msp-osd) provide the full flight controller OSD and allow it to be recorded alongside the goggles' video.

## Supported devices

- DJI Goggles V1 and V2.
- Original DJI FPV Air Unit and Caddx Vista.
- DJI O3 Air Unit recordings.

DJI O4 Air Unit recordings are not supported. OSD data has been tested with Betaflight and INAV.

## Enable OSD recording with WTFOS / msp-osd

These instructions apply to the original DJI FPV Air Unit / Vista setup using WTFOS and msp-osd.

1. Follow the [msp-osd installation and flight controller setup instructions](https://github.com/fpv-wtf/msp-osd#setup-and-installation). Install WTFOS and msp-osd on the goggles and each Air Unit / Vista, and configure the flight controller to send MSP DisplayPort OSD data.
2. Connect the goggles to the [WTFOS configurator](https://fpv.wtf) and open its CLI.
3. Enable OSD recording:

   ```text
   package-config set msp-osd rec_enabled true
   package-config apply msp-osd
   ```

4. Record a flight with the goggles' DVR.

The O3 recording format is also supported by WTF Flight Log, but the WTFOS / msp-osd setup commands above are for the original Air Unit / Vista system.

## Recording files

A recording can contain the following matching files:

| Example | Contents |
| --- | --- |
| `DJIG0003.mp4` | Goggles DVR video. |
| `DJIG0003.osd` | Flight controller OSD character data. |
| `DJIG0003.srt` | Subtitle/telemetry data. |

Copy the matching files together and keep their original filenames. The `.osd` file is required to reconstruct the flight controller OSD and extract flight data.

## Import and playback

1. Open [WTF Flight Log](https://timo-drick.github.io/WTF-Flight-Log/).
2. Choose **Import files** to select the matching recording files, or **Import directory** to import their folder.
3. Open the resulting log entry to view the video, OSD, and available flight data.

DJI goggles split longer videos into multiple files after about 10 minutes. Import all related video and SRT segments along with the OSD file so the app can group them into one log entry.

For GPS tracks and KML export, latitude and longitude must be visible in the recorded flight controller OSD.
