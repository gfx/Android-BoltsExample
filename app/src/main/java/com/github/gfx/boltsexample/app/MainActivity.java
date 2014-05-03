package com.github.gfx.boltsexample.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.client.HttpResponseException;

import bolts.Continuation;
import bolts.Task;


public class MainActivity extends Activity {

    private static final String kApiBase = "http://www.wdyl.com/profanity"; // an example of Web API

    private final AsyncHttpClient client = new AsyncHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final TextView textView = (TextView) findViewById(R.id.body);
        textView.setText("");

        nestedRequests();
    }

    private void nestedRequests() {
        final TextView textView = (TextView) findViewById(R.id.body);

        textView.append("nested\n");

        final long t0 = System.currentTimeMillis();
        client.get(kApiBase, new RequestParams("q", "apple"), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] content) {
                assert statusCode == 200; // skip error handling

                textView.append("apple : " + new String(content) + "\n");

                client.get(kApiBase, new RequestParams("q", "banana"), new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] content) {
                        assert statusCode == 200; // skip error handling

                        textView.append("banana : " + new String(content) + "\n");

                        client.get(kApiBase, new RequestParams("q", "beef"), new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] content) {
                                assert statusCode == 200; // skip error handling

                                textView.append("beef : " + new String(content) + "\n");

                                client.get(kApiBase, new RequestParams("q", "xxx"), new AsyncHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, byte[] content) {
                                        assert statusCode == 200; // skip error handling

                                        textView.append("xxx : " + new String(content) + "\n");
                                        textView.append("elapsed (nested) : " + (System.currentTimeMillis() - t0) + "ms\n");

                                        waterfallRequests();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }


    private Task<String> getApiAsync(String word) {
        final Task<String>.TaskCompletionSource taskSource = Task.create();

        RequestParams params = new RequestParams();
        params.put("q", word);

        client.get(this, kApiBase, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String s = new String(responseBody);
                if (statusCode == 200) {
                    taskSource.setResult(s);
                } else {
                    taskSource.setError(new HttpResponseException(statusCode, s));
                }
            }
        });

        return taskSource.getTask();
    }

    private void waterfallRequests() {
        final TextView textView = (TextView) findViewById(R.id.body);
        textView.append("waterfall\n");

        final long t0 = System.currentTimeMillis();
        getApiAsync("apple").continueWithTask(new Continuation<String, Task<String>>() {
            @Override
            public Task<String> then(Task<String> task) throws Exception {
                textView.append("apple : " + task.getResult() + "\n");

                return getApiAsync("banana");
            }
        }).continueWithTask(new Continuation<String, Task<String>>() {
            @Override
            public Task<String> then(Task<String> task) throws Exception {
                textView.append("banana : " + task.getResult() + "\n");

                return getApiAsync("beef");
            }
        }).continueWithTask(new Continuation<String, Task<String>>() {
            @Override
            public Task<String> then(Task<String> task) throws Exception {
                textView.append("beef : " + task.getResult() + "\n");

                return getApiAsync("xxx");
            }
        }).continueWith(new Continuation<String, Void>() {
            @Override
            public Void then(Task<String> task) throws Exception {
                textView.append("xxx : " + task.getResult() + "\n");

                textView.append("elapsed (serial): " + (System.currentTimeMillis() - t0) + "ms\n");

                return null;
            }
        });
    }
}
