package com.blogspot.vsvydenko.developex_searcher.ui;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.blogspot.vsvydenko.developex_searcher.R;
import com.blogspot.vsvydenko.developex_searcher.entity.UrlItem;
import com.blogspot.vsvydenko.developex_searcher.http.RequestManager;
import com.blogspot.vsvydenko.developex_searcher.http.RequestQueueFactory;
import com.blogspot.vsvydenko.developex_searcher.http.UrlRequest;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vsvydenko on 07.09.14.
 */
public class SearchFragment extends Fragment {

    public static final String MAIN_QUEUE = "MAIN_QUEUE";
    public static final int PROGRESS_MAX = 100;
    public static final String REGEX_PATTERN
            = "(http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    private View returnView;

    private int foundUrlCounter = 0;
    private int scannedUrlCounter = 0;
    private int foundTextCounter = 0;
    private int currentLevel = 1;

    private String mBaseUrl;
    private String mText;
    private int mThreadsNumber = 1;
    private int mMaxValueScannedUrls;
    private ConcurrentHashMap<Integer, LinkedList<String>> urlsHashMap
            = new ConcurrentHashMap<Integer, LinkedList<String>>();

    private EditText mBaseUrlEditText;
    private EditText mThreadNumberText;
    private EditText mSearchTextEditText;
    private EditText mMaxValOfScanUrlEditText;

    private Button mStartButton;
    private Button mStopButton;
    private ProgressBar mProgressBar;

    private TextView mFoundTextView;

    private ListView mStatusListView;

    private UrlAdapter mUrlAdapter;
    private CopyOnWriteArrayList mUrlsList;

    public SearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        returnView = inflater.inflate(R.layout.fragment_search, container, false);

