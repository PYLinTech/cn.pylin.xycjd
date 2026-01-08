package cn.pylin.xycjd.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.pylin.xycjd.BuildConfig;
import cn.pylin.xycjd.IUserShellService;
import cn.pylin.xycjd.R;
import cn.pylin.xycjd.service.UserShellService;
import rikka.shizuku.Shizuku;

public class ShizukuShellHelper {

    public interface Callback {
        void onResult(@NonNull String result);
        void onError(@NonNull String error);
    }

    private static ShizukuShellHelper instance;
    private IUserShellService userService;
    private ServiceConnection connection;
    private final Context context;

    private ShizukuShellHelper(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public static ShizukuShellHelper getInstance() {
        return getInstance(null);
    }

    public static ShizukuShellHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ShizukuShellHelper(context);
        }
        // 如果已有实例但 context 为空，尝试更新为有效的 ApplicationContext
        if (instance.context == null && context != null) {
            try {
                java.lang.reflect.Field contextField = ShizukuShellHelper.class.getDeclaredField("context");
                contextField.setAccessible(true);
                contextField.set(instance, context.getApplicationContext());
            } catch (Exception ignored) {}
        }
        return instance;
    }

    /** ---------------- 执行命令（自动绑定） ---------------- */
    public void execCommand(@NonNull String command, long timeoutMs, @NonNull Callback callback) {

        // 1. 基础检查
        if (!Shizuku.pingBinder()) {
            callback.onError(context.getString(R.string.shizuku_not_run));
            return;
        }

        if (Shizuku.checkSelfPermission()
                != PackageManager.PERMISSION_GRANTED) {
            callback.onError(context.getString(R.string.shizuku_not_authorized));
            return;
        }

        // 2. 已绑定，直接执行
        if (userService != null) {
            runCommand(command, timeoutMs, callback);
            return;
        }

        // 3. 构造 UserService 参数
        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                new ComponentName(
                        BuildConfig.APPLICATION_ID,
                        UserShellService.class.getName()
                )
        )
                .daemon(false)
                .processNameSuffix("shell")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE);

        // 4. ServiceConnection
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                userService = IUserShellService.Stub.asInterface(binder);
                runCommand(command, timeoutMs, callback);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                userService = null;
            }
        };

        // 5. 关键：根据 Binder 状态决定是否等待
        try {
            if (Shizuku.pingBinder()) {
                // Binder 已 ready，直接 bind（最常见情况）
                Shizuku.bindUserService(args, connection);
            } else {
                // Binder 未 ready，等它 ready 再 bind
                Shizuku.addBinderReceivedListener(() -> {
                    try {
                        Shizuku.bindUserService(args, connection);
                    } catch (Exception e) {
                        callback.onError(context.getString(R.string.shizuku_service_error));
                    }
                });
            }
        } catch (Exception e) {
            callback.onError(context.getString(R.string.shizuku_service_error));
        }
    }

    /** ---------------- 解绑/销毁服务 ---------------- */
    public void unbindService(boolean remove) {
        if (userService == null) return;
        try {
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                    new ComponentName(
                            BuildConfig.APPLICATION_ID,
                            UserShellService.class.getName()
                    )
            )
                    .daemon(false)
                    .processNameSuffix("shell")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE);
            Shizuku.unbindUserService(args, connection, remove);
        } catch (Exception ignored) {}
        userService = null;
        connection = null;
    }

    /** ---------------- 内部方法：异步执行命令 + 超时 ---------------- */
    private void runCommand(String command, long timeoutMs, Callback callback) {
        AtomicBoolean finished = new AtomicBoolean(false);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        mainHandler.postDelayed(() -> {
            if (!finished.getAndSet(true)) callback.onError("命令执行超时");
        }, timeoutMs);

        new Thread(() -> {
            try {
                String output = userService.execLine(command);
                if (!finished.getAndSet(true)) mainHandler.post(() -> callback.onResult(output != null ? output : ""));
            } catch (Exception e) {
                if (!finished.getAndSet(true)) mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "未知错误"));
            }
        }).start();
    }
}