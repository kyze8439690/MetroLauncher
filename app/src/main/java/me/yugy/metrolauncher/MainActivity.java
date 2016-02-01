package me.yugy.metrolauncher;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends Activity {

    private MetroView mMetroView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMetroView = (MetroView) findViewById(R.id.metro_view);
        mMetroView.setAdapter(new MetroAdapter() {
            @Override
            public int getSize(int position) {
                switch (position % 3) {
                    case 0: return MetroView.SIZE_SMALL;
                    case 1: return MetroView.SIZE_MIDDLE;
                    case 2: return MetroView.SIZE_BIG;
                }
                return MetroView.SIZE_BIG;
            }

            @Override
            public View getView(LayoutInflater inflater, int position, @Nullable View convertView, ViewGroup parent) {
                View item = convertView;
                Holder holder;
                if (item == null) {
                    item = inflater.inflate(R.layout.item_metro, parent, false);
                    holder = new Holder(item);
                } else {
                    holder = (Holder) item.getTag();
                }
                holder.text.setText(String.valueOf(position));
                return item;
            }

            @Override
            public int getCount() {
                return 10;
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
