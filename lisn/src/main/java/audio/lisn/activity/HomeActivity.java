package audio.lisn.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.Authorization;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.AuthorizationListener;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.AuthorizationOptions;
import audio.lisn.appsupport.gsma.android.mobileconnect.values.Prompt;
import audio.lisn.appsupport.gsma.android.mobileconnect.values.ResponseType;
import audio.lisn.fragment.HomeFragment;
import audio.lisn.fragment.MyBookFragment;
import audio.lisn.fragment.StoreBaseFragment;
import audio.lisn.fragment.StoreFragment;
import audio.lisn.model.BookCategory;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.Constants;
import audio.lisn.util.Log;
import audio.lisn.util.OnSwipeTouchListener;
import audio.lisn.view.PlayerControllerView;
import audio.lisn.webservice.JsonUTF8ArrayRequest;
import audio.lisn.webservice.JsonUTF8StringRequest;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        HomeFragment.OnHomeItemSelectedListener,StoreFragment.OnStoreBookSelectedListener,
        AuthorizationListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private DrawerLayout drawer;
    NavigationView navigationView;
    private int mNavItemId;
    boolean isUserLogin;
    PlayerControllerView playerControllerView;
    FrameLayout containerLayout;
    String subscriberId;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setActionBarIcon(R.drawable.ic_drawer);

        setContentView(R.layout.activity_home);
        containerLayout= (FrameLayout) findViewById(R.id.container_body);

        initToolbar();
        drawer = (DrawerLayout) findViewById(R.id.drawer);
        drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        getSupportActionBar().setTitle(R.string.title_home);

        // listen for navigation events
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        updateNavigationView();



        mNavItemId=R.id.drawer_home;

        playerControllerView = (PlayerControllerView) findViewById(R.id.audio_player_layout);

        playerControllerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Log.v(TAG, "onSwipeRight");
                playerControllerView.animate()
                        .translationX(playerControllerView.getWidth())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);

                                playerControllerView.setX(0);
                                playerControllerView.setVisibility(View.GONE);

                            }

                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                playerControllerView.stopAudioPlayer();

                                setLayoutMargin(false);
                            }
                        });

            }
            @Override
            public void onSingleTap() {
                showAudioPlayer();
            }

        });

        navigateFragment(mNavItemId);
        registerBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(menuUpdateReceiver,
                new IntentFilter(Constants.MENU_ITEM_SELECT));

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        loadTourScreen();

    }
    private void loadTourScreen(){
        Intent intent = new Intent(getApplicationContext(),
                AppTourActivity.class);
        startActivity(intent);
    }
    private void showAudioPlayer(){
        if(AppController.getInstance().getCurrentAudioBook() !=null) {
            PlayerControllerActivity.navigate(this, playerControllerView, null,-1);
        }

    }

    private void updateNavigationView() {
        isUserLogin= AppController.getInstance().isUserLogin();
        navigationView.getMenu().clear();
        View headerLayout = navigationView.getHeaderView(0); // 0-index header
        TextView userName = (TextView) headerLayout.findViewById(R.id.user_name);
        if (isUserLogin){
            navigationView.inflateMenu(R.menu.navigation_menu_member);

            userName.setText(AppController.getInstance().getUserName());
        }else{
            navigationView.inflateMenu(R.menu.navigation_menu_none_member);
            userName.setText("");
            mNavItemId =R.id.drawer_home ;
            navigateFragment(mNavItemId);

        }

    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_drawer);
            actionBar.setDisplayHomeAsUpEnabled(true);
          //  getSupportActionBar().setLogo(R.drawable.ic_app_top_bar);
        }
    }


//    @Override protected int getLayoutResource() {
//        return R.layout.activity_home;
//    }

