package com.blogspot.vsvydenko.developex_searcher;

import com.blogspot.vsvydenko.developex_searcher.http.RequestManager;

import android.app.Application;

/**
 * Created by vsvydenko on 07.09.14.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        RequestManager.initializeWith(getApplicationContext());
    }

}
