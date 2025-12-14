package cn.pylin.xycjd;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String packageName;
    private String appName;
    private Drawable appIcon;
    private boolean isSystemApp;
    private int versionCode;
    private String versionName;
    private boolean isChecked;

    public AppInfo(String packageName, String appName, Drawable appIcon, boolean isSystemApp, int versionCode, String versionName) {
        this.packageName = packageName;
        this.appName = appName;
        this.appIcon = appIcon;
        this.isSystemApp = isSystemApp;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.isChecked = false;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public void setSystemApp(boolean systemApp) {
        isSystemApp = systemApp;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}