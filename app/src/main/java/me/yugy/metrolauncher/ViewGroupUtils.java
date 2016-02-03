package me.yugy.metrolauncher;

import android.view.View;
import android.view.ViewGroup;

public class ViewGroupUtils {

    public static void offsetChildrenTopAndBottom(ViewGroup viewGroup, int offset) {
        final int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            final View v = viewGroup.getChildAt(i);
            v.offsetTopAndBottom(offset);
        }
    }
}
