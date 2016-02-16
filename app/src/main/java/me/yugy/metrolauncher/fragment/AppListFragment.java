package me.yugy.metrolauncher.fragment;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;

import java.util.List;

import me.yugy.app.common.utils.DebugUtils;
import me.yugy.app.common.utils.UIUtils;
import me.yugy.app.common.utils.VersionUtils;
import me.yugy.metrolauncher.adapter.AppListAdapter;
import me.yugy.metrolauncher.loader.AppListLoader;
import me.yugy.metrolauncher.core.Conf;
import me.yugy.metrolauncher.model.AppInfo;

public class AppListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<AppInfo>> {

    public static AppListFragment newInstance() {
        return new AppListFragment();
    }

    private AppListAdapter mAdapter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDividerHeight(0);
        getListView().setDivider(null);
        getListView().setSelector(new ColorDrawable(0));
        getListView().setOverScrollMode(View.OVER_SCROLL_NEVER);
        getListView().setClipToPadding(false);
        if (VersionUtils.lollipopOrLater()) {
            getListView().setPadding(
                    getListView().getPaddingLeft(),
                    getListView().getPaddingTop() + UIUtils.getStatusBarHeight(getActivity()),
                    getListView().getPaddingRight(),
                    getListView().getPaddingBottom());
        }
        mAdapter = new AppListAdapter();
        setListAdapter(mAdapter);
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
        mAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<AppInfo>> loader) {
        mAdapter.setData(null);
    }

}
