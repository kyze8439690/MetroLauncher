package me.yugy.metrolauncher.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.InjectView;
import me.yugy.app.common.core.BaseFragment;
import me.yugy.metrolauncher.adapter.MetroAdapter;
import me.yugy.metrolauncher.widget.MetroView;
import me.yugy.metrolauncher.R;

public class MainFragment extends BaseFragment {

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @InjectView(R.id.metro_view)
    MetroView mMetroView;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_main;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMetroView.setAdapter(new MetroAdapter() {
            @Override
            public int getSize(int position) {
                switch (position % 11) {
                    case 0:
                        return MetroView.SIZE_SMALL;
                    case 1:
                        return MetroView.SIZE_MIDDLE;
                    case 2:
                        return MetroView.SIZE_SMALL;
                    case 3:
                        return MetroView.SIZE_SMALL;
                    case 4:
                        return MetroView.SIZE_SMALL;
                    case 5:
                        return MetroView.SIZE_MIDDLE;
                    case 6:
                        return MetroView.SIZE_SMALL;
                    case 7:
                        return MetroView.SIZE_SMALL;
                    case 8:
                        return MetroView.SIZE_SMALL;
                    case 9:
                        return MetroView.SIZE_SMALL;
                    case 10:
                        return MetroView.SIZE_BIG;
                }
                return MetroView.SIZE_BIG;
            }

            @Override
            public View getView(LayoutInflater inflater, final int position, @Nullable View convertView, ViewGroup parent) {
                View view = convertView;
                Holder holder;
                if (view == null) {
                    view = inflater.inflate(R.layout.item_metro, parent, false);
                    holder = new Holder(view);
                } else {
                    holder = (Holder) view.getTag();
                }
                holder.text.setText(String.valueOf(position));
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        Toast.makeText(v.getContext(), "" + position, Toast.LENGTH_SHORT).show();
                        mMetroView.animateOpen(v);
                    }
                });
                view.setVisibility(View.VISIBLE);
                return view;
            }

            @Override
            public int getCount() {
                return 150;
            }

            class Holder {
                TextView text;

                public Holder(View view) {
                    text = (TextView) view.findViewById(R.id.text);
                    view.setTag(this);
                }
            }
        });
    }
}
