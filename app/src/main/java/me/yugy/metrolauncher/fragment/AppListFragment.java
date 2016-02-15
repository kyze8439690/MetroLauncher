package me.yugy.metrolauncher.fragment;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;

import java.util.List;

import me.yugy.app.common.utils.DebugUtils;
import me.yugy.metrolauncher.adapter.AppListAdapter;
import me.yugy.metrolauncher.loader.AppListLoader;
import me.yugy.metrolauncher.core.Conf;
import me.yugy.metrolauncher.model.AppInfo;

public class AppListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<AppInfo>> {

    public static AppListFragment newInstance() {
        return new AppListFragment();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDividerHeight(0);
        getListView().setDivider(null);
        getListView().setSelector(new ColorDrawable(0));
        getLoaderManager().initLoader(Conf.LOADER_ID_LOAD_APP_LIST, null, this);
    }

    @Override
    public Loader<List<AppInfo>> onCreateLoader(int id, Bundle args) {
        if (id == Conf.LOADER_ID_LOAD_APP_LIST) {
            return new AppListLoader(getActivity());
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<AppInfo>> loader, List<AppInfo> data) {
        DebugUtils.log("onLoadFinished");
        setListAdapter(new AppListAdapter(data));
    }

    @Override
    public void onLoaderReset(Loader<List<AppInfo>> loader) {
        setListAdapter(null);
    }

}
