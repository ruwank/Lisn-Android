package audio.lisn.webservice;

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import audio.lisn.util.AppUtils;
import audio.lisn.util.Log;

/**
 * Created by Rasika on 7/3/15.
 */
public class JsonUTF8ArrayRequest extends Request<JSONArray> {
    private Listener<JSONArray> listener;
    private Map<String, String> params;

    public JsonUTF8ArrayRequest(String url, Map<String, String> params,
                                Listener<JSONArray> reponseListener, ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.listener = reponseListener;
        this.params = params;
    }

    public JsonUTF8ArrayRequest(int method, String url, Map<String, String> params,
                                Listener<JSONArray> reponseListener, ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = reponseListener;
        this.params = params;
    }
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<String, String>();
        String credentials = AppUtils.getCredentialsData();
        String auth = "Basic "
                + Base64.encodeToString(credentials.getBytes(),
                Base64.NO_WRAP);
        headers.put("Authorization", auth);
        return headers;
    }

    protected Map<String, String> getParams()
            throws com.android.volley.AuthFailureError {
        Log.v("params","params:"+params);
        return params;
    };

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            return Response.success(new JSONArray(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    @Override
    protected void deliverResponse(JSONArray jsonObject) {
        Log.v("jsonObject","jsonObject"+jsonObject);
        listener.onResponse(jsonObject);

    }
}
