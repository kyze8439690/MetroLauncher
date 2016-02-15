package me.yugy.metrolauncher.loader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.yugy.metrolauncher.model.AppInfo;

public class AppListLoader extends AsyncTaskLoader<List<AppInfo>> {

    private PackageManager mPackageManager;

    public AppListLoader(Context context) {
        super(context);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public List<AppInfo> loadInBackground() {
        Intent filter = new Intent(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = mPackageManager.queryIntentActivities(filter, 0);
        List<AppInfo> appList = new ArrayList<>();
        for (ResolveInfo info : apps) {
            ComponentName componentName = new ComponentName(
                    info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name);
            AppInfo appInfo = AppInfo.fromResolveInfo(mPackageManager, info, componentName);
            appList.add(appInfo);
        }
        Collections.sort(appList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return lhs.firstChar - rhs.firstChar;
            }
        });
        return appList;
    }

    @Override
    public void deliverResult(List<AppInfo> data) {
        if (isReset()) {
            data.clear();
            return;
        }
        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(List<AppInfo> data) {
        data.clear();
    }

    @Override
    protected void onReset() {
        onStopLoading();
    }


}
