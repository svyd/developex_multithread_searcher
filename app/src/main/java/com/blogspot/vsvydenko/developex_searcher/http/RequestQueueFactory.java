package com.blogspot.vsvydenko.developex_searcher.http;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import java.io.File;

/**
 * Created by vsvydenko on 07.09.14.
 */
public class RequestQueueFactory {

    public static RequestQueue getDefault(Context context, int threadPoolSize) {
        return newRequestQueue(context.getApplicationContext(), null, threadPoolSize);
    }

    // copied from Volley.newRequestQueue(..); source code
    public static RequestQueue newRequestQueue(Context context, HttpStack stack,
            int threadPoolSize) {
        File cacheDir = new File(context.getCacheDir(), "def_cahce_dir");

        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new OkHttpStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }
        }

        Network network = new BasicNetwork(stack);
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network,
                threadPoolSize);

        return queue;
    }

}