private void setLayoutMargin(boolean setMargin){
    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)containerLayout.getLayoutParams();

    if(setMargin){
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, (int) getResources().getDimension(R.dimen.snackbar_height));
    }else{
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin,0);

    }

    containerLayout.setLayoutParams(params);
}
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.hasStartedPlayer){
            setLayoutMargin(true);

            playerControllerView.setVisibility(View.VISIBLE);
        }else{
            setLayoutMargin(false);

            playerControllerView.setVisibility(View.GONE);

        }
        playerControllerView.updateView();
        registerPlayerUpdateBroadcastReceiver();
        if(AppController.getInstance().isUserLogin()){
            updateServiceProvider();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayerUpdateReceiver);

    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {

	        
			MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.home, menu);
            MenuItem searchItem = menu.findItem(R.id.action_search);

            SearchManager searchManager = (SearchManager) HomeActivity.this.getSystemService(Context.SEARCH_SERVICE);

            SearchView searchView = null;
            if (searchItem != null) {
                searchView = (SearchView) searchItem.getActionView();
            }
            if (searchView != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(HomeActivity.this.getComponentName()));
            }

            return true;



	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
            item.setEnabled(false);


        switch (item.getItemId()) {
            case android.R.id.home:
                drawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
	}


    private void navigateFragment(int position) {
        Fragment fragment = null;
        String title = getString(R.string.app_name);
        switch (position) {
            case  R.id.drawer_home:
                fragment = HomeFragment.newInstance();
                title = getString(R.string.title_home);
                break;
            case R.id.drawer_store:
                fragment = StoreBaseFragment.newInstance();
                title = getString(R.string.title_store);
                break;
            case R.id.drawer_my_book:
                fragment = MyBookFragment.newInstance();
                title = getString(R.string.title_my_book);
                break;
            case R.id.drawer_settings:
                fragment = HomeFragment.newInstance();
               // title = getString(R.string.title_settings);
                break;
            case R.id.drawer_feedback:
                fragment = StoreBaseFragment.newInstance();
                // title = getString(R.string.title_settings);
                break;
            case R.id.drawer_about_us:
                fragment = MyBookFragment.newInstance();
                // title = getString(R.string.title_settings);
                break;

            default:
                break;
        }

        if (fragment != null) {

            final Fragment finalFragment = fragment;

            new Handler().post(new Runnable() {
                public void run() {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.container_body, finalFragment);
                    fragmentTransaction.commit();
                }
            });



            // set the toolbar title
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        if(menuItem.getItemId() ==R.id.drawer_about_us){
            //menuItem.setChecked(false);

            Intent intent = new Intent(this,
                    AboutUsActivity.class);
            startActivity(intent);

        }
        else if(menuItem.getItemId() ==R.id.drawer_feedback){
            //menuItem.setChecked(false);

            Intent intent = new Intent(this,
                    FeedBackActivity.class);
            startActivity(intent);

        }
        else if(menuItem.getItemId() ==R.id.drawer_contact_us){
            //menuItem.setChecked(false);

            Intent intent = new Intent(this,
                    ContactusActivity.class);
            startActivity(intent);

        }
        else if(menuItem.getItemId() ==R.id.drawer_app_tour){

            Intent intent = new Intent(this,
                    AppTourActivity.class);
            startActivity(intent);

        }
        else if(menuItem.getItemId() ==R.id.drawer_settings){
            AppController.getInstance().logOutUser();
            updateNavigationView();

        }else if(menuItem.getItemId() ==R.id.drawer_my_book){
            if(AppController.getInstance().isUserLogin()){
                mNavItemId = menuItem.getItemId();
                navigateFragment(mNavItemId);
            }else{
                Intent intent = new Intent(this,
                        LoginActivity.class);
                startActivityForResult(intent, 13);
            }

        }
        else {
           // menuItem.setChecked(true);
            mNavItemId = menuItem.getItemId();
            navigateFragment(mNavItemId);
        }
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }


    @Override
    public void onHomeItemSelected(int position, boolean isDownloadedBook) {
       // navigationView.getMenu().getItem(3).setChecked(true);


    }

    @Override
    public void onOptionButtonClicked(int buttonIndex) {

        MenuItem menuItem=navigationView.getMenu().getItem(buttonIndex);

            menuItem.setChecked(false);
            mNavItemId = menuItem.getItemId();
            navigateFragment(mNavItemId);


    }

    @Override
    public void onStoreBookSelected(int position) {

    }
    private void downloadBookCategoryList() {
        String url=getString(R.string.book_category_list_url);

        JsonUTF8ArrayRequest categoryListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        updateCategoryList(jsonArray);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        categoryListReq.setShouldCache(true);
        AppController.getInstance().addToRequestQueue(categoryListReq, "tag_category_list");
    }
    private void updateCategoryList(JSONArray jsonArray){
        BookCategory[] bookCategories= new BookCategory[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            try {

                JSONObject obj = jsonArray.getJSONObject(i);
                BookCategory bookCategory=new BookCategory(obj);
                bookCategories[i]=bookCategory;

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        AppController.getInstance().setBookCategories(bookCategories);
        onOptionButtonClicked(2);

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 13) {
            if(resultCode ==  Constants.RESULT_SUCCESS){
                new Handler().post(new Runnable() {
                    public void run() {
                        downloadBookCategoryList();

                    }
                });



            }
            if (resultCode ==  Constants.RESULT_ERROR) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                android.support.v7.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        }else {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }


            super.onActivityResult(requestCode,  resultCode,  data);
    }
        private void registerBroadcastReceiver(){
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mLoginUpdateReceiver,
                new IntentFilter("login-status-event"));
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mLoginUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            Log.v("mLoginUpdateReceiver","mLoginUpdateReceiver");
            updateNavigationView();
            MenuItem menuItem=navigationView.getMenu().getItem(1);
            menuItem.setChecked(true);
        }
    };
    private void registerPlayerUpdateBroadcastReceiver(){
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mPlayerUpdateReceiver,
                new IntentFilter("audio-event"));
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mPlayerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            playerControllerView.updateView();
        }
    };

    private BroadcastReceiver menuUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int index = intent.getExtras().getInt("index");
            navigationView.getMenu().getItem(index).setChecked(true);
        }
    };

    //Update mobile data

    private void updateServiceProvider(){
        if(isMobileDataEnable()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager m_telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

                subscriberId = m_telephonyManager.getSubscriberId();

                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                String provider = sharedPref.getString(getString(R.string.service_provider), "");
                if (subscriberId != null && !provider.equalsIgnoreCase(subscriberId)) {

                    if(subscriberId.startsWith("41302")) {
                        getDialogMobileNumber();
                    }else{

                        String url = "";

                        if (subscriberId.startsWith("41301")) {
                            url = getResources().getString(R.string.mobitel_pay_url);
                        } else if (subscriberId.startsWith("41303")) {
                            url = getResources().getString(R.string.etisalat_pay_url);
                        }
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("userid", AppController.getInstance().getUserId());
                        params.put("action", "number");

                        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,true,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {

                                        if (response.toUpperCase().contains("SUCCESS")) {
                                            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                                                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPref.edit();

                                            editor.putString(getString(R.string.service_provider), subscriberId);
                                            editor.commit();
                                        }

                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
                        AppController.getInstance().addToRequestQueue(stringRequest, "tag_mobitel_number");
                    }
                }

            }
        }

    }
    private void getDialogMobileNumber(){

        String openIDConnectScopes = "openid phone";
        String returnUri = getString(R.string.dialog_pay_url);

        String authUri=getString(R.string.mconnect_url);//"https://mconnect.dialog.lk/openidconnect/authorize";
        String clientId="y0erf48J8J_JFKuCrNM4TKfLxnAa";
        String clientSecret="Y1FMDA3wtPT6dfMebci9lWUudnMa";

        String state= UUID.randomUUID().toString();
        String nonce=UUID.randomUUID().toString();
        int maxAge=0;
        String acrValues="2";

        Authorization authorization=new Authorization();

        AuthorizationOptions authorizationOptions=new AuthorizationOptions();
        authorizationOptions.setClaimsLocales("en");
        authorizationOptions.setUILocales("en");
        authorizationOptions.setLoginHint("+44");

        Prompt prompt= Prompt.LOGIN;

        //prompt=Prompt.NONE;
        authorizationOptions.setUILocales("");

        authorization.authorize(authUri, ResponseType.CODE, clientId, clientSecret, openIDConnectScopes, returnUri, state, nonce, prompt,
                maxAge, acrValues, authorizationOptions, this /* listener */, this /* activity */);


    }
    private boolean isMobileDataEnable(){

        boolean mobileYN = false;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            mobileYN = Settings.Global.getInt(getContentResolver(), "mobile_data", 1) == 1;
        }
        else{
            mobileYN = Settings.Secure.getInt(getContentResolver(), "mobile_data", 1) == 1;
        }
        return mobileYN;

    }

    @Override
    public void authorizationCodeResponse(String state, String authorizationCode, String error, String clientId, String clientSecret, String scopes, String redirectUri) {
Log.v(TAG,"state :"+state);
        if(authorizationCode.equalsIgnoreCase("0")){

            String url = getResources().getString(R.string.update_mobile_no_url);


            Map<String, String> params = new HashMap<String, String>();
            params.put("userid", AppController.getInstance().getUserId());
            params.put("mobile", state);

            JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            if (response.toUpperCase().contains("SUCCESS")) {
                                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();

                                editor.putString(getString(R.string.service_provider), subscriberId);
                                editor.commit();
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
            AppController.getInstance().addToRequestQueue(stringRequest, "tag_update_number");
        }
    }

    @Override
    public void authorizationError(String reason) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
           // mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            //mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
