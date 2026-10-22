package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** One launch after OEM boot handling finishes; no persistent service or key filtering. */
public final class BootHomeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo selected = context.getPackageManager().resolveActivity(home, 0);
        if (selected == null || selected.activityInfo == null
                || !context.getPackageName().equals(selected.activityInfo.packageName)) return;
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                app.startActivity(new Intent(app, BootHomeActivity.class).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                Log.i("TvHomeBoot", "Opened configured home after OEM boot");
            } catch (RuntimeException error) {
                Log.e("TvHomeBoot", "Could not open home after boot", error);
            } finally {
                pending.finish();
            }
        }, 5000);
    }
}
