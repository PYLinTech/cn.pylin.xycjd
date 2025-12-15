package cn.pylin.xycjd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

public class FloatingWindowPermissionManager {

    public static boolean hasPermission(Context context) {
        return Settings.canDrawOverlays(context);
    }

    public static void requestPermission(Activity activity, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, requestCode);
    }

    public static void showPermissionDeniedMessage(Context context) {
        Toast.makeText(context, R.string.floating_window_permission_denied, Toast.LENGTH_SHORT).show();
    }
}