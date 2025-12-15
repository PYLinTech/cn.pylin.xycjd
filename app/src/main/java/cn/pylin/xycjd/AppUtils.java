package cn.pylin.xycjd;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppUtils {
    private static final String TAG = "AppUtils";

    /**
     * 获取所有已安装应用列表
     * @param context 上下文
     * @param includeSystemApps 是否包含系统应用
     * @return 应用列表
     */
    public static List<AppInfo> getInstalledApps(Context context, boolean includeSystemApps) {
        List<AppInfo> appInfoList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        
        try {
            // 获取所有已安装的应用
            List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
            
            for (PackageInfo packageInfo : packageInfoList) {
                try {
                    ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                    
                    // 检查是否是系统应用
                    boolean isSystemApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    
                    // 根据参数决定是否包含系统应用
                    if (!includeSystemApps && isSystemApp) {
                        continue;
                    }
                    
                    // 获取应用名称
                    String appName = applicationInfo.loadLabel(packageManager).toString();
                    
                    // 获取应用图标
                    // 注意：这里不直接获取图标，而是在需要时异步加载，避免在主线程中进行耗时操作
                    
                    // 创建AppInfo对象
                    AppInfo appInfo = new AppInfo(
                            packageInfo.packageName,
                            appName,
                            null, // 图标稍后异步加载
                            isSystemApp,
                            packageInfo.versionCode,
                            packageInfo.versionName
                    );
                    
                    appInfoList.add(appInfo);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing package: " + packageInfo.packageName, e);
                }
            }
            
            // 按应用名称排序
            Collections.sort(appInfoList, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo o1, AppInfo o2) {
                    return o1.getAppName().compareToIgnoreCase(o2.getAppName());
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps", e);
        }
        
        return appInfoList;
    }
    
    /**
     * 异步加载应用图标
     * @param context 上下文
     * @param packageName 包名
     * @return 应用图标
     */
    public static android.graphics.drawable.Drawable loadAppIcon(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return applicationInfo.loadIcon(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error loading icon for package: " + packageName, e);
            return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        }
    }
    

}