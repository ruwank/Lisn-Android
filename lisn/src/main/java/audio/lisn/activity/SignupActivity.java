package audio.lisn.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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


public class SignupActivity extends AppCompatActivity {

    EditText _firstNameText;
    EditText _lastNameText;
    EditText _emailText;
    EditText _passwordText;
    EditText _confPasswordText;
    Button _signupButton;
    TextView _loginLink;
    ConnectionDetector connectionDetector;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Sign up");

        connectionDetector = new ConnectionDetector(getApplicationContext());

        _firstNameText = (EditText) findViewById(R.id.input_first_name);
        _lastNameText = (EditText) findViewById(R.id.input_last_name);
        _emailText = (EditText) findViewById(R.id.input_email);
        _passwordText = (EditText) findViewById(R.id.input_password);
        _confPasswordText = (EditText) findViewById(R.id.input_conf_password);
        _signupButton = (Button) findViewById(R.id.btn_signup);
        _loginLink = (TextView) findViewById(R.id.link_login);

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                finish();
            }
        });
        _signupButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                signup();
            }
        });

        progressDialog = new ProgressDialog(SignupActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Signing up...");
    }

    private void signup() {
        if (!validate()) {
            return;
        }
        if(connectionDetector.isConnectingToInternet()){
            _signupButton.setEnabled(false);
            progressDialog.show();
            String url = getString(R.string.add_user_url);
            String fname = _firstNameText.getText().toString();
            String lname = _lastNameText.getText().toString();

            String email = _emailText.getText().toString();
            String password = _passwordText.getText().toString();
            Map<String, String> postParam = new HashMap<String, String>();

            try {
                String android_id = getUniqueID();
                postParam.put("fname", fname);
                postParam.put("lname", lname);
                postParam.put("email", email);
                postParam.put("password", password);
                postParam.put("usertype", "email");
                postParam.put("device", android_id);
                postParam.put("os", "android");

                postParam.put("username", "NULL");
                postParam.put("fbname", "NULL");
                postParam.put("loc", "NULL");
                postParam.put("bday", "NULL");
                postParam.put("mobile", "NULL");
                postParam.put("age", "NULL");
                postParam.put("pref", "NULL");
                postParam.put("fbid", "NULL");
                postParam.put("mname", "NULL");
                postParam.put("fburl", "NULL");



            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Log.v("response", "postParam :" + postParam);
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
                            if (status.equalsIgnoreCase(Constants.USER_SUCCESS) || status.equalsIgnoreCase(Constants.USER_EXIST)) {


                                if (separated[1] != null) {
                                    String uid = "0";
                                    String userName = "";
                                    String type = "";
                                    String loginStatus = "";
                                    String[] separated2 = separated[1].split("=");
                                    if (separated2[1] != null) {
                                        uid = separated2[1].trim();
                                    }

                                    if (separated[2] != null) {
                                        String[] separated3 = separated[2].split("=");
                                        if (separated3[1] != null) {
                                            userName = separated3[1].trim();
                                        }
                                    }
                                    if (separated[3] != null) {
                                        String[] separated4 = separated[3].split("=");
                                        if (separated4[1] != null) {
                                            loginStatus = separated4[1].trim();
                                        }
                                    }
                                    if (separated[4] != null) {
                                        String[] separated5 = separated[4].split("=");
                                        if (separated5[1] != null) {
                                            type = separated5[1].trim();
                                        }
                                    }

                                    userAddedSuccess(status,uid, userName,type,loginStatus);

                                    Log.v("response", "uid :" + uid +"userName :" + userName +"loginStatus :" + loginStatus+"type :" + type);
                                } else {
                                    userAddedError(Constants.USER_SERVER_ERROR);

                                }

                            } else {
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
            _signupButton.setEnabled(true);
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
    private  void userAddedSuccess(String status,String uid, String userName,String type,String loginStatus){
        _signupButton.setEnabled(true);
        progressDialog.dismiss();
        String error_title=getString(R.string.SERVER_ERROR_TITLE);
        String error_message=getString(R.string.SERVER_ERROR_MESSAGE);

        switch (status){
            case Constants.USER_SUCCESS:{
                error_title=getString(R.string.USER_EMAIL_SIGNUP_SUCCESS_TITLE);
                error_message=getString(R.string.USER_EMAIL_SIGNUP_SUCCESS_MESSAGE);
            }
            break;
            case Constants.USER_EXIST:{
                if(type.equalsIgnoreCase("fb")){
                    error_title=getString(R.string.USER_EXIST_TITLE);
                    error_message=getString(R.string.USER_EXIST_MESSAGE);
                }else{
                    if(loginStatus.equalsIgnoreCase("pending")){
                        error_title=getString(R.string.USER_EMAIL_SIGNUP_SUCCESS_TITLE);
                        error_message=getString(R.string.USER_EMAIL_SIGNUP_SUCCESS_MESSAGE);
                    }else {
                        String email = _emailText.getText().toString();
                        error_title = getString(R.string.USER_DUPLICATE_TITLE);
                        error_message = String.format(getString(R.string.USER_DUPLICATE_MESSAGE), email);
                    }
                }

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
                       finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();


    }
    private String getUniqueID(){
        String myAndroidDeviceId = "";
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            myAndroidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        }else {
            TelephonyManager mTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephony.getDeviceId() != null) {
                myAndroidDeviceId = mTelephony.getDeviceId();
            } else {
                myAndroidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }
        return myAndroidDeviceId;
    }

    public boolean validate() {
        boolean valid = true;

        String name = _firstNameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        String confPassword = _confPasswordText.getText().toString();

        if (name.isEmpty()) {
            _firstNameText.setError("Enter valid name");
            valid = false;
        } else {
            _firstNameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("Enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 ) {
            _passwordText.setError("must be more than 4 characters");
            valid = false;
        } else {
                if(!password.equals(confPassword)){

                    _confPasswordText.setError("Password confirmation doesn't match Password");
                valid = false;
            }
            _passwordText.setError(null);
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
