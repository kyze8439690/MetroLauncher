package me.yugy.metrolauncher.adapter;

import android.content.ActivityNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.yugy.app.common.view.BaseHolder;
import me.yugy.app.common.view.OnViewClickListener;
import me.yugy.metrolauncher.R;
import me.yugy.metrolauncher.model.AppInfo;
import me.yugy.metrolauncher.view.TiltEffectAttacher;

public class AppListAdapter extends BaseAdapter {

    private List<AppInfo> mData;

    public AppListAdapter() {
        mData = new ArrayList<>();
    }

    public void setData(List<AppInfo> data) {
        mData.clear();
        if (data != null) {
            mData.addAll(data);
        }
        notifyDataSetChanged();
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
            TiltEffectAttacher.attach(rootView);
        }

        @Override
        public void parse(final AppInfo appInfo) {
            icon.setImageDrawable(appInfo.icon);
            label.setText(appInfo.label);
            rootView.setOnClickListener(new OnViewClickListener() {
                @Override
                protected void onViewClick(View view) {
                    try {
                        view.getContext().startActivity(appInfo.intent);
                    } catch (ActivityNotFoundException | SecurityException e) {
                        Toast.makeText(view.getContext(), "Activity not found.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }
}
