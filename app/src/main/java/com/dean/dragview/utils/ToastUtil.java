package com.dean.dragview.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Administrator on 2016/3/3.
 */
public class ToastUtil {
    private static Toast mToast;

    public static void show(Context context,String text){
        if(mToast==null)
            mToast=Toast.makeText(context,text,Toast.LENGTH_SHORT);
        mToast.setText(text);
        mToast.show();
    }
}
