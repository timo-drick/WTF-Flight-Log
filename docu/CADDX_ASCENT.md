# Caddx Ascent system

[Back to the main README](../README.md)

WTF Flight Log supports video, OSD, and subtitle/telemetry recordings from the Caddx Ascent system.

## Tested hardware

Support has been tested with:

- **Walksnail Ascent FPV VRX Pro**.
- **Walksnail Ascent HD Lite+**.

Ascent OSD parsing has been tested with Betaflight data.

## Prepare a recording

1. Configure your flight controller to send its OSD to the Ascent system using MSP DisplayPort.
2. Turn **Rec OSD off** in the receiver's settings. This prevents the OSD from being baked into the video and is required to save a separate `.osd` file.
3. Turn **Google Icon Style on** in the receiver's settings. This is required to save a `.srt` telemetry file.
4. Record a flight using the receiver's DVR.
5. Check that matching `.osd` and `.srt` files were saved alongside the video. The `.osd` file is required to reconstruct the flight controller OSD and extract flight data in WTF Flight Log.
6. Copy the video and its matching `.osd` and `.srt` files from the receiver. Keep them together with their original filenames.

If no `.osd` file is present, confirm that **Rec OSD is off** and check the flight controller's MSP DisplayPort configuration. If no `.srt` file is present, confirm that **Google Icon Style is on** before recording another flight.

## Recording files

Import the video recording (`.mp4` or `.mov`) together with its matching data files. For example, the Ascent sample included in this project uses:

| Example | Contents |
| --- | --- |
| `AscentG0001.osd` | Flight controller OSD character data. |
| `AscentG0001.srt` | Receiver telemetry, including signal, channel, battery voltage, latency, and bitrate. |

The `.srt` file provides subtitle/telemetry information; the `.osd` file provides the flight controller overlay and the data used for flight analysis.

## Import and playback

1. Open [WTF Flight Log](https://timo-drick.github.io/WTF-Flight-Log/).
2. Choose **Import files** to select the video and matching data files, or **Import directory** to import their folder.
3. Open the resulting log entry to play the video with its OSD and available subtitle overlay.

Available flight statistics depend on the elements in your recorded OSD. For GPS tracks and KML export, include latitude and longitude in the flight controller's OSD layout.