        return returnView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initializeContent();
    }

    private void initializeContent() {

        mBaseUrlEditText = (EditText) returnView.findViewById(R.id.editBaseUrl);
        mThreadNumberText = (EditText) returnView.findViewById(R.id.editThreadsNumber);
        mSearchTextEditText = (EditText) returnView.findViewById(R.id.editSearchText);
        mMaxValOfScanUrlEditText = (EditText) returnView.findViewById(R.id.editScannedUrlsNumber);

        mStartButton = (Button) returnView.findViewById(R.id.btnStart);
        mStopButton = (Button) returnView.findViewById(R.id.btnStop);

        mProgressBar = (ProgressBar) returnView.findViewById(R.id.progress);
        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(PROGRESS_MAX);

        mFoundTextView = (TextView) returnView.findViewById(R.id.txtFound);

        mStatusListView = (ListView) returnView.findViewById(R.id.lstUrlStatus);

        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnStart:
                        startSearch();
                        break;
                    case R.id.btnStop:
                        stopSearch();
                        break;
                }
            }
        };

        mStartButton.setOnClickListener(click);
        mStopButton.setOnClickListener(click);

    }

    private void startSearch() {
        resetData();
        if (TextUtils.isEmpty(mBaseUrlEditText.getText()) ||
                TextUtils.isEmpty(mThreadNumberText.getText()) ||
                TextUtils.isEmpty(mSearchTextEditText.getText()) ||
                TextUtils.isEmpty(mMaxValOfScanUrlEditText.getText())) {
            Toast.makeText(getActivity(), getString(R.string.check_input_data), Toast.LENGTH_LONG)
                    .show();
            return;
        } else {
            setEnabledParameterUIState(false);
            mBaseUrl = mBaseUrlEditText.getText().toString().trim();
            mThreadsNumber = Integer.parseInt(mThreadNumberText.getText().toString());
            mText = mSearchTextEditText.getText().toString().trim();
            mMaxValueScannedUrls = Integer.parseInt(mMaxValOfScanUrlEditText.getText().toString());

            RequestManager.queue().create(MAIN_QUEUE, RequestQueueFactory
                    .newRequestQueue(getActivity().getApplicationContext(), null,
                            mThreadsNumber));
            RequestManager.queue().use(MAIN_QUEUE);
            urlsHashMap.put(currentLevel, new LinkedList<String>());
            urlsHashMap.get(currentLevel).add(mBaseUrl);

            doSearch();
        }
    }

    private void doSearch() {
        LinkedList<String> currentList = urlsHashMap.get(currentLevel);
        int listSize = currentList.size();
        final AtomicInteger taskAlive = new AtomicInteger(listSize);
        for (final String url : currentList) {
            RequestManager.queue().
                    use(MAIN_QUEUE).
                    addRequest(new UrlRequest(url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            scannedUrlCounter++;
                            searchUrls(response);
                            int found = searchText(response);
                            if (mUrlsList == null) {
                                mUrlsList = new CopyOnWriteArrayList<UrlItem>();
                            }
                            mUrlsList.add(new UrlItem(url, getString(R.string.success), found));
                            updateUI();
                            checkIfSearchComplete();
                            boolean levelIsCompleted = taskAlive.decrementAndGet() == 0;
                            checkIfCurrentLevelComplete(levelIsCompleted);

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            scannedUrlCounter++;
                            if (mUrlsList == null) {
                                mUrlsList = new CopyOnWriteArrayList<UrlItem>();
                            }
                            mUrlsList.add(new UrlItem(url,
                                    String.format(getString(R.string.error),
                                            volleyError.toString()),
                                    0));
                            updateUI();
                            checkIfSearchComplete();
                            boolean levelIsCompleted = taskAlive.decrementAndGet() == 0;
                            checkIfCurrentLevelComplete(levelIsCompleted);
                        }
                    })).start();
        }
    }

    private void checkIfSearchComplete() {
        if (scannedUrlCounter > mMaxValueScannedUrls) {
            Toast.makeText(getActivity(), getString(R.string.completed), Toast.LENGTH_LONG)
                    .show();
            stopSearch();
        }
    }

    private void checkIfCurrentLevelComplete(boolean levelIsCompleted) {
        if (levelIsCompleted) {
            currentLevel++;
            if (scannedUrlCounter < mMaxValueScannedUrls) {
                doSearch();
            }
        }
    }

    private int searchUrls(String response) {
        int value = 0;
        int level = currentLevel + 1;
        Pattern p = Pattern.compile(REGEX_PATTERN);
        Matcher matcher = p.matcher(response);
        while (matcher.find()) {
            value++;
            foundUrlCounter++;
            if (urlsHashMap.get(level) == null) {
                urlsHashMap.put(level, new LinkedList<String>());
            }
            urlsHashMap.get(level).add(matcher.group());
        }
        return value;
    }

    private int searchText(String response) {
        int value = 0;
        Pattern p = Pattern.compile(mText);
        Matcher matcher = p.matcher(response);
        while (matcher.find()) {
            value++;
            foundTextCounter++;
        }
        return value;
    }

    private void stopSearch() {
        setEnabledParameterUIState(true);
        RequestManager.queue().use(MAIN_QUEUE).cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        RequestManager.queue().use(MAIN_QUEUE).stop();
        RequestManager.queue().removeMainQueue();
    }

    private void setEnabledParameterUIState(boolean value) {
        mBaseUrlEditText.setEnabled(value);
        mThreadNumberText.setEnabled(value);
        mSearchTextEditText.setEnabled(value);
        mMaxValOfScanUrlEditText.setEnabled(value);
        if (value) {
            mStartButton.setEnabled(true);
            mStopButton.setEnabled(false);
        } else {
            mStartButton.setEnabled(false);
            mStopButton.setEnabled(true);
        }
    }

    private void resetData() {
        scannedUrlCounter = 0;
        mMaxValueScannedUrls = 1;
        mThreadsNumber = 1;
        foundTextCounter = 0;
        foundUrlCounter = 0;
        mText = "";
        mProgressBar.setProgress(0);
        mFoundTextView.setText(String.format(getString(R.string.found), foundTextCounter));
        mUrlAdapter = null;
        if (mUrlsList != null) {
            mUrlsList.clear();
        }
        if (urlsHashMap != null) {
            urlsHashMap.clear();
        }
        if (mStatusListView != null) {
            mStatusListView.setAdapter(null);
        }
    }

    private void updateUI() {
        mProgressBar.setProgress(scannedUrlCounter * PROGRESS_MAX / mMaxValueScannedUrls);
        mFoundTextView.setText(String.format(getString(R.string.found), foundTextCounter));

        if (mUrlAdapter == null) {
            mUrlAdapter = new UrlAdapter(getActivity(), mUrlsList);
        }

        if (mStatusListView.getAdapter() == null) {
            mStatusListView.setAdapter(mUrlAdapter);
        } else {
            mUrlAdapter.notifyDataSetChanged();
        }
    }

}
