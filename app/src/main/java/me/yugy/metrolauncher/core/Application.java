package me.yugy.metrolauncher.core;

import android.content.Context;

import me.yugy.app.common.utils.DebugUtils;
import me.yugy.metrolauncher.BuildConfig;

public class Application extends android.app.Application {

    private static Application sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        DebugUtils.setLogEnable(BuildConfig.DEBUG);
    }

    public static Context getContext() {
        return sInstance;
    }
}
