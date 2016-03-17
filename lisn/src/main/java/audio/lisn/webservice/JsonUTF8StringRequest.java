package audio.lisn.webservice;


import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import audio.lisn.util.AppUtils;
import audio.lisn.util.Log;

/**
 * Created by Rasika on 7/4/15.
 */
public class JsonUTF8StringRequest extends Request<String> {
    private Response.Listener<String> listener;
    private Map<String, String> params;
    private String url;
    boolean mobilePayment;

    public JsonUTF8StringRequest(int method, String url, Response.ErrorListener listener) {
        super(method, url, listener);
    }
    public JsonUTF8StringRequest(String url, Map<String, String> params,
                                 Response.Listener<String> reponseListener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.listener = reponseListener;
        this.params = params;
        this.url=url;
    }

    public JsonUTF8StringRequest(int method, String url, Map<String, String> params,
                                 Response.Listener<String> reponseListener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = reponseListener;
        this.params = params;
        this.url=url;
//        RetryPolicy mRetryPolicy = new DefaultRetryPolicy(
//
//                0,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
//        this.setRetryPolicy(mRetryPolicy);


        this.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS*10,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }
    public JsonUTF8StringRequest(int method, String url, Map<String, String> params,boolean mobilePayment,
                                 Response.Listener<String> reponseListener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = reponseListener;
        this.params = params;
        this.url=url;
        this.mobilePayment=mobilePayment;


        this.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS*10,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    protected Map<String, String> getParams()
            throws com.android.volley.AuthFailureError {
        Log.v("params", "params:" + params);
        return params;
    };

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<String, String>();
        String credentials = AppUtils.getCredentialsData();

        if(mobilePayment){
            credentials="app:je#82@m*1x";
        }
        String auth = "Basic "
                + Base64.encodeToString(credentials.getBytes(),
                Base64.NO_WRAP);
        headers.put("Authorization", auth);
        return headers;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            return Response.success(new String(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (Exception je) {
            return Response.error(new ParseError(je));
        }
    }

    @Override
    protected void deliverResponse(String jsonObject) {
        listener.onResponse(jsonObject);

    }
}
