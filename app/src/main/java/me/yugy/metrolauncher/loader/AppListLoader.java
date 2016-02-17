package me.yugy.metrolauncher.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.yugy.metrolauncher.AppListModifyReceiver;
import me.yugy.metrolauncher.model.AppInfo;

public class AppListLoader extends AsyncTaskLoader<List<AppInfo>> {

    private PackageManager mPackageManager;
    @Nullable private List<AppInfo> mApps;
    @Nullable private AppListModifyReceiver mReceiver;

    public AppListLoader(Context context) {
        super(context);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public List<AppInfo> loadInBackground() {
        Intent filter = new Intent(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = mPackageManager.queryIntentActivities(filter, 0);
        if (mApps == null) {
            mApps = new ArrayList<>();
        }
        for (ResolveInfo info : apps) {
            ComponentName componentName = new ComponentName(
                    info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name);
            AppInfo appInfo = AppInfo.fromResolveInfo(mPackageManager, info, componentName);
            mApps.add(appInfo);
        }
        Collections.sort(mApps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return lhs.firstChar - rhs.firstChar;
            }
        });
        return mApps;
    }

    @Override
    public void deliverResult(List<AppInfo> data) {
        if (isReset()) {
            onReleaseResources(data);
            return;
        }

        List<AppInfo> oldApps = mApps;

        mApps = data;

        if (isStarted()) {
            super.deliverResult(data);
        }

        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mApps != null) {
            deliverResult(mApps);
        }

        if (mReceiver == null) {
            mReceiver = new AppListModifyReceiver(this);
        }

        if (takeContentChanged() || mApps == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(@Nullable List<AppInfo> data) {
        super.onCanceled(data);
        onReleaseResources(data);
    }

    private void onReleaseResources(List<AppInfo> data) {
        if (data != null) {
            data.clear();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();

        if (mApps != null) {
            onReleaseResources(mApps);
            mApps = null;
        }

        if (mReceiver != null) {
            getContext().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }


}
