package com.vicent.pushnotification;

import android.app.Application;
import android.content.Context;

/**
 * Created by LeeVicent on 2018/1/28.
 */

public class MyApplication extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
