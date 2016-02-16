package me.yugy.metrolauncher.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.yugy.app.common.core.BaseActivity;
import me.yugy.app.common.utils.UIUtils;
import me.yugy.metrolauncher.R;
import me.yugy.metrolauncher.fragment.AppListFragment;
import me.yugy.metrolauncher.fragment.MainFragment;

public class MainActivity extends BaseActivity {

    @InjectView(R.id.pager) ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        UIUtils.makeStatusBarTransparent(getWindow());
        UIUtils.setStatusBarColor(getWindow(), Color.TRANSPARENT);
        mPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));
    }

    private static class MainPagerAdapter extends FragmentPagerAdapter {

        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return MainFragment.newInstance();
                case 1: return AppListFragment.newInstance();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mPager != null) {
            if (mPager.getCurrentItem() == 1) {
                mPager.setCurrentItem(0);
            }
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }
}
