package audio.lisn.util;

import android.location.Location;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.HashMap;
import java.util.Map;

import audio.lisn.app.AppController;
import audio.lisn.webservice.JsonUTF8StringRequest;

public class Analytic {
    public static final String TAG = Analytic.class.getSimpleName();

    public void analyticEvent(int activityId,String bookId, String info){

        //http://app.lisn.audio/api/1.4/analytics.php?sessionid=123&amp;bookid=1&amp;userid=1&amp;activityid=1&amp;lon=2.123

       // &amp;lat=80.1234&amp;info=Test

        Map<String, String> params = new HashMap<String, String>();

        if(AppController.getInstance().isUserLogin())
        params.put("userid", AppController.getInstance().getUserId());
        params.put("sessionid", AppController.getInstance().getSessionId());
        if(bookId.length()>0)
        params.put("bookid", bookId);
        params.put("activityid", ""+activityId);
        if(info.length()>0)
            params.put("info",info);
        Location location=AppController.getInstance().getLastLocation();

        if(location != null){
            params.put("lon", String.valueOf(location.getLongitude()));
            params.put("lat", String.valueOf(location.getLatitude()));

        }

        Log.v(TAG,""+params);
        String url = "http://app.lisn.audio/api/1.4/analytics.php";

        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url,params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v(TAG,""+response);


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v(TAG,""+error);


            }
        });


        AppController.getInstance().addToRequestQueue(stringRequest, "tag_analytic");

    }
}
