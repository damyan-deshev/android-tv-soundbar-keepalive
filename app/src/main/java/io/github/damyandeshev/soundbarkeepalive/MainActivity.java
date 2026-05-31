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
import android.widget.FrameLayout;
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

        FrameLayout frame = new FrameLayout(this);

        final ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(48, 32, 80, 32);
        scrollView.addView(root);
        frame.addView(scrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        View scrollTrack = new View(this);
        scrollTrack.setBackgroundColor(Color.rgb(78, 78, 78));
        FrameLayout.LayoutParams trackParams = new FrameLayout.LayoutParams(4,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.RIGHT);
        trackParams.setMargins(0, 40, 24, 40);
        frame.addView(scrollTrack, trackParams);

        final View scrollThumb = new View(this);
        scrollThumb.setBackgroundColor(Color.LTGRAY);
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(8, 160,
                Gravity.RIGHT | Gravity.TOP);
        thumbParams.setMargins(0, 56, 22, 0);
        frame.addView(scrollThumb, thumbParams);

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                updateScrollThumb(scrollView, scrollThumb);
            }
        });
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    updateScrollThumb(scrollView, scrollThumb);
                }
            });
        }

        TextView title = new TextView(this);
        title.setText("Soundbar Keepalive");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidth());

        LinearLayout firstRow = addRow(root, 20);
        frequencyInput = addNumberField(firstRow, "Frequency Hz",
                prefs.getInt(KeepAliveService.EXTRA_FREQUENCY_HZ, KeepAliveService.DEFAULT_FREQUENCY_HZ));
        sampleRateInput = addNumberField(firstRow, "Sample rate",
                prefs.getInt(KeepAliveService.EXTRA_SAMPLE_RATE, KeepAliveService.DEFAULT_SAMPLE_RATE));
        amplitudeInput = addNumberField(firstRow, "Amplitude",
                prefs.getInt(KeepAliveService.EXTRA_AMPLITUDE, KeepAliveService.DEFAULT_AMPLITUDE));

        LinearLayout secondRow = addRow(root, 14);
        durationInput = addNumberField(secondRow, "Pulse ms",
                prefs.getInt(KeepAliveService.EXTRA_DURATION_MS, KeepAliveService.DEFAULT_DURATION_MS));
        intervalInput = addNumberField(secondRow, "Interval sec",
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

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionRowParams = fullWidth();
        actionRowParams.setMargins(0, 18, 0, 0);
        root.addView(actions, actionRowParams);

        addCompactButton(actions, "Start", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConfigured(KeepAliveService.ACTION_START);
                status.setText("Running");
            }
        });
        addCompactButton(actions, "Save", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveConfigured();
                status.setText("Saved");
            }
        });
        addCompactButton(actions, "Pulse Once", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConfigured(KeepAliveService.ACTION_PULSE);
                status.setText("Pulse sent");
            }
        });
        addCompactButton(actions, "Stop", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, KeepAliveService.class);
                intent.setAction(KeepAliveService.ACTION_STOP);
                startService(intent);
                status.setText("Stopped");
            }
        });
        addCompactButton(actions, "Exit", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    finishAndRemoveTask();
                } else {
                    finish();
                }
            }
        });

        status = new TextView(this);
        status.setText("Idle");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = fullWidth();
        statusParams.setMargins(0, 18, 0, 0);
        root.addView(status, statusParams);

        setContentView(frame);
    }

    private void updateScrollThumb(ScrollView scrollView, View scrollThumb) {
        int viewportHeight = Math.max(1, scrollView.getHeight());
        View content = scrollView.getChildAt(0);
        int contentHeight = content == null ? viewportHeight : Math.max(viewportHeight, content.getHeight());
        int topOffset = 56;
        int bottomOffset = 56;
        int availableHeight = Math.max(1, viewportHeight - topOffset - bottomOffset);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) scrollThumb.getLayoutParams();
        if (contentHeight <= viewportHeight) {
            params.height = Math.min(availableHeight, 160);
            params.topMargin = topOffset;
            scrollThumb.setAlpha(0.45f);
        } else {
            int scrollRange = contentHeight - viewportHeight;
            params.height = Math.max(96, (viewportHeight * availableHeight) / contentHeight);
            int maxThumbTop = topOffset + Math.max(0, availableHeight - params.height);
            params.topMargin = topOffset + ((maxThumbTop - topOffset) * scrollView.getScrollY()) / scrollRange;
            scrollThumb.setAlpha(0.9f);
        }
        scrollThumb.setLayoutParams(params);
    }

    private LinearLayout addRow(LinearLayout root, int topMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(0, topMargin, 0, 0);
        root.addView(row, params);
        return row;
    }

    private EditText addNumberField(LinearLayout parent, String label, int value) {
        LinearLayout field = new LinearLayout(this);
        field.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        fieldParams.setMargins(8, 0, 8, 0);
        parent.addView(field, fieldParams);

        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextColor(Color.LTGRAY);
        textView.setTextSize(16);
        field.addView(textView, fullWidth());

        EditText editText = new EditText(this);
        editText.setText(String.valueOf(value));
        editText.setSingleLine(true);
        editText.setSelectAllOnFocus(true);
        editText.setTextSize(20);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.addView(editText, fullWidth());
        return editText;
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

    private void saveConfigured() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KeepAliveService.EXTRA_FREQUENCY_HZ,
                        readInt(frequencyInput, KeepAliveService.DEFAULT_FREQUENCY_HZ))
                .putInt(KeepAliveService.EXTRA_SAMPLE_RATE,
                        readInt(sampleRateInput, KeepAliveService.DEFAULT_SAMPLE_RATE))
                .putInt(KeepAliveService.EXTRA_AMPLITUDE,
                        readInt(amplitudeInput, KeepAliveService.DEFAULT_AMPLITUDE))
                .putInt(KeepAliveService.EXTRA_DURATION_MS,
                        readInt(durationInput, KeepAliveService.DEFAULT_DURATION_MS))
                .putInt(KeepAliveService.EXTRA_INTERVAL_SEC,
                        readInt(intervalInput, KeepAliveService.DEFAULT_INTERVAL_SEC))
                .apply();
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
