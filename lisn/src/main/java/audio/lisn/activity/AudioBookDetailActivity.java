package audio.lisn.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.transition.Slide;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.widget.ShareDialog;
import com.ms.square.android.expandabletextview.ExpandableTextView;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import audio.lisn.R;
import audio.lisn.adapter.BookReviewViewAdapter;
import audio.lisn.app.AppController;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.Authorization;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.AuthorizationListener;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.AuthorizationOptions;
import audio.lisn.appsupport.gsma.android.mobileconnect.values.Prompt;
import audio.lisn.appsupport.gsma.android.mobileconnect.values.ResponseType;
import audio.lisn.model.AudioBook;
import audio.lisn.model.BookReview;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.service.DownloadService;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.CustomTypeFace;
import audio.lisn.util.Log;
import audio.lisn.util.OnSwipeTouchListener;
import audio.lisn.util.WCLinearLayoutManager;
import audio.lisn.util.WakeLocker;
import audio.lisn.view.PlayerControllerView;
import audio.lisn.webservice.FileDownloadTask;
import audio.lisn.webservice.FileDownloadTaskListener;
import audio.lisn.webservice.JsonUTF8StringRequest;

public class AudioBookDetailActivity extends  AppCompatActivity implements Runnable,FileDownloadTaskListener,AuthorizationListener {

    private static final String TRANSITION_NAME = "audio.lisn.AudioBookDetailActivity";
    private CollapsingToolbarLayout collapsingToolbarLayout;
    AudioBook audioBook;
    ImageButton previewPlayButton;
    Button btnReview;
    MediaPlayer mediaPlayer = null;
    ConnectionDetector connectionDetector;

    public RelativeLayout previewLayout;
    public TextView previewLabel,timeLabel;
    public ProgressBar spinner;
    private boolean isPlayingPreview,isLoadingPreview;
    String leftTime;
    ProgressDialog mProgressDialog;
    ProgressDialog progressDialog;
    int totalAudioFileCount, downloadedFileCount;
    List<FileDownloadTask> downloadingList = new ArrayList<FileDownloadTask>();
    ImageView bookCoverImage;
    private PopupWindow pwindo;
    int previousDownloadedFileCount;
    PlayerControllerView playerControllerView;
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE=101;


    BookReviewViewAdapter bookReviewViewAdapter;
    RecyclerView reviewContainer;
    private static final String KEY_TERMS_ACCEPTED_FOR_CARD="KEY_TERMS_ACCEPTED_FOR_CARD";
    private static final String KEY_TERMS_ACCEPTED_FOR_DIALOG="KEY_TERMS_ACCEPTED_FOR_DIALOG";
    private static final String KEY_TERMS_ACCEPTED_FOR_MOBITEL="KEY_TERMS_ACCEPTED_FOR_MOBITEL";
    private static final String KEY_TERMS_ACCEPTED_FOR_ETISALAT="KEY_TERMS_ACCEPTED_FOR_ETISALAT";

    String dialogNo;

    CallbackManager callbackManager;

    public enum ServiceProvider {
        PROVIDER_NONE, PROVIDER_MOBITEL,PROVIDER_DIALOG,PROVIDER_ETISALAT
    }

    public enum PaymentOption {
        OPTION_NONE, OPTION_CARD,OPTION_MOBITEL,OPTION_DIALOG,OPTION_ETISALAT
    }

    ServiceProvider  serviceProvider;
    PaymentOption  paymentOption;
    Thread timerUpdateThread;
    String subscriberId;


