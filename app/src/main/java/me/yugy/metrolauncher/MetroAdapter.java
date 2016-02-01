package me.yugy.metrolauncher;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class MetroAdapter {

    @MetroView.Size
    public abstract int getSize(int position);

    public abstract View getView(LayoutInflater inflater, int position, @Nullable View convertView, ViewGroup parent);

    public abstract int getCount();

}