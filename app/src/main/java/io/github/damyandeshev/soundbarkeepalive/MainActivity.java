package io.github.damyandeshev.soundbarkeepalive;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "settings";

    private EditText frequencyInput;
    private EditText sampleRateInput;
    private EditText amplitudeInput;
    private EditText durationInput;
    private EditText intervalInput;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(48, 40, 48, 48);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Soundbar Keepalive");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidth());

        frequencyInput = addNumberField(root, "Frequency Hz",
                prefs.getInt(KeepAliveService.EXTRA_FREQUENCY_HZ, KeepAliveService.DEFAULT_FREQUENCY_HZ));
        sampleRateInput = addNumberField(root, "Sample rate",
                prefs.getInt(KeepAliveService.EXTRA_SAMPLE_RATE, KeepAliveService.DEFAULT_SAMPLE_RATE));
        amplitudeInput = addNumberField(root, "Amplitude",
                prefs.getInt(KeepAliveService.EXTRA_AMPLITUDE, KeepAliveService.DEFAULT_AMPLITUDE));
        durationInput = addNumberField(root, "Pulse ms",
                prefs.getInt(KeepAliveService.EXTRA_DURATION_MS, KeepAliveService.DEFAULT_DURATION_MS));
        intervalInput = addNumberField(root, "Interval sec",
                prefs.getInt(KeepAliveService.EXTRA_INTERVAL_SEC, KeepAliveService.DEFAULT_INTERVAL_SEC));

        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);
        presets.setPadding(0, 18, 0, 0);
        root.addView(presets, fullWidth());

        addCompactButton(presets, "25k", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyValues(25000, 96000, 900, 6000, 540);
            }
        });
        addCompactButton(presets, "22k", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyValues(22000, 48000, 900, 6000, 540);
            }
        });
        addCompactButton(presets, "Silent", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyValues(0, 48000, 0, 6000, 540);
            }
        });

        addButton(root, "Start", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConfigured(KeepAliveService.ACTION_START);
                status.setText("Running");
            }
        });
        addButton(root, "Pulse Once", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConfigured(KeepAliveService.ACTION_PULSE);
                status.setText("Pulse sent");
            }
        });
        addButton(root, "Stop", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, KeepAliveService.class);
                intent.setAction(KeepAliveService.ACTION_STOP);
                startService(intent);
                status.setText("Stopped");
            }
        });

        status = new TextView(this);
        status.setText("Idle");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = fullWidth();
        statusParams.setMargins(0, 24, 0, 0);
        root.addView(status, statusParams);

        setContentView(scrollView);
    }

    private EditText addNumberField(LinearLayout root, String label, int value) {
        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextColor(Color.LTGRAY);
        textView.setTextSize(16);
        LinearLayout.LayoutParams labelParams = fullWidth();
        labelParams.setMargins(0, 20, 0, 4);
        root.addView(textView, labelParams);

        EditText editText = new EditText(this);
        editText.setText(String.valueOf(value));
        editText.setSingleLine(true);
        editText.setSelectAllOnFocus(true);
        editText.setTextSize(20);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(editText, fullWidth());
        return editText;
    }

    private void addButton(LinearLayout root, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(20);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(0, 20, 0, 0);
        root.addView(button, params);
    }

    private void addCompactButton(LinearLayout root, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(18);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(6, 0, 6, 0);
        root.addView(button, params);
    }

    private void applyValues(int frequencyHz, int sampleRate, int amplitude, int durationMs, int intervalSec) {
        frequencyInput.setText(String.valueOf(frequencyHz));
        sampleRateInput.setText(String.valueOf(sampleRate));
        amplitudeInput.setText(String.valueOf(amplitude));
        durationInput.setText(String.valueOf(durationMs));
        intervalInput.setText(String.valueOf(intervalSec));
    }

    private void startConfigured(String action) {
        Intent intent = new Intent(this, KeepAliveService.class);
        intent.setAction(action);
        intent.putExtra(KeepAliveService.EXTRA_FREQUENCY_HZ,
                readInt(frequencyInput, KeepAliveService.DEFAULT_FREQUENCY_HZ));
        intent.putExtra(KeepAliveService.EXTRA_SAMPLE_RATE,
                readInt(sampleRateInput, KeepAliveService.DEFAULT_SAMPLE_RATE));
        intent.putExtra(KeepAliveService.EXTRA_AMPLITUDE,
                readInt(amplitudeInput, KeepAliveService.DEFAULT_AMPLITUDE));
        intent.putExtra(KeepAliveService.EXTRA_DURATION_MS,
                readInt(durationInput, KeepAliveService.DEFAULT_DURATION_MS));
        intent.putExtra(KeepAliveService.EXTRA_INTERVAL_SEC,
                readInt(intervalInput, KeepAliveService.DEFAULT_INTERVAL_SEC));
        startForegroundServiceCompat(intent);
    }

    private int readInt(EditText input, int fallback) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void startForegroundServiceCompat(Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
