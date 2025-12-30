package cn.pylin.xycjd.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import cn.pylin.xycjd.R;

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
        Toast.makeText(context, context.getString(R.string.floating_window_permission_denied), Toast.LENGTH_SHORT).show(); //显示权限被拒绝的提示信息
    }
}