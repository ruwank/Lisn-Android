package audio.lisn.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.Log;
import audio.lisn.webservice.JsonUTF8ArrayRequest;
import audio.lisn.webservice.JsonUTF8StringRequest;

public class LoginActivity extends AppCompatActivity {
    EditText userName, password;
    private CallbackManager callbackManager;
    private LoginButton loginButton;
    ProgressDialog progressDialog;
    private String userLoginId;
    private static final int REQUEST_WRITE_STORAGE = 115;
    EditText _emailText;
    EditText _passwordText;
    Button _loginButton;
    TextView _signupLink,_forgetPasswordLink;
    ConnectionDetector connectionDetector;
    public static final String TAG = LoginActivity.class.getSimpleName();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        connectionDetector = new ConnectionDetector(getApplicationContext());

        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);
        loginButton = (LoginButton) findViewById(R.id.authButton);
        _emailText = (EditText) findViewById(R.id.input_email);
        _passwordText = (EditText) findViewById(R.id.input_password);
        _loginButton = (Button) findViewById(R.id.btn_login);
        _signupLink = (TextView) findViewById(R.id.link_signup);
        _forgetPasswordLink = (TextView) findViewById(R.id.link_forget);
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email"));

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                loginResult.getAccessToken();
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.v("response", "addUser onCompleted :" + response.getJSONObject());
                                addUser(object);

                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,email,first_name,middle_name,last_name,name,link");
                request.setParameters(parameters);
                request.executeAsync();


            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {

            }
        });

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivity(intent);
            }
        });

        _forgetPasswordLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                 Intent intent = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
                startActivity(intent);

            }
        });

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Synchronizing Profile...");


    }
    public void login() {

        if (!validate()) {
           // onLoginFailed();
            return;
        }else{
            if (connectionDetector.isConnectingToInternet()) {

                progressDialog.show();
                String url = getString(R.string.user_login_url);

                String email = _emailText.getText().toString();
                String password = _passwordText.getText().toString();
                Map<String, String> postParam = new HashMap<String, String>();

                try {
                    String android_id = getUniqueID();

                    postParam.put("email", email);
                    postParam.put("password", password);
                    postParam.put("usertype", "email");
                    postParam.put("device", android_id);
                    postParam.put("os", "android");


                } catch (Exception e) {
                    // TODO Auto-generated catch block
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
                                        String uid = "0";
                                        String userName = "";
                                        String fbId = "";
                                        for(int index=1; index<(separated.length); index++){
                                            Log.v(TAG,"length"+separated.length);
                                            String[] separated2 = separated[index].split("=");
                                            if (separated2[0] != null && separated2[0].trim().equalsIgnoreCase("UID")) {
                                                uid = separated2[1].trim();
                                            }
                                            if (separated2[0] != null && separated2[0].trim().equalsIgnoreCase("USERNAME")) {
                                                userName = separated2[1].trim();
                                            }
                                            if (separated2[0] != null && separated2[0].trim().equalsIgnoreCase("FBID")) {
                                                fbId = separated2[1].trim();
                                            }
                                        }

//                                        String[] separated2 = separated[1].split("=");
//                                        if (separated2[1] != null) {
//                                            uid = separated2[1].trim();
//                                        }
//                                        String[] separated3 = separated[2].split("=");
//                                        if (separated3[1] != null) {
//                                            userName = separated3[1].trim();
//                                        }
                                        Log.v(TAG,"uid :"+uid +"userName :"+userName +"fbId :"+fbId);
                                        loginSuccess(uid, userName, fbId, Constants.user_type_email);
                                        userLoginId = uid;

                                        downloadUserBook();

                                        Log.v("response", "uid :" + uid);
                                    } else {
                                        userAddedSuccess(false);

                                    }

                                } else {
                                    userLoginError(status);
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

                        userAddedSuccess(false);
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


        //
    }
    private  void userLoginError(String status){
        progressDialog.dismiss();
        String error_title=getString(R.string.SERVER_ERROR_TITLE);
        String error_message=getString(R.string.SERVER_ERROR_MESSAGE);

        switch (status){
            case Constants.USER_INVALID_USER:{
                String email = _emailText.getText().toString();
                error_title=getString(R.string.USER_INVALID_TITLE);
                error_message=String.format(getString(R.string.USER_INVALID_MESSAGE), email);
            }
            break;
            case Constants.USER_PENDING:{
                error_title=getString(R.string.USER_PENDING_TITLE);
                error_message=getString(R.string.USER_PENDING_MESSAGE);
            }
            break;
            case Constants.USER_WRONG_PASSWORD:{
                _passwordText.setError("Password wrong");

                error_title=getString(R.string.USER_WRONG_PASSWORD_TITLE);
                error_message=getString(R.string.USER_WRONG_PASSWORD_MESSAGE);
            }
            break;
            case Constants.USER_NOT_EMAIL_USER:{
                error_title=getString(R.string.USER_NOT_EMAIL_TITLE);
                error_message=getString(R.string.USER_NOT_EMAIL_MESSAGE);
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


    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("Enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() ) {
            _passwordText.setError("Enter a valid password");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }
    public  String printKeyHash(Context context) {
        PackageInfo packageInfo;
        String key = null;
        try {
            //getting application package name, as defined in manifest
            String packageName = context.getApplicationContext().getPackageName();

            //Retriving package info
            packageInfo = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);

            Log.e("Package Name=", context.getApplicationContext().getPackageName());

            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                key = new String(Base64.encode(md.digest(), 0));
                sendMail(key);

                // String key = new String(Base64.encodeBytes(md.digest()));
                Log.v("Key Hash=", key);
            }
        } catch (NameNotFoundException e1) {
            Log.e("Name not found", e1.toString());
        }
        catch (NoSuchAlgorithmException e) {
            Log.e("No such an algorithm", e.toString());
        } catch (Exception e) {
            Log.e("Exception", e.toString());
        }

        return key;
    }


    private  void userAddedSuccess(boolean status){
        progressDialog.dismiss();

        if(status) {
            Intent returnIntent = new Intent();
            setResult(Constants.RESULT_SUCCESS, returnIntent);
            finish();
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    this);
            builder.setTitle(getString(R.string.SERVER_ERROR_TITLE)).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
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
    private void addUser( JSONObject object) {
        if (connectionDetector.isConnectingToInternet()) {
            progressDialog.show();

            Log.v("object", "addUser : " + object.toString());
            String url = getString(R.string.add_user_url);

            String username = "NULL";
            String fbname = "NULL";
            String loc = "NULL";
            String bday = "NULL";
            String email = "NULL";
            String mobile = "NULL";
            String age = "NULL";
            String pref = "NULL";
            String fbid = "NULL";
            String fname = "NULL";
            String mname = "NULL";
            String lname = "NULL";
            String fburl = "NULL";
            String userName = "";


            if (object.optString("email") != null && object.optString("email").length() > 0) {
                email = object.optString("email");
            }

            if (object.optString("id") != null && object.optString("id").length() > 0) {
                fbid = object.optString("id");
            }
            if (object.optString("first_name") != null && object.optString("first_name").length() > 0) {
                fname = object.optString("first_name");
                userName = fname;
            }
            if (object.optString("middle_name") != null && object.optString("middle_name").length() > 0) {
                mname = object.optString("middle_name");
            }
            if (object.optString("last_name") != null && object.optString("last_name").length() > 0) {
                lname = object.optString("last_name");
                userName = userName + " " + lname;
            }
            if (object.optString("link") != null && object.optString("link").length() > 0) {
                fburl = object.optString("link");
            }
            Map<String, String> postParam = new HashMap<String, String>();

            try {
                String android_id = getUniqueID();

                postParam.put("username", username);
                postParam.put("fbname", fbname);
                postParam.put("loc", loc);
                postParam.put("bday", bday);
                postParam.put("email", email);
                postParam.put("mobile", mobile);
                postParam.put("age", age);
                postParam.put("pref", pref);
                postParam.put("fbid", fbid);
                postParam.put("fname", fname);
                postParam.put("mname", mname);
                postParam.put("lname", lname);
                postParam.put("fburl", fburl);
                postParam.put("usertype", "fb");
                postParam.put("device", android_id);
                postParam.put("os", "android");


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // Map<String,String> postParam = new HashMap<String, String>();

            final String finalUserName = userName;
            JsonUTF8StringRequest userAddReq = new JsonUTF8StringRequest(Request.Method.POST, url, postParam,
                    new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            Log.v("response", "response :" + response);

                            //SUCCESS: UID=5
                            Log.v(TAG, "respondString :" + response);

                            String[] separated = response.split(":");
                            if ((separated[0].trim().equalsIgnoreCase("SUCCESS")) || (separated[0].trim().equalsIgnoreCase("EXIST"))) {
                                //respondString :EXIST: UID=6:USERNAME=Shanaka Mendis:STATUS=pending:TYPE=fb:FBID=990283831022937[Ljava.lang.Object;@431387b0

                                if (separated[1] != null) {
                                    String uid = "";
                                    String fbId = "";
                                    String userName = "";
                                    for(int index=1; index<(separated.length); index++){
                                        Log.v(TAG,"length"+separated.length);
                                        String[] separated2 = separated[index].split("=");
                                        if (separated2[0] != null && separated2[0].trim().equalsIgnoreCase("UID")) {
                                            uid = separated2[1].trim();
                                        }
                                        if (separated2[0] != null && separated2[0].trim().equalsIgnoreCase("USERNAME")) {
                                            userName = separated2[1].trim();
                                        }
                                        if (separated2[0] != null && separated2[0].trim().equalsIgnoreCase("FBID")) {
                                            fbId = separated2[1].trim();
                                        }
                                    }

                                    Log.v(TAG,"uid :"+uid +"userName :"+userName +"fbId :"+fbId);

                                    loginSuccess(uid, userName,fbId,Constants.user_type_fb);
                                    userLoginId = uid;

                                    downloadUserBook();

                                    Log.v("response", "uid :" + uid);
                                } else {
                                    userAddedSuccess(false);

                                }

                            } else {
                                userAddedSuccess(false);
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

                    userAddedSuccess(false);
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


    private  void sendMail(String message) {

		/* Create the Intent */
        final Intent emailIntent = new Intent(
                android.content.Intent.ACTION_SEND);
        String messageBody = "<b>Message:</b> " + message;


		/* Fill it with Data */
        emailIntent.setType("text/html");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[] { "" });
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "DEBUG");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                Html.fromHtml(messageBody));

		/* Send it off to the Activity-Chooser */
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));

    }

    private void loginSuccess(String user_id,String userName,String fbId,String type) {
        SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.user_login_id),user_id);
        editor.putString(getString(R.string.user_login_name),userName);
        editor.putString(getString(R.string.user_fb_id),fbId);
        editor.putString(getString(R.string.user_type),type);
        editor.putBoolean(getString(R.string.user_login_status), true);
        editor.commit();
        AppController.getInstance().setUserId(user_id);
        AppController.getInstance().setFbId(fbId);
        AppController.getInstance().setUserName(userName);
        sendLoginSuccessMessage();

    }
    private void sendLoginSuccessMessage() {
        Intent intent = new Intent("login-status-event");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        //	uiHelper.onSaveInstanceState(savedState);
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
    private void downloadUserBook(){
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);}
        else {

            String url = getString(R.string.user_book_list_url);

            Map<String, String> params = new HashMap<String, String>();
            params.put("userid", userLoginId);

            JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(Request.Method.POST, url, params,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            Log.v("response", "respondString :" + jsonArray);
                            addToDownloadList(jsonArray);
                            userAddedSuccess(true);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    userAddedSuccess(true);

                }
            });
            bookListReq.setShouldCache(true);
            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(bookListReq, "tag_boo_list");
        }

    }

    private void addToDownloadList(JSONArray jsonArray){

        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(getApplicationContext());
        HashMap<String, AudioBook> hashMap = downloadedAudioBook.getBookList(getApplicationContext());
        Log.v("DownloadedChapter", "DownloadedChapter hashMap size befor" +hashMap.size());
        HashMap<String, AudioBook> bookList=new HashMap<String, AudioBook>();
        for (AudioBook item : hashMap.values()) {
            bookList.put(item.getBook_id(), item);
        }
        downloadedAudioBook.removeBook(getApplicationContext());

        Log.v("DownloadedChapter", "DownloadedChapter hashMap size after" +hashMap.size());

        for (int i = 0; i < jsonArray.length(); i++) {
            try {

                JSONObject obj = jsonArray.getJSONObject(i);
                AudioBook book = new AudioBook(obj, i, getApplicationContext());
                book.setPurchase(true);
                book.setDownloaded(true);

                if(bookList !=null && bookList.size()>0) {
                    AudioBook returnBook = bookList.get(book.getBook_id());

                        if (returnBook != null && returnBook.getDownloadedChapter() != null && returnBook.getDownloadedChapter().size() > 0) {
                            book.setDownloadedChapter(returnBook.getDownloadedChapter());

                        Log.v("DownloadedChapter", "DownloadedChapter" + returnBook.getDownloadedChapter().size());
                        //
                    } else {
                        Log.v("DownloadedChapter", "DownloadedChapter" + returnBook.getDownloadedChapter().size());

                    }
                }

                downloadedAudioBook.addBookToList(getApplicationContext(), book.getBook_id(), book);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {


        switch (requestCode)
        {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    downloadUserBook();
                    //reload my activity with permission granted or use the features what required the permission
                } else
                {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }

        }

    }


}
