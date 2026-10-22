package com.gathin.tvhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserManager;
import android.view.Gravity;
import android.widget.TextView;

/** Draws HOME during Direct Boot, then opens the full launcher after credential storage unlocks. */
public final class BootHomeActivity extends Activity {
    private boolean registered;
    private boolean opening;
    private final BroadcastReceiver unlocked = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_UNLOCKED.equals(intent.getAction())) openLauncher();
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView placeholder = new TextView(this);
        placeholder.setBackgroundColor(Color.BLACK);
        placeholder.setTextColor(Color.WHITE);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setTextSize(24);
        placeholder.setText("正在启动…");
        setContentView(placeholder);
        UserManager users = getSystemService(UserManager.class);
        if (users != null && users.isUserUnlocked()) {
            openLauncher();
            return;
        }
        // USER_UNLOCKED is a protected system broadcast; no third-party sender is accepted.
        registerReceiver(unlocked, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        registered = true;
        // Close the race in which unlock happened between the first check and registration.
        if (users != null && users.isUserUnlocked()) openLauncher();
    }

    private void openLauncher() {
        if (opening || isFinishing()) return;
        opening = true;
        startActivity(new Intent(this, MainActivity.class).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override protected void onDestroy() {
        if (registered) unregisterReceiver(unlocked);
        super.onDestroy();
    }
}
