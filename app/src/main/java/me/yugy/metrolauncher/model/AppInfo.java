package me.yugy.metrolauncher.model;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import me.yugy.app.common.utils.PinyinUtils;
import me.yugy.metrolauncher.core.Application;

public class AppInfo {

    public CharSequence label;

    public char firstChar;

    public Drawable icon;

    public Intent intent;

    public static AppInfo fromResolveInfo(PackageManager pm, ResolveInfo info, ComponentName component) {
        AppInfo appInfo = new AppInfo();
        appInfo.label = info.loadLabel(pm);
        if (appInfo.label.length() == 0) {
            appInfo.firstChar = '#';
        } else {
            appInfo.firstChar = appInfo.label.charAt(0);
            if (me.yugy.app.common.utils.TextUtils.isCharCJK(appInfo.firstChar)) {
                String s = PinyinUtils.toPinyin(Application.getContext(), appInfo.firstChar);
                if (TextUtils.isEmpty(s)) {
                    appInfo.firstChar = '#';
                } else {
                    appInfo.firstChar = s.charAt(0);
                }
            }
        }
        appInfo.firstChar = Character.toUpperCase(appInfo.firstChar);
        if (!Character.isLetter(appInfo.firstChar)) {
            appInfo.firstChar = '#';
        }
        appInfo.icon = info.loadIcon(pm);
        appInfo.intent = new Intent(Intent.ACTION_MAIN);
        appInfo.intent.addCategory(Intent.CATEGORY_LAUNCHER);
        appInfo.intent.setComponent(component);
        appInfo.intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return appInfo;
    }
}
