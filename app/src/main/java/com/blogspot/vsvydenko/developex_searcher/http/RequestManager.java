package com.blogspot.vsvydenko.developex_searcher.http;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.blogspot.vsvydenko.developex_searcher.ui.SearchFragment;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vsvydenko on 07.09.14.
 */
public class RequestManager {

    private static RequestManager instance;

    private RequestController mRequestController;

    private RequestManager(Context context) {
        Context applicationContext = context.getApplicationContext();
        mRequestController = new RequestController(applicationContext);
    }

    public static synchronized void initializeWith(Context context) {
        if (instance == null) {
            instance = new RequestManager(context);
        }
    }

    public static synchronized QueueBuilder queue() {
        if (instance == null) {
            throw new IllegalStateException(RequestManager.class.getSimpleName() +
                    " is not initialized, call initializeWith(..) method first.");
        }
        return instance.getRequestController().mQueueBuilder;
    }

    private RequestController getRequestController() {
        return mRequestController;
    }

    public class RequestController {

        private QueueBuilder mQueueBuilder;

        public RequestController(Context context) {
            mQueueBuilder = new QueueBuilder(context);
        }

        public RequestController addRequest(RequestInterface volleyRequest) {
            mQueueBuilder.getRequestQueue().add(volleyRequest.create());
            return this;
        }

        public void start() {
            mQueueBuilder.getRequestQueue().start();
        }

        public void stop() {
            if (mQueueBuilder.getRequestQueue() != null) {
                mQueueBuilder.getRequestQueue().stop();
            }
        }

        public void cancelAll(Object tag) {
            mQueueBuilder.getRequestQueue().cancelAll(tag);
        }

        public void cancelAll(RequestQueue.RequestFilter requestFilter) {
            mQueueBuilder.getRequestQueue().cancelAll(requestFilter);
        }
    }

    public class QueueBuilder {

        private Context mContext;

        private Map<String, RequestQueue> mRequestQueue = new HashMap<String, RequestQueue>();

        private String mCurQueue;

        public QueueBuilder(Context context) {
            mContext = context;
        }

        public RequestController use(String queueName) {
            mCurQueue = queueName;
            return mRequestController;
        }

        public void create(String queueName, RequestQueue requestQueue) {
            if (mRequestQueue.containsKey(queueName)) {
                throw new IllegalArgumentException(
                        "RequestQueue - \"" + queueName + "\" already exists!");
            }
            mRequestQueue.put(queueName, requestQueue);
        }

        public void removeMainQueue(){
            if (mRequestQueue.containsKey(SearchFragment.MAIN_QUEUE)) {
                mRequestQueue.remove(SearchFragment.MAIN_QUEUE);
            }
        }

        private RequestQueue getRequestQueue() {
            RequestQueue result = mRequestQueue.get(mCurQueue);
            return result;
        }
    }
}
