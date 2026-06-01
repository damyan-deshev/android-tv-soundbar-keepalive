package io.github.damyandeshev.soundbarkeepalive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class KeepAliveReceiver extends BroadcastReceiver {
    private static final String TAG = "SoundbarKeepalive";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.i(TAG, "receiver restart action=" + action);
            KeepAliveService.startIfEnabled(context, action);
        }
    }
}
