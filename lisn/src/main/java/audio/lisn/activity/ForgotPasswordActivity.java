package audio.lisn.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.HashMap;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.Log;
import audio.lisn.webservice.JsonUTF8StringRequest;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText _emailText;
    Button _resetButton;
    ConnectionDetector connectionDetector;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Forgot Password");

        connectionDetector = new ConnectionDetector(getApplicationContext());

        _emailText = (EditText) findViewById(R.id.input_email);
        _resetButton = (Button) findViewById(R.id.btn_reset);


        _resetButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                reset();
            }
        });
        progressDialog = new ProgressDialog(ForgotPasswordActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Processing...");
    }

    private void reset() {
        if (!validate()) {
            return;
        }
        if(connectionDetector.isConnectingToInternet()){
            _resetButton.setEnabled(false);
            progressDialog.show();
            String url = getString(R.string.forget_password_url);
            String email = _emailText.getText().toString();
            Map<String, String> postParam = new HashMap<String, String>();

            try {
                postParam.put("email", email);
                postParam.put("usertype", "email");

            } catch (Exception e) {
                e.printStackTrace();
            }
            // Map<String,String> postParam = new HashMap<String, String>();

            JsonUTF8StringRequest userAddReq = new JsonUTF8StringRequest(Request.Method.POST, url, postParam,
                    new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            Log.v("response", "response :" + response);

                            //SUCCESS: UID=5
                            Log.v("response", "respondString :" + response);

                            String[] separated = response.split(":");
                            String status=separated[0].trim();
                            if (status.equalsIgnoreCase(Constants.USER_SUCCESS)) {

                                if (separated[1] != null) {
                                    onRequestSuccess();
                                    // loginSuccess(uid, finalUserName);

                                } else {
                                    userAddedError(Constants.USER_SERVER_ERROR);

                                }

                            }

                            else  {
                                userAddedError(status);
                            }
                            Log.v("response", "response :" + response);

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v("response", "error :" + error.getMessage());
                    NetworkResponse response = error.networkResponse;
                    if (response != null) {
                        Log.v("response", response.statusCode + " data: " + response.data.toString());
                    }

                    // sendMail("Error Message: statusCode: "+response.statusCode+" data: "+ response.data.toString());

                    userAddedError(Constants.USER_SERVER_ERROR);
                }
            });
            userAddReq.setShouldCache(false);
            AppController.getInstance().addToRequestQueue(userAddReq, "tag_user_add");

        }else{
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
                    this);
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(R.string.NO_INTERNET_MESSAGE).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    private  void userAddedError(String status){
        progressDialog.dismiss();
        String error_title=getString(R.string.SERVER_ERROR_TITLE);
        String error_message=getString(R.string.SERVER_ERROR_MESSAGE);

        switch (status){
            case Constants.USER_INVALID_USER:{
                error_title=getString(R.string.USER_INVALID_TITLE);
                error_message=getString(R.string.USER_INVALID_MESSAGE);
            }
                break;
            case Constants.USER_PENDING:{
                error_title=getString(R.string.USER_PENDING_TITLE);
                error_message=getString(R.string.USER_PENDING_MESSAGE);
            }
            break;
            case Constants.USER_NOT_EMAIL_USER:{
                error_title=getString(R.string.USER_NOT_EMAIL_TITLE);
                error_message=getString(R.string.USER_NOT_EMAIL_MESSAGE);
            }
            break;
            case Constants.USER_FAILED:{
                error_title=getString(R.string.USER_FAILED_TITLE);
                error_message=getString(R.string.USER_FAILED_MESSAGE);
            }
            break;
            default:
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
                this);
        builder.setTitle(error_title).setMessage(error_message).setPositiveButton(
                getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

    }
    public void onRequestSuccess() {
        Toast.makeText(getBaseContext(), R.string.FORGET_PASSWORD_SUCCESS, Toast.LENGTH_LONG).show();
finish();
    }
    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();



        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("Enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }



        return valid;
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
