package com.blogspot.vsvydenko.developex_searcher.http;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;


/**
 * Created by vsvydenko on 07.09.14.
 */
public class UrlRequest extends RequestInterface {

    private Response.Listener<String> mResponseListener;
    private Response.ErrorListener mErrorListenerer;
    private String requestUrl;

    public UrlRequest(String requestUrl, Response.Listener<String> responseListener,
            Response.ErrorListener errorListener) {

        this.requestUrl = requestUrl;
        mResponseListener = responseListener;
        mErrorListenerer = errorListener;
    }

    @Override
    public Request create() {

        Request request = new StringRequest(
                Request.Method.GET,
                requestUrl,
                mResponseListener,
                mErrorListenerer
        );
        return request;
    }
}
