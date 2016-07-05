package audio.lisn.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.HashMap;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Log;
import audio.lisn.webservice.JsonUTF8StringRequest;

public class FeedBackActivity extends AppCompatActivity {

    EditText subject,description;
    ConnectionDetector connectionDetector;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_back);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.action_feedback);

        connectionDetector = new ConnectionDetector(getApplicationContext());

        progressDialog = new ProgressDialog(FeedBackActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Publishing...");

        subject=(EditText)findViewById(R.id.subject);
        description=(EditText)findViewById(R.id.description);
        Button submitButton = (Button) findViewById(R.id.btnSubmit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitFeedBack();
            }
        });



    }

    private void submitFeedBack() {
        String title=subject.getText().toString();
        String message=description.getText().toString();
        if (connectionDetector.isConnectingToInternet()) {
            if(title.length() > 0 && message.length()>0) {

                progressDialog.setMessage("Publishing...");
                progressDialog.show();

                Map<String, String> params = new HashMap<String, String>();
                params.put("userid", AppController.getInstance().getUserId());
                params.put("title", title);
                params.put("message", message);

                String url = getResources().getString(R.string.user_feedback_url);

                JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                progressDialog.dismiss();

                                Log.v("response", "response:" + response);
                                Toast toast = Toast.makeText(getApplicationContext(), R.string.FEEDBACK_PUBLISH_SUCCESS, Toast.LENGTH_LONG);
                                toast.show();


                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();

                        Toast toast = Toast.makeText(getApplicationContext(), "Feedback publish failed try again later", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });

                AppController.getInstance().addToRequestQueue(stringRequest, "tag_user_feedback");
            }else{
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_valid_data), Toast.LENGTH_SHORT);
                toast.show();
            }
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_INTERNET_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
