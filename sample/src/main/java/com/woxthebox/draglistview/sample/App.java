package com.woxthebox.draglistview.sample;

import android.app.Application;

/**
 * Created by FRAMGIA\nguyen.viet.manh on 2/28/18.
 */

public class App extends Application {
    private static App instance;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