    public static void navigate(AppCompatActivity activity, View transitionImage, AudioBook audioBook) {
        Intent intent = new Intent(activity, AudioBookDetailActivity.class);
        intent.putExtra("audioBook", audioBook);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage,activity.getString(R.string.transition_book));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @SuppressWarnings("ConstantConditions")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_audio_book_detail);
        findServiceProvider();
        callbackManager = CallbackManager.Factory.create();
        dialogNo="";
        downloadedFileCount=0;
        audioBook = (AudioBook) getIntent().getSerializableExtra("audioBook");

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String itemTitle = audioBook.getEnglish_title();
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(itemTitle);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        connectionDetector = new ConnectionDetector(getApplicationContext());

        playerControllerView = (PlayerControllerView) findViewById(R.id.audio_player_layout);
        playerControllerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                playerControllerView.animate()
                        .translationX(playerControllerView.getWidth())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                playerControllerView.setX(0);
                                playerControllerView.setVisibility(View.INVISIBLE);

                            }

                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                // setLayoutMargin(false);

                                playerControllerView.stopAudioPlayer();
                            }
                        });

            }

            @Override
            public void onSingleTap() {
                showAudioPlayer();
            }

        });


    }
    @Override
    protected void onResume() {
        super.onResume();
        updateData();

        if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.hasStartedPlayer){
            playerControllerView.setVisibility(View.VISIBLE);
        }else{
            playerControllerView.setVisibility(View.INVISIBLE);

        }
        playerControllerView.updateView();
        registerPlayerUpdateBroadcastReceiver();
        bookReviewViewAdapter.notifyDataSetChanged();
    }
    @Override
    public void onPause() {
        super.onPause();
        if(mediaPlayer !=null && mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayerUpdateReceiver);

    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if ( mProgressDialog!=null && mProgressDialog.isShowing() ){
            mProgressDialog.dismiss();
        }
    }

    private void findServiceProvider() {
        Log.v("deviceID", "findDeviceID");
        serviceProvider = ServiceProvider.PROVIDER_NONE;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSIONS_REQUEST_READ_PHONE_STATE);
        } else {
            setServiceProvider();
        }

    }
    private void setServiceProvider(){
        TelephonyManager m_telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

         this.subscriberId = m_telephonyManager.getSubscriberId();
        if (subscriberId != null) {
            if (subscriberId.startsWith("41301")) {
                serviceProvider = ServiceProvider.PROVIDER_MOBITEL;
            } else if (subscriberId.startsWith("41302")) {
                serviceProvider = ServiceProvider.PROVIDER_DIALOG;
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
                    updateData();
                    downloadAudioFile();
                } else
                {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
            case PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    setServiceProvider();
                }

            }
        }

    }
    private void termsAndConditionAccepted(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if(paymentOption == PaymentOption.OPTION_CARD){
            prefs.edit().putBoolean(KEY_TERMS_ACCEPTED_FOR_CARD, true).commit();

            buyFromCardButtonPressed();

        }else if(paymentOption == PaymentOption.OPTION_MOBITEL){
            prefs.edit().putBoolean(KEY_TERMS_ACCEPTED_FOR_MOBITEL, true).commit();

            addToMobitelBill();

        }else if(paymentOption == PaymentOption.OPTION_ETISALAT){
            prefs.edit().putBoolean(KEY_TERMS_ACCEPTED_FOR_ETISALAT, true).commit();

            addToEtisalatBill();

        }
        else if(paymentOption == PaymentOption.OPTION_DIALOG){

            prefs.edit().putBoolean(KEY_TERMS_ACCEPTED_FOR_DIALOG, true).commit();
            addToDialogBill();


        }

    }
    private void showTermsAndCondition(){

        String title = getString(R.string.app_name) ;
        String message = getString(R.string.terms_condition);


        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(Html.fromHtml(message))
                .setCancelable(false)
                .setPositiveButton(R.string.terms_accept,
                        new Dialog.OnClickListener() {

                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface, int i) {
                                // Mark this version as read.
                                termsAndConditionAccepted();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new Dialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }

                        });
        builder.create().show();

    }

    private void getDialogMobileNumber(){

        if(AppController.getInstance().isUserLogin()) {

            if (connectionDetector.isConnectingToInternet()) {


        progressDialog.setMessage("Payment Processing...");
        progressDialog.show();
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
        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }

    private void showAudioPlayer(){
        PlayerControllerActivity.navigate(this, playerControllerView, null);

    }
    private void showBookReview(){
        BookReviewActivity.navigate(this, playerControllerView, audioBook.getReviews());

    }
    private void updateData() {

        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        ImageView bookBannerImage = (ImageView) findViewById(R.id.bookBannerImage);
        ViewGroup.LayoutParams params = bookBannerImage.getLayoutParams();
        params.height=(int)(size.x/2);
        bookBannerImage.setLayoutParams(params);

        // String bannerImageUrl="http://lorempixel.com/500/500/animals/8/";
        String bannerImageUrl=audioBook.getBanner_image();

        Picasso.with(this)
                .load(bannerImageUrl)
                .placeholder(R.drawable.default_banner)
                .into(bookBannerImage);

        bookCoverImage = (ImageView) findViewById(R.id.bookCoverImage);
        Picasso.with(this)
                .load(audioBook.getCover_image())
                .placeholder(R.drawable.ic_launcher)
                .into(bookCoverImage);

        // ViewGroup.LayoutParams params = bookCoverImage.getLayoutParams();
        RelativeLayout.LayoutParams bookCoverImageLayoutParams =
                (RelativeLayout.LayoutParams)bookCoverImage.getLayoutParams();
        bookCoverImageLayoutParams.width= (int) ((size.x-60)/3);

        bookCoverImage.setLayoutParams(bookCoverImageLayoutParams);

        String narratorText="";
        String durationText="";
        TextView title = (TextView) findViewById(R.id.title);

        ExpandableTextView description = (ExpandableTextView) findViewById(R.id.description);
        TextView descriptionTextView = (TextView) findViewById(R.id.expandable_text);


        TextView fileSize = (TextView) findViewById(R.id.fileSize);
        TextView duration = (TextView) findViewById(R.id.duration);

        TextView category = (TextView) findViewById(R.id.category);
        TextView price = (TextView) findViewById(R.id.price);
        TextView author = (TextView) findViewById(R.id.author);
        TextView narrator = (TextView) findViewById(R.id.narrator);
        TextView ratingValue = (TextView) findViewById(R.id.rating_value);
        LinearLayout rateLayout=(LinearLayout)findViewById(R.id.app_rate_layout);

        View separator_top_description=(View)findViewById(R.id.separator_top_description);
        View separator_top_rateLayout=(View)findViewById(R.id.separator_top_rateLayout);
        View separator_top_reviewContainer=(View)findViewById(R.id.separator_top_reviewContainer);

        Button btnDownload=(Button)findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentOption=PaymentOption.OPTION_NONE;
                playGetButtonPressed();

            }
        });

        Button addToBillButton=(Button)findViewById(R.id.addToBillButton);
        addToBillButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToMyBillButtonPressed();
            }
        });

        Button btnPayFromCard=(Button)findViewById(R.id.buyFromCardButton);
        btnPayFromCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentOption=PaymentOption.OPTION_CARD;
                buyFromCardButtonPressed();
            }
        });


        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating_bar);
        LayerDrawable stars1 = (LayerDrawable) ratingBar.getProgressDrawable();
        stars1.getDrawable(2).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);


        RatingBar userRatingBar = (RatingBar) findViewById(R.id.user_rate_bar);
        LayerDrawable stars = (LayerDrawable) userRatingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);

        userRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {

            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if(fromUser){
                    initiatePopupWindow(rating);
                }

            }

        });
        previewPlayButton = (ImageButton) findViewById(R.id.previewPlayButton);
        previewPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPreviewButtonPressed();

            }
        });

        previewLayout=(RelativeLayout)findViewById(R.id.preview_layout);
        RelativeLayout.LayoutParams previewLayoutLayoutParams =
                (RelativeLayout.LayoutParams)previewLayout.getLayoutParams();
        previewLayoutLayoutParams.width= (int) ((size.x-60)/3);

        previewLayout.setLayoutParams(previewLayoutLayoutParams);
        previewLabel=(TextView)findViewById(R.id.preview_label);
        timeLabel=(TextView)findViewById(R.id.time_label);
        RelativeLayout.LayoutParams timeLabelLayoutParams =
                (RelativeLayout.LayoutParams)timeLabel.getLayoutParams();
        timeLabelLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);


        spinner = (ProgressBar)findViewById(R.id.progressBar);

        if(audioBook.getLanguageCode()== AudioBook.LanguageCode.LAN_SI){
            descriptionTextView.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            title.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            author.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            category.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            narrator.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            duration.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            narratorText=getString(R.string.narrator_si);
            durationText=getString(R.string.duration_si);
        }else{
            descriptionTextView.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            title.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            author.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            category.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            narrator.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            duration.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));

            narratorText=getString(R.string.narrator_en);
            durationText=getString(R.string.duration_en);
        }
        title.setText(audioBook.getTitle());
        String priceText="Free";
        if( Float.parseFloat(audioBook.getPrice())>0 ){
            priceText="Rs. "+audioBook.getPrice();
        }

        if(Float.parseFloat(audioBook.getRate())>-1){
            ratingBar.setRating(Float.parseFloat(audioBook.getRate()));
            ratingValue.setText(String.format("%.1f", Float.parseFloat(audioBook.getRate())));


        }

        narratorText=narratorText+" - "+audioBook.getNarrator();
        durationText=durationText+" - "+audioBook.getDuration();
        author.setText(audioBook.getAuthor());
        category.setText(audioBook.getCategory());
        narrator.setText(narratorText);
        fileSize.setText(audioBook.getFileSize()+" Mb");
        price.setText(priceText);
        title.setText(audioBook.getTitle());
        duration.setText(durationText);
        if(audioBook.getDescription() !=null && audioBook.getDescription().length()>1){
            description.setText(audioBook.getDescription());
            description.setVisibility(View.VISIBLE);
            separator_top_description.setVisibility(View.VISIBLE);
        }else{
        }

        btnPayFromCard.setText("Pay by Card (" + audioBook.getDiscount() + "% discount)");

        if(AppController.getInstance().isUserLogin() && audioBook.isPurchase()){
            previewPlayButton.setVisibility(View.GONE);
            btnDownload.setText("Download");
            btnDownload.setVisibility(View.VISIBLE);

            if(audioBook.getAudioFileCount() == audioBook.getDownloadedChapter().size()){
                btnDownload.setText("Play");
            }
            separator_top_rateLayout.setVisibility(View.VISIBLE);
            rateLayout.setVisibility(View.VISIBLE);
            userRatingBar.setRating(0);
            addToBillButton.setVisibility(View.GONE);
            btnPayFromCard.setVisibility(View.GONE);
        }else{
            if(Float.parseFloat(audioBook.getPrice())>0) {
                btnDownload.setVisibility(View.GONE);

                if (serviceProvider !=ServiceProvider.PROVIDER_NONE){
                    addToBillButton.setVisibility(View.VISIBLE);
                    if(serviceProvider ==ServiceProvider.PROVIDER_MOBITEL){
                        addToBillButton.setText("Add to Mobitel bill");
                    }
                    else if(serviceProvider ==ServiceProvider.PROVIDER_DIALOG){
                        addToBillButton.setText("Add to Dialog bill");

                    }
                    else if(serviceProvider ==ServiceProvider.PROVIDER_ETISALAT){
                        addToBillButton.setText("Add to Etisalat bill");

                    }

                }
                btnPayFromCard.setVisibility(View.VISIBLE);


            }else{
                btnDownload.setText("Download");
                btnDownload.setVisibility(View.VISIBLE);

            }

        }

        ImageButton  btnShare=(ImageButton)findViewById(R.id.btnShare);

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBook();
            }
        });
       LinearLayout shareLayout=(LinearLayout)findViewById(R.id.shareLayout);
        if(AppController.getInstance().isUserLogin() && audioBook.isPurchase()) {
            shareLayout.setVisibility(View.VISIBLE);
        }else{
            shareLayout.setVisibility(View.INVISIBLE);
        }



            int reviewsCount=0;

        if(audioBook.getReviews() !=null)
            reviewsCount=audioBook.getReviews().size();
        final int finalReviewsCount = reviewsCount;

        if(finalReviewsCount>0){
            separator_top_reviewContainer.setVisibility(View.VISIBLE);

        }
        btnReview=(Button)findViewById(R.id.btnReview);
        btnReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(finalReviewsCount >0){
                    showBookReview();

                }


            }
        });


        btnReview.setText(""+reviewsCount);


        TextView allReviews=(TextView)findViewById(R.id.all_reviews);
        allReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showBookReview();


            }
        });


        reviewContainer=(RecyclerView)findViewById(R.id.reviewContainer);

        ArrayList<BookReview> reviews = new ArrayList<BookReview>();

        if(reviewsCount>3){
            for (int i=0;i<3;i++){
                reviews.add(audioBook.getReviews().get(i));
            }

            allReviews.setVisibility(View.VISIBLE);
        }else{
            reviews=audioBook.getReviews();
        }
        WCLinearLayoutManager linearLayoutManagerVertical =
                new WCLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        reviewContainer.setLayoutManager(linearLayoutManagerVertical);

        bookReviewViewAdapter=new BookReviewViewAdapter(getApplicationContext(),reviews);
        reviewContainer.setAdapter(bookReviewViewAdapter);

        mProgressDialog = new ProgressDialog(AudioBookDetailActivity.this);
        mProgressDialog.setMessage("Downloading file..");
        mProgressDialog.setTitle("Download in progress ...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                stopDownload();
            }
        });

        progressDialog = new ProgressDialog(AudioBookDetailActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Publishing...");




    }

    private void stopDownload(){
        for (int i = 0; i < downloadingList.size(); i++) {
            FileDownloadTask downloadTask = downloadingList.get(i);
            downloadTask.cancel(true);

        }
    }


    private void updatePreviewLayout(){
        if(isLoadingPreview || isPlayingPreview){
           // setLayoutMargin(true);
            previewLayout.setVisibility(View.VISIBLE);
            previewPlayButton.setImageResource(R.drawable.btn_play_preview_pause);

            if(isPlayingPreview){
                spinner.setVisibility(View.INVISIBLE);
                previewLabel.setText("Preview");
                timeLabel.setText(leftTime);

            }else{
                spinner.setVisibility(View.VISIBLE);
                previewLabel.setText("Loading...");
                timeLabel.setText("");
            }
        }else{
           // setLayoutMargin(false);
            previewLayout.setVisibility(View.INVISIBLE);
            previewPlayButton.setImageResource(R.drawable.btn_play_preview_start);
        }
    }

    private void playPreviewButtonPressed(){
        if (audioBook.getPreview_audio() !=null && (audioBook.getPreview_audio().length()>0)) {
            boolean stopPlayer = false;
            if(isLoadingPreview || isPlayingPreview ){
                stopPlayer=true;
            }

            if(stopPlayer){
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    new Thread(this).interrupt();
                }

                mediaPlayer.reset();
                isPlayingPreview=false;
                isLoadingPreview=false;

            }else{
                playPreview();
            }

        }
        updatePreviewLayout();
    }
    private void playPreview( ) {

        isLoadingPreview=true;
        isPlayingPreview=false;
        if (connectionDetector.isConnectingToInternet()) {
            pausePlayer();
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                if( timerUpdateThread != null ) {
                    timerUpdateThread.interrupt();
                }
            }

            mediaPlayer.reset();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(audioBook.getPreview_audio());
            } catch (IOException e) {
                Log.v("playPreview", "IOException" + e.getMessage());

                e.printStackTrace();
            }
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    isPlayingPreview=true;
                    isLoadingPreview=false;
                    startTimer();
                    mp.start();
                    updatePreviewLayout();
                    WakeLocker.acquire(getApplicationContext());
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    return false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlayingPreview=false;
                    isLoadingPreview=false;
                    stopTimer();
                    updatePreviewLayout();
                    WakeLocker.release();

                }
            });
            mediaPlayer.prepareAsync(); // prepare async to not block main


        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_ENOUGH_SPACE_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    private void updateAudioBook(int chapter){
        if(chapter>0) {
            audioBook.addChapterToDownloadedChapter(chapter);
        }
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getApplicationContext());
       // downloadedAudioBook.readFileFromDisk(getApplicationContext());
        downloadedAudioBook.addBookToList(getApplicationContext(),
                audioBook.getBook_id(), audioBook);

    }
    private void logUserDownload(){
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        saveCoverImage();
        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", AppController.getInstance().getUserId());
        params.put("bookid", audioBook.getBook_id());

        String url = getResources().getString(R.string.user_download_activity_url);

        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url,params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("response", "response:" + response);

                        audioBook.setPurchase(true);
                        updateAudioBook(0);
                        progressDialog.dismiss();
                        updateData();
                        downloadAudioFile();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();

            }
        });


        AppController.getInstance().addToRequestQueue(stringRequest, "tag_download_book");

    }

    private void downloadAudioFileFromUrl(int filePart){

        if (connectionDetector.isConnectingToInternet()) {
            String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                    + audioBook.getBook_id()+File.separator;
            String filePath=dirPath + filePart + ".lisn";
            File file = new File(filePath);

            if (file.exists()) {
                file.delete();
            }



            FileDownloadTask downloadTask =  new FileDownloadTask(this,this,audioBook.getBook_id());
            downloadTask.execute(dirPath, "" + filePart);
            downloadingList.add(downloadTask);


        }else{
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    this);
            builder.setTitle(getString(R.string.NO_INTERNET_TITLE)).setMessage(getString(R.string.NO_INTERNET_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private void downloadAudioFile() {
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }else {
            previousDownloadedFileCount = downloadedFileCount;
            String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                    + audioBook.getBook_id() + File.separator;
            File fileDir = new File(dirPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();

            }

            if (connectionDetector.isConnectingToInternet()) {

                mProgressDialog.show();
                downloadedFileCount = 0;
                totalAudioFileCount = 0;
                downloadingList.clear();


                for (int filePart = 1; filePart <= (audioBook.getAudioFileCount()); filePart++) {
                    File file = new File(dirPath + filePart + ".lisn");

                    if (!file.exists() || !(audioBook.getDownloadedChapter().contains(filePart))) {
                        downloadAudioFileFromUrl(filePart);
                        totalAudioFileCount++;
                    }


                }
                if (downloadedFileCount == totalAudioFileCount) {
                    mProgressDialog.dismiss();
                    starAudioPlayer();


                } else {
                    if (AppUtils.getAvailableMemory() < audioBook.getFileSize()) {
                        stopDownload();

                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                this);
                        builder.setTitle(R.string.NO_ENOUGH_SPACE_TITLE).setMessage(R.string.NO_ENOUGH_SPACE_MESSAGE).setPositiveButton(
                                R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // FIRE ZE MISSILES!
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();

                    } else {
                        mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());
                    }
                }

            } else {

                downloadedFileCount = 0;
                totalAudioFileCount = 0;

                for (int filePart = 1; filePart <= (audioBook.getAudioFileCount()); filePart++) {
                    File file = new File(dirPath + filePart + ".lisn");
                    if (!file.exists()) {
                        totalAudioFileCount++;
                    }


                }
                if (downloadedFileCount == totalAudioFileCount) {
                    mProgressDialog.dismiss();
                    starAudioPlayer();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            this);
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
        }
    }

    private void starAudioPlayer() {
        if (previousDownloadedFileCount == 0) {
            PlayerControllerActivity.navigate(this, bookCoverImage, audioBook);

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.DOWNLOAD_COMPLETE_TITLE).setMessage(getString(R.string.DOWNLOAD_COMPLETE_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_YES, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            PlayerControllerActivity.navigate(AudioBookDetailActivity.this, bookCoverImage, audioBook);

                        }
                    })
                    .setNegativeButton(R.string.BUTTON_NO, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                            updateData();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    private void shareBook() {

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ui_bg_logo);

        String imageUrl=audioBook.getCover_image();

        String description=audioBook.getEnglish_description();
        if(description == null || description.contains("null")){
            description=audioBook.getEnglish_title();
        }
        ShareOpenGraphObject object = new ShareOpenGraphObject.Builder()
                .putString("og:type", "lisn_audiobook:audiobook")
                .putString("og:title", audioBook.getEnglish_title())
                .putString("og:description", description)
                .putString("og:image", imageUrl)

                        .build();

// Create an action
        ShareOpenGraphAction action = new ShareOpenGraphAction.Builder()
                .setActionType("lisn_audiobook:enjoy")
                .putInt("expires_in",60*60*24)
                .putObject("audiobook", object)
                .build();
        // Create the content
        ShareOpenGraphContent content = new ShareOpenGraphContent.Builder()
                .setPreviewPropertyName("audiobook")
                .setAction(action)
                .build();

        ShareDialog shareDialog = new ShareDialog(this);

        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {

            @Override
            public void onSuccess(Sharer.Result result) {

                AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);

                builder.setTitle(R.string.SHARE_SUCCESS_TITLE).setMessage(getString(R.string.SHARE_SUCCESS_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);

                builder.setTitle(R.string.SHARE_ERROR_TITLE).setMessage(getString(R.string.SHARE_ERROR_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        shareDialog.show(content);

    }

    private void playGetButtonPressed(){

        if(AppController.getInstance().isUserLogin()){

            if (audioBook.isPurchase()) {
                downloadAudioFile();

            } else {
                if (Float.parseFloat(audioBook.getPrice()) > 0) {
                    Intent intent = new Intent(this,
                            PurchaseActivity.class);
                    intent.putExtra("audioBook", audioBook);
                    startActivityForResult(intent, 2);

                }else{
                    logUserDownload();
                }
            }
        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }
    private void buyFromCardButtonPressed(){

        if(AppController.getInstance().isUserLogin()){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if(prefs.getBoolean(KEY_TERMS_ACCEPTED_FOR_CARD, false)) {
                Intent intent = new Intent(this,
                        PurchaseActivity.class);
                intent.putExtra("audioBook", audioBook);
                startActivityForResult(intent, 2);

            }else {
                showTermsAndCondition();
            }

        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }

    private void addToMyBillButtonPressed(){

        if(isMobileDataEnable()) {
            if (serviceProvider == ServiceProvider.PROVIDER_MOBITEL) {
                paymentOption = PaymentOption.OPTION_MOBITEL;
                addToMobitelBill();
            } else if (serviceProvider == ServiceProvider.PROVIDER_ETISALAT) {
                paymentOption = PaymentOption.OPTION_ETISALAT;
                addToEtisalatBill();

            }
            else if (serviceProvider == ServiceProvider.PROVIDER_DIALOG) {
                paymentOption = PaymentOption.OPTION_DIALOG;
                addToDialogBill();

            }
        }else{

            SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            String provider = sharedPref.getString(getString(R.string.service_provider),"");

            if(provider.equalsIgnoreCase(subscriberId)) {
                if (serviceProvider == ServiceProvider.PROVIDER_MOBITEL ) {
                    paymentOption = PaymentOption.OPTION_MOBITEL;
                    addToMobitelBill();
                } else if (serviceProvider == ServiceProvider.PROVIDER_ETISALAT ) {
                    paymentOption = PaymentOption.OPTION_ETISALAT;
                    addToEtisalatBill();

                }else if (serviceProvider == ServiceProvider.PROVIDER_DIALOG ) {
                    paymentOption = PaymentOption.OPTION_DIALOG;
                    addToDialogBill();

                }
            }else {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        this);
                builder.setTitle(R.string.NO_MOBILE_DATA_TITLE).setMessage(R.string.NO_MOBILE_DATA_MESSAGE).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

    }
    private void updateServiceProviderData(){


        SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(getString(R.string.service_provider),subscriberId);
        editor.commit();
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
        if(connectionDetector.getNetworkType() == ConnectionDetector.NetworkType.TYPE_MOBILE){
            return mobileYN;
        }else{
            return false;
        }


    }
    private void addToBillServerConnect(){

        Log.v("addToBillServerConnect","addToBillServerConnect 1");

        String url = "";

        if(paymentOption==PaymentOption.OPTION_MOBITEL){
            url = getResources().getString(R.string.mobitel_pay_url);
        } else if(paymentOption==PaymentOption.OPTION_DIALOG){
            url = getResources().getString(R.string.dialog_pay_url);
        } else if(paymentOption==PaymentOption.OPTION_ETISALAT){
            url = getResources().getString(R.string.etisalat_pay_url);
        }
        progressDialog.setMessage("Payment Processing...");
        progressDialog.show();

        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", AppController.getInstance().getUserId());
        params.put("bookid", audioBook.getBook_id());
        params.put("amount", audioBook.getPrice());
        if(paymentOption==PaymentOption.OPTION_DIALOG){
            params.put("number", dialogNo);
        }else{
            params.put("action", "charge");
        }



        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,true,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("addToBillServerConnect", "addToBillServerConnect 2");


                        progressDialog.dismiss();
                        if (response.toUpperCase().contains("SUCCESS")) {
                            Log.v("addToBillServerConnect", "addToBillServerConnect 3");
                            updateServiceProviderData();

                            audioBook.setPurchase(true);
                            updateAudioBook(0);
                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(getString(R.string.PAYMENT_COMPLETE_TITLE)).
                                    setMessage(getString(R.string.PAYMENT_COMPLETE_MESSAGE)).setPositiveButton(
                                    getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            updateData();
                                            downloadAudioFile();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.BUTTON_LATER), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            updateData();
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else if (response.toUpperCase().contains("ALREADY_PAID")) {
                            Log.v("addToBillServerConnect","addToBillServerConnect 3");

                            audioBook.setPurchase(true);
                            updateAudioBook(0);
                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(getString(R.string.ALREADY_PAID_TITLE)).
                                    setMessage(getString(R.string.ALREADY_PAID_MESSAGE)).setPositiveButton(
                                    getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            updateData();
                                            downloadAudioFile();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.BUTTON_LATER), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            updateData();

                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        else if (response.toUpperCase().contains("EMPTY_NUMBER")) {
                            String title="";
                            String message="";
                            subscriberId="0";
                            updateServiceProviderData();
                            if(isMobileDataEnable()){
                                title=getString(R.string.EMPTY_NUMBER_TITLE);

                                if(paymentOption==PaymentOption.OPTION_MOBITEL){
                                    message=getString(R.string.EMPTY_NUMBER_MESSAGE_MOBITEL);
                                } else if(paymentOption==PaymentOption.OPTION_DIALOG){
                                    message=getString(R.string.EMPTY_NUMBER_MESSAGE_DIALOG);
                                }
                                else if(paymentOption==PaymentOption.OPTION_ETISALAT){
                                    message=getString(R.string.EMPTY_NUMBER_MESSAGE_ETISALAT);
                                }
                            }else{
                                title=getString(R.string.NO_MOBILE_DATA_TITLE);
                                message=getString(R.string.NO_MOBILE_DATA_MESSAGE);

                            }



                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(title).setMessage(message).setPositiveButton(
                                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // FIRE ZE MISSILES!
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else{
                            Log.v("addToBillServerConnect","addToBillServerConnect 4");

                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(getString(R.string.SERVER_ERROR_TITLE)).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // FIRE ZE MISSILES!
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }

                        Log.v("addToBillServerConnect","addToBillServerConnect 5");

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Log.v("addToBillServerConnect", "addToBillServerConnect 6");

                AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });



        Log.v("addToBillServerConnect", "addToBillServerConnect 7");

        AppController.getInstance().addToRequestQueue(stringRequest, "tag_mobitel_payment");
    }
    private void addToMobitelBill(){
        Log.v("addToMobitelBill","addToMobitelBill 1");

        if(AppController.getInstance().isUserLogin()){
            Log.v("addToMobitelBill","addToMobitelBill 2");

            if (connectionDetector.isConnectingToInternet()) {
                Log.v("addToMobitelBill","addToMobitelBill 3");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if(prefs.getBoolean(KEY_TERMS_ACCEPTED_FOR_MOBITEL, false)) {
                    Log.v("addToMobitelBill","addToMobitelBill 4");

                    AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                    builder.setTitle("Confirm Payment").setMessage("Rs." + audioBook.getPrice() + " will be added to your Mobitel bill. Continue?").setPositiveButton(
                            getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    addToBillServerConnect();
                                }
                            })
                            .setNegativeButton(getString(R.string.BUTTON_CANCEL), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    showTermsAndCondition();
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


        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }
    private void addToEtisalatBill(){

        if(AppController.getInstance().isUserLogin()){

            if (connectionDetector.isConnectingToInternet()) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if(prefs.getBoolean(KEY_TERMS_ACCEPTED_FOR_ETISALAT, false)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                    //Confirm Payment

                    builder.setTitle("Confirm Payment").setMessage("Rs." + audioBook.getPrice() + " will be added to your Etisalat bill. Continue?").setPositiveButton(
                            getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    addToBillServerConnect();
                                }
                            })
                            .setNegativeButton(getString(R.string.BUTTON_CANCEL), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    showTermsAndCondition();
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


        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }



    private void addToDialogBill(){
        if(AppController.getInstance().isUserLogin()){

            if (connectionDetector.isConnectingToInternet()) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if(prefs.getBoolean(KEY_TERMS_ACCEPTED_FOR_DIALOG, false)) {





                    AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                    //Confirm Payment

                    builder.setTitle("Confirm Payment").setMessage("Rs." + audioBook.getPrice() + " will be added to your Dialog bill. Continue?").setPositiveButton(
                            getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(
                                            getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                                    String provider = sharedPref.getString(getString(R.string.service_provider),"");

                                    if(provider.equalsIgnoreCase(subscriberId) || dialogNo.length()>10) {
                                    addToBillServerConnect();
                                }else{
                                    getDialogMobileNumber();
                                }
                                }
                            })
                            .setNegativeButton(getString(R.string.BUTTON_CANCEL), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }else{
                    showTermsAndCondition();
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


        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }

    }
    private void startTimer(){
        if( timerUpdateThread != null) {
            timerUpdateThread.interrupt();
        }
        timerUpdateThread = new Thread( this );
        timerUpdateThread.start();
    }
    private void stopTimer(){
        if( timerUpdateThread != null ) {
            timerUpdateThread.interrupt();
        }
    }

    private void releaseMediaPlayer(){
        if (mediaPlayer != null){
            if(mediaPlayer.isPlaying())
                mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;

        }
        stopTimer();
    }


    private void initActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }

    }
    private void postponeTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        } else {
            ActivityCompat.postponeEnterTransition(this);
        }
    }

    private void startPostponedTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startPostponedEnterTransition();
        } else {
            ActivityCompat.startPostponedEnterTransition(this);
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
    private void saveCoverImage() {
        String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                + audioBook.getBook_id()+File.separator;
        bookCoverImage.buildDrawingCache();
        Bitmap bitmapImage=bookCoverImage.getDrawingCache();
        //Bitmap resized = Bitmap.createScaledBitmap(bitmapImage, (int)(160*2), (int)(240*2), true);

        OutputStream fOut = null;
        Uri outputFileUri;
        try {
            File fileDir = new File(dirPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();

            }
            File filepath = new File(fileDir, "book_cover.jpg");

            FileOutputStream fos = null;
            try {

                fos = new FileOutputStream(filepath);

                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        int currentPosition = 0;//
        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < mediaPlayer.getDuration()) {
            try {
                Thread.sleep(1000);
                currentPosition = mediaPlayer.getCurrentPosition();

            } catch (Exception e) {
                e.printStackTrace();
            }
            updateTimer();
        }
    }
    private void updateTimer() {
        int currentPosition = mediaPlayer.getCurrentPosition();
        int totalDuration =mediaPlayer.getDuration();
        leftTime= AppUtils.milliSecondsToTimer(totalDuration - currentPosition);
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(this.getMainLooper());

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                updatePreviewLayout();

            } // This is your code
        };
        mainHandler.post(timerRunnable);
    }

    @Override
    public void onPostExecute(String result,String file_name) {
        if(!isFinishing()) {
            Log.v("onPostExecute", "onPostExecute" + file_name + "result" + result);
            if (result != null && result.equalsIgnoreCase("UNAUTHORISED")) {
                showMessage("UNAUTHORISED");

            } else if (result != null && result.equalsIgnoreCase("NOTFOUND")) {
                showMessage("NOTFOUND");

            } else {
                mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());

                downloadedFileCount++;
                if (result == null) {
                    updateAudioBook(Integer.parseInt(file_name));

                    if (totalAudioFileCount == downloadedFileCount) {
                        downloadAudioFile();
                    }
                }
            }
        }
    }


    private void showMessage(String result){
        stopDownload();
        mProgressDialog.dismiss();

        if (result.equalsIgnoreCase("UNAUTHORISED")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.USER_UNAUTHORISED_TITLE).setMessage(getString(R.string.USER_UNAUTHORISED_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

        }else if(result.equalsIgnoreCase("NOTFOUND")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void initiatePopupWindow(float rating) {
        try {
// We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater) AudioBookDetailActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rate_book_popup,
                    (ViewGroup) findViewById(R.id.popup_element));
            pwindo = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,true);

            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
            final RatingBar ratingBar = (RatingBar) layout.findViewById(R.id.rating_bar);
            LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
            stars.getDrawable(2).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);

            final TextView reviewTitle = (TextView) layout.findViewById(R.id.review_title);
            final TextView reviewDescription = (TextView) layout.findViewById(R.id.review_description);
            ratingBar.setRating(rating);

            Button btnClosePopup = (Button) layout.findViewById(R.id.btn_submit);
            btnClosePopup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    publishUserReview(ratingBar.getRating(),reviewTitle.getText().toString(),reviewDescription.getText().toString());

                }
            });
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pwindo.dismiss();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void updateReview(float rate,String title,String comment){
        ArrayList<BookReview> reviews=audioBook.getReviews();
        BookReview bookReview=new BookReview();
        bookReview.setRateValue("" + rate);
        bookReview.setTitle(title);
        bookReview.setMessage(comment);
        bookReview.setUserId(AppController.getInstance().getUserId());
        bookReview.setUserName(AppController.getInstance().getUserName());

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
        bookReview.setTimeString(dateFormat.format(date));


        reviews.add(0,bookReview);
        audioBook.setReviews(reviews);
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getApplicationContext());
        downloadedAudioBook.addBookToList(getApplicationContext(),
                audioBook.getBook_id(), audioBook);

        updateData();
    }
    private void publishUserReview( final float rate, final String title, final String comment){


        if (connectionDetector.isConnectingToInternet()) {
            if (rate > 0 && title.length() > 0 && comment.length() > 0) {

                progressDialog.setMessage("Publishing...");
                progressDialog.show();

                Map<String, String> params = new HashMap<String, String>();
                params.put("userid", AppController.getInstance().getUserId());
                params.put("bookid", audioBook.getBook_id());
                params.put("rate", "" + rate);
                params.put("title", title);
                params.put("comment", comment);

                String url = getResources().getString(R.string.add_review_url);

                JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                pwindo.dismiss();
                                progressDialog.dismiss();
                                updateReview(rate, title, comment);

                                Log.v("response", "response:" + response);
                                Toast toast = Toast.makeText(getApplicationContext(), R.string.REVIEW_PUBLISH_SUCCESS, Toast.LENGTH_LONG);
                                toast.show();


                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pwindo.dismiss();
                        progressDialog.dismiss();

                        Toast toast = Toast.makeText(getApplicationContext(), "Review publish failed try again later", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });

                AppController.getInstance().addToRequestQueue(stringRequest, "tag_review_book");
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_valid_data), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_ENOUGH_SPACE_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("onActivityResult","onActivityResult");
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if(resultCode == Constants.RESULT_SUCCESS){
                if(paymentOption == PaymentOption.OPTION_NONE){
                    playGetButtonPressed();
                }else if(paymentOption == PaymentOption.OPTION_MOBITEL){
                    addToMobitelBill();

                }else if(paymentOption == PaymentOption.OPTION_ETISALAT){
                    addToEtisalatBill();

                }else if(paymentOption == PaymentOption.OPTION_DIALOG){
                    addToDialogBill();

                }else if(paymentOption == PaymentOption.OPTION_CARD){
                    buyFromCardButtonPressed();

                }
            }
            if (resultCode == Constants.RESULT_ERROR) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
        else if (requestCode == 2) {
            if(resultCode == Constants.RESULT_SUCCESS){
                audioBook.setPurchase(true);
                updateAudioBook(0);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.PAYMENT_COMPLETE_TITLE).setMessage(getString(R.string.PAYMENT_COMPLETE_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_NOW, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                updateData();
                                downloadAudioFile();
                            }
                        })
                        .setNegativeButton(R.string.BUTTON_LATER, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();


            }else if (resultCode == Constants.RESULT_SUCCESS_ALREADY) {
                Log.v("addToBillServerConnect","addToBillServerConnect 3");

                audioBook.setPurchase(true);
                updateAudioBook(0);
                AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                builder.setTitle(getString(R.string.ALREADY_PAID_TITLE)).
                        setMessage(getString(R.string.ALREADY_PAID_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                updateData();
                                downloadAudioFile();
                            }
                        })
                        .setNegativeButton(getString(R.string.BUTTON_LATER), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            if (resultCode == Constants.RESULT_ERROR) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

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
            if(AudioPlayerService.mediaPlayer!=null && AudioPlayerService.mediaPlayer.isPlaying()) {
                releaseMediaPlayer();
            }
        }
    };

    private class DownloadReceiver extends ResultReceiver {
        public DownloadReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == DownloadService.UPDATE_PROGRESS) {
                Log.v("DownloadService","DownloadService :"+resultData);
                int progress = resultData.getInt("progress");


                String result=resultData.getString("result");
                String file_name=resultData.getString("file_name");


                    if (result != null && result.equalsIgnoreCase("UNAUTHORISED")){
                        showMessage("UNAUTHORISED");

                    }else if(result != null && result.equalsIgnoreCase("NOTFOUND")){
                        showMessage("NOTFOUND");

                    }else if(result != null && result.equalsIgnoreCase("OK")){
                        Log.v("DownloadService","DownloadService getDownloadedChapter :"+audioBook.getDownloadedChapter().size());

                        mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());

                        downloadedFileCount++;
                       // if (result == null) {
                            updateAudioBook(Integer.parseInt(file_name));

                            if (totalAudioFileCount == downloadedFileCount) {
                                updateData();
                                downloadAudioFile();
                            }
                       // }
                    }
            }
        }
    }
    private void pausePlayer() {
        Intent intent = new Intent(Constants.PLAYER_STATE_CHANGE);
        intent.putExtra("state", "pause");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
    @Override
    public void authorizationCodeResponse(String state, String authorizationCode, String error, String clientId, String clientSecret, String scopes, String redirectUri) {
        if(authorizationCode.equalsIgnoreCase("0")){
            if(state !=null && state.length()>10){
                dialogNo=state;
                addToBillServerConnect();
            }else{
                progressDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                builder.setTitle(R.string.EMPTY_NUMBER_TITLE).setMessage(R.string.EMPTY_NUMBER_MESSAGE_DIALOG).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

            }


        }else{
            progressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
            builder.setTitle(R.string.EMPTY_NUMBER_TITLE).setMessage(R.string.EMPTY_NUMBER_MESSAGE_DIALOG).setPositiveButton(
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
    public void authorizationError(String reason) {

    }

}
