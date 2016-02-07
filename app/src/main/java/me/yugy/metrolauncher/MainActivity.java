package me.yugy.metrolauncher;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.animation.AnimatorCompatHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.widget.TextView;
import android.widget.Toast;

import me.yugy.app.common.utils.DebugUtils;

public class MainActivity extends Activity {

    private MetroView mMetroView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DebugUtils.setLogEnable(BuildConfig.DEBUG);
        mMetroView = (MetroView) findViewById(R.id.metro_view);
        mMetroView.setAdapter(new MetroAdapter() {
            @Override
            public int getSize(int position) {
                switch (position % 11) {
                    case 0: return MetroView.SIZE_SMALL;
                    case 1: return MetroView.SIZE_MIDDLE;
                    case 2: return MetroView.SIZE_SMALL;
                    case 3: return MetroView.SIZE_SMALL;
                    case 4: return MetroView.SIZE_SMALL;
                    case 5: return MetroView.SIZE_MIDDLE;
                    case 6: return MetroView.SIZE_SMALL;
                    case 7: return MetroView.SIZE_SMALL;
                    case 8: return MetroView.SIZE_SMALL;
                    case 9: return MetroView.SIZE_SMALL;
                    case 10: return MetroView.SIZE_BIG;
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
