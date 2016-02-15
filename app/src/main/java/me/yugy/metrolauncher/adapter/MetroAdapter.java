package me.yugy.metrolauncher.adapter;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.yugy.metrolauncher.widget.MetroView;

public abstract class MetroAdapter {

    @MetroView.Size
    public abstract int getSize(int position);

    public abstract View getView(LayoutInflater inflater, int position, @Nullable View convertView, ViewGroup parent);

    public abstract int getCount();

}