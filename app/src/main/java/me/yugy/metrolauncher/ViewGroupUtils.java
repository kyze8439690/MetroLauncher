package me.yugy.metrolauncher;

import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ViewGroupUtils {

    public static void offsetChildrenTopAndBottom(ViewGroup viewGroup, int offset) {
        if (!offsetChildrenTopAndBottomReflect(viewGroup, offset)) {
            final int count = viewGroup.getChildCount();
            for (int i = 0; i < count; i++) {
                final View v = viewGroup.getChildAt(i);
                v.offsetTopAndBottom(offset);
            }
        }
    }

    private static boolean offsetChildrenTopAndBottomReflect(ViewGroup viewGroup, int offset) {
        try {
            Method method = ViewGroup.class.getMethod("offsetChildrenTopAndBottom", int.class);
            method.invoke(viewGroup, offset);
            return true;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }
}
