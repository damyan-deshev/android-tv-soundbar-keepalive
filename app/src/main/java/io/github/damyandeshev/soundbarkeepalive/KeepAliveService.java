package io.github.damyandeshev.soundbarkeepalive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeepAliveService extends Service {
    public static final String ACTION_START = "io.github.damyandeshev.soundbarkeepalive.START";
    public static final String ACTION_STOP = "io.github.damyandeshev.soundbarkeepalive.STOP";
    public static final String ACTION_PULSE = "io.github.damyandeshev.soundbarkeepalive.PULSE";

    public static final String EXTRA_FREQUENCY_HZ = "frequency_hz";
    public static final String EXTRA_AMPLITUDE = "amplitude";
    public static final String EXTRA_DURATION_MS = "duration_ms";
    public static final String EXTRA_INTERVAL_SEC = "interval_sec";
    public static final String EXTRA_SAMPLE_RATE = "sample_rate";

    private static final String TAG = "SoundbarKeepalive";
    private static final String CHANNEL_ID = "soundbar_keepalive";
    private static final int NOTIFICATION_ID = 7603;
    public static final int DEFAULT_FREQUENCY_HZ = 25000;
    public static final int DEFAULT_AMPLITUDE = 900;
    public static final int DEFAULT_DURATION_MS = 6000;
    public static final int DEFAULT_INTERVAL_SEC = 540;
    public static final int DEFAULT_SAMPLE_RATE = 96000;

    private static final String PREFS_NAME = "settings";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int frequencyHz;
    private int amplitude;
    private int durationMs;
    private int intervalSec;
    private int sampleRate;
    private boolean scheduled;

    private final Runnable pulseRunnable = new Runnable() {
        @Override
        public void run() {
            playPulseAsync(false);
            if (scheduled) {
                handler.postDelayed(this, Math.max(30, intervalSec) * 1000L);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        loadConfig(null);
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification("ready"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSchedule();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        loadConfig(intent);
        startForeground(NOTIFICATION_ID, buildNotification("active"));

        if (ACTION_PULSE.equals(action)) {
            playPulseAsync(true);
        } else {
            startSchedule();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopSchedule();
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startSchedule() {
        stopSchedule();
        scheduled = true;
        handler.post(pulseRunnable);
    }

    private void stopSchedule() {
        scheduled = false;
        handler.removeCallbacks(pulseRunnable);
    }

    private void loadConfig(Intent intent) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int nextFrequency = prefs.getInt(EXTRA_FREQUENCY_HZ, DEFAULT_FREQUENCY_HZ);
        int nextAmplitude = prefs.getInt(EXTRA_AMPLITUDE, DEFAULT_AMPLITUDE);
        int nextDuration = prefs.getInt(EXTRA_DURATION_MS, DEFAULT_DURATION_MS);
        int nextInterval = prefs.getInt(EXTRA_INTERVAL_SEC, DEFAULT_INTERVAL_SEC);
        int nextSampleRate = prefs.getInt(EXTRA_SAMPLE_RATE, DEFAULT_SAMPLE_RATE);

        if (intent != null) {
            nextFrequency = intent.getIntExtra(EXTRA_FREQUENCY_HZ, nextFrequency);
            nextAmplitude = intent.getIntExtra(EXTRA_AMPLITUDE, nextAmplitude);
            nextDuration = intent.getIntExtra(EXTRA_DURATION_MS, nextDuration);
            nextInterval = intent.getIntExtra(EXTRA_INTERVAL_SEC, nextInterval);
            nextSampleRate = intent.getIntExtra(EXTRA_SAMPLE_RATE, nextSampleRate);
        }

        frequencyHz = clamp(nextFrequency, 0, 47000);
        amplitude = clamp(nextAmplitude, 0, 32767);
        durationMs = clamp(nextDuration, 50, 600000);
        intervalSec = clamp(nextInterval, 30, 3600);
        sampleRate = clamp(nextSampleRate, 8000, 192000);

        prefs.edit()
                .putInt(EXTRA_FREQUENCY_HZ, frequencyHz)
                .putInt(EXTRA_AMPLITUDE, amplitude)
                .putInt(EXTRA_DURATION_MS, durationMs)
                .putInt(EXTRA_INTERVAL_SEC, intervalSec)
                .putInt(EXTRA_SAMPLE_RATE, sampleRate)
                .apply();
    }

    private void playPulseAsync(final boolean stopAfterPulse) {
        final int hz = frequencyHz;
        final int amp = amplitude;
        final int ms = durationMs;
        final int rate = sampleRate;
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    playPulse(hz, amp, ms, rate);
                } finally {
                    if (stopAfterPulse) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                stopForeground(true);
                                stopSelf();
                            }
                        });
                    }
                }
            }
        });
    }

    private void playPulse(int hz, int amp, int ms, int rate) {
        Log.i(TAG, "pulse start hz=" + hz + " amp=" + amp + " ms=" + ms + " rate=" + rate);
        int samples = Math.max(1, (rate * ms) / 1000);
        short[] pcm = new short[samples * 2];
        int toneHz = hz >= rate / 2 ? Math.max(0, (rate / 2) - 100) : hz;
        double step = (2.0 * Math.PI * toneHz) / rate;
        int fadeSamples = Math.min(samples / 4, Math.max(1, rate / 200));

        for (int i = 0; i < samples; i++) {
            double envelope = 1.0;
            if (i < fadeSamples) {
                envelope = (double) i / fadeSamples;
            } else if (i >= samples - fadeSamples) {
                envelope = (double) (samples - i - 1) / fadeSamples;
            }
            short value = toneHz == 0 || amp == 0
                    ? 0
                    : (short) Math.round(Math.sin(step * i) * amp * Math.max(0.0, envelope));
            pcm[i * 2] = value;
            pcm[i * 2 + 1] = value;
        }

        int byteCount = pcm.length * 2;
        AudioTrack track = null;
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(byteCount)
                    .build();
            track.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            track.play();
            Thread.sleep(ms + 150L);
        } catch (Exception e) {
            Log.e(TAG, "pulse failed", e);
        } finally {
            if (track != null) {
                try {
                    track.stop();
                } catch (Exception ignored) {
                }
                track.release();
            }
            Log.i(TAG, "pulse finish");
        }
    }

    private Notification buildNotification(String state) {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                launch,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Soundbar Keepalive")
                .setContentText(state + " " + frequencyHz + "Hz amp " + amplitude + " every " + intervalSec + "s")
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
