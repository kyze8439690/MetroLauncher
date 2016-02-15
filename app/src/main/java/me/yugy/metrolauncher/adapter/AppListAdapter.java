package me.yugy.metrolauncher.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.yugy.app.common.view.BaseHolder;
import me.yugy.app.common.view.OnViewClickListener;
import me.yugy.metrolauncher.R;
import me.yugy.metrolauncher.model.AppInfo;

public class AppListAdapter extends BaseAdapter {

    private List<AppInfo> mData;

    public AppListAdapter(List<AppInfo> data) {
        mData = data;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item = convertView;
        Holder holder;
        if (item == null) {
            item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_list, parent, false);
            holder = new Holder(item);
        } else {
            holder = (Holder) item.getTag();
        }
        holder.parse(getItem(position));
        return item;
    }

    class Holder extends BaseHolder<AppInfo> {

        @InjectView(R.id.label) TextView label;
        @InjectView(R.id.icon) ImageView icon;

        public Holder(View view) {
            super(view);
        }

        @Override
        public void parse(final AppInfo appInfo) {
            icon.setImageDrawable(appInfo.icon);
            label.setText(appInfo.label);
            rootView.setOnClickListener(new OnViewClickListener() {
                @Override
                protected void onViewClick(View view) {
                    view.getContext().startActivity(appInfo.intent);
                }
            });
        }

    }
}
