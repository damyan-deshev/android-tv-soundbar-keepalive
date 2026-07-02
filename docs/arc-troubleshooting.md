# HDMI ARC Troubleshooting Notes

These notes capture observed Android TV + HDMI ARC failure modes so the next
debugging session can start from evidence instead of rediscovering the basics.

## Key Observations

- A running foreground service does not prove that the soundbar is receiving the
  keepalive pulse. If ARC is gone, Android can keep creating successful
  `AudioTrack`s while routing them to the built-in speaker.
- In one observed failure, the app process and foreground service were still
  alive, the pulse schedule continued, and pulse `AudioTrack`s completed without
  underruns. The actual failure was ARC route loss.
- The CEC history showed the audio system sending `<Terminate ARC>`. The TV then
  replied with `<Report ARC Terminated>`, disabled System Audio Mode, and
  `AudioService` marked the HDMI ARC output unavailable.
- After ARC is terminated, ordinary app playback cannot wake the bar through ARC,
  because the app no longer has an ARC route to play into.
- When ARC is healthy again, the same keepalive pulse routes to `HDMI_ARC` and
  looks normal in `media.metrics`.

## What To Check First

Use the active TV ADB transport id instead of hard-coding a private address:

```sh
TV_TID="$(adb devices -l | awk '/ device / && /product:/ && !/emulator/ {sub(/transport_id:/,"",$NF); print $NF; exit}')"
```

Check ARC and CEC state:

```sh
adb -t "$TV_TID" shell 'dumpsys hdmi_control | grep -E "mSystemAudioActivated|mArcEstablished|mSamStatusOfArc|Terminate ARC|Initiate ARC|Request ARC|Set System Audio|Report ARC" | tail -n 220'
```

Check Android audio routing:

```sh
adb -t "$TV_TID" shell 'dumpsys audio | grep -Ei "STREAM_MUSIC|Devices:|setWiredDeviceConnectionState|hmdi_arc|hdmi_arc|speaker" | tail -n 240'
```

Check whether this app's pulses were actually routed to ARC or to speakers:

```sh
adb -t "$TV_TID" shell 'dumpsys media.metrics | grep -Ei "soundbarkeepalive|audiotrack|audiotrackdeviceusage|devices:|SPEAKER|HDMI_ARC|sample_rate|frame_count|xruns" | tail -n 240'
```

Check service/package state only after checking the route:

```sh
adb -t "$TV_TID" shell dumpsys activity services io.github.damyandeshev.soundbarkeepalive/.KeepAliveService
adb -t "$TV_TID" shell dumpsys package io.github.damyandeshev.soundbarkeepalive
```

## Failure Signature

This pattern means the app is probably not the root cause:

```text
CEC:    <Terminate ARC>
CEC:    <Report ARC Terminated>
CEC:    <Set System Audio Mode> ...:00
Audio:  hmdi_arc DEVICE_STATE_UNAVAILABLE
Route:  keepalive AudioTrack devices=SPEAKER
App:    foreground service still running
```

At that point the pulse is still being generated, but it is no longer reaching
the soundbar.

## Recovery Attempts

The clean recovery is to get the TV and audio system to redo the System Audio
Mode / ARC handshake:

```text
TV -> audio system: System Audio Mode Request
TV -> audio system: Request ARC Initiation
audio system -> TV: Initiate ARC
TV: ARC available, STREAM_MUSIC routes to HDMI_ARC
```

From ADB shell, this may help when the TV is still configured for the HDMI sound
system:

```sh
adb -t "$TV_TID" shell cmd hdmi_control setsam on
adb -t "$TV_TID" shell cmd hdmi_control setarc on
```

If the TV has fallen back to speakers, switching the TV audio output to speakers
and then back to the HDMI sound system was observed to trigger the full
handshake. Some vendor/JointSpace audio-output calls can trigger the same path,
but they may be UI-dependent and can show on-screen menus, so they are not a
good in-app recovery mechanism.

## Why The App Cannot Intercept ARC Termination

A normal sideloaded APK cannot drop or rewrite incoming HDMI-CEC messages before
the OS handles them. The relevant Android permissions are system/privileged:

- `android.permission.HDMI_CEC` is `signature|privileged|vendorPrivileged`
- `android.permission.CHANGE_HDMI_CEC_ACTIVE_SOURCE` is `signature|privileged`
- `android.permission.MODIFY_AUDIO_ROUTING` is `signature|privileged|role`

Likewise, calling `cmd hdmi_control` from inside the app does not become an ADB
shell call. It runs with the app UID and lacks the shell/system privileges that
make the command useful over ADB.

## Practical Direction

The app can reasonably do two things:

- Prefer an available HDMI ARC output when Android exposes one.
- Detect and record when a pulse routes to the built-in speaker, so the next
  failure has a precise app-side timestamp.

It cannot, as a normal APK, recreate a missing ARC audio device after the audio
system has terminated ARC.
