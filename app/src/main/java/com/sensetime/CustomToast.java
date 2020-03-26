package com.sensetime;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

public class CustomToast {
    private static Toast toast;

    public static void show(Context context, String msg) {
        if (toast == null) {
            toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        } else {
            View view = toast.getView();
            toast.cancel();
            toast = new Toast(context);
            toast.setView(view);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setText(msg);
        }
        toast.show();
    }
}
