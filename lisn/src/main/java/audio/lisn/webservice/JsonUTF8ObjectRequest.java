package audio.lisn.webservice;

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import audio.lisn.util.AppUtils;
import audio.lisn.util.Log;

/**
 * Created by Rasika on 7/4/15.
 */
public class JsonUTF8ObjectRequest extends Request<JSONObject> {
    private Response.Listener<JSONObject> listener;
    private Map<String, String> params;


    public JsonUTF8ObjectRequest(String url, Map<String, String> params,
                                 Response.Listener<JSONObject> reponseListener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.listener = reponseListener;
        this.params = params;
    }

    public JsonUTF8ObjectRequest(int method, String url, Map<String, String> params,
                                 Response.Listener<JSONObject> reponseListener, Response.ErrorListener errorListener) {
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
        Log.v("params", "params:" + params);
        return params;
    };

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    @Override
    protected void deliverResponse(JSONObject jsonObject) {
        listener.onResponse(jsonObject);

    }
}
