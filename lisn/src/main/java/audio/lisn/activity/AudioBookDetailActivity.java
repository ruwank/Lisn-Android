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
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
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
import com.philjay.valuebar.ValueBar;
import com.philjay.valuebar.colors.BarColorFormatter;
import com.philjay.valuebar.colors.RedToGreenFormatter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import audio.lisn.R;
import audio.lisn.adapter.BookChapterListAdapter;
import audio.lisn.adapter.BookReviewViewAdapter;
import audio.lisn.app.AppController;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.Authorization;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.AuthorizationListener;
import audio.lisn.appsupport.gsma.android.mobileconnect.authorization.AuthorizationOptions;
import audio.lisn.appsupport.gsma.android.mobileconnect.values.Prompt;
import audio.lisn.appsupport.gsma.android.mobileconnect.values.ResponseType;
import audio.lisn.model.AudioBook;
import audio.lisn.model.AudioBook.PaymentOption;
import audio.lisn.model.AudioBook.ServiceProvider;
import audio.lisn.model.BookChapter;
import audio.lisn.model.BookReview;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.Analytic;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.CustomTypeFace;
import audio.lisn.util.Log;
import audio.lisn.util.OnSwipeTouchListener;
import audio.lisn.util.WCLinearLayoutManager;
import audio.lisn.view.ExpandablePanel;
import audio.lisn.view.ExpandableTextView;
import audio.lisn.view.PlayerControllerView;
import audio.lisn.webservice.FileDownloadTask;
import audio.lisn.webservice.FileDownloadTaskListener;
import audio.lisn.webservice.JsonUTF8StringRequest;
import de.hdodenhof.circleimageview.CircleImageView;


public class AudioBookDetailActivity extends  AppCompatActivity implements FileDownloadTaskListener,AuthorizationListener, BookChapterListAdapter.BookChapterSelectListener {

    private static final String TRANSITION_NAME = "audio.lisn.AudioBookDetailActivity";
    public static final String TAG = AudioBookDetailActivity.class.getSimpleName();

    AudioBook audioBook;
    ConnectionDetector connectionDetector;
    public ProgressBar spinner;
    ProgressDialog mProgressDialog;
    ProgressDialog progressDialog;
   // int totalAudioFileCount, downloadedFileCount;
    List<FileDownloadTask> downloadingList = new ArrayList<FileDownloadTask>();
    ImageView bookCoverImage;
    ImageView bookCoverImageBack;
    private PopupWindow pwindo,paymentOptionView;
    int previousDownloadedFileCount;
    Button btnDownload,btnCoupon;
    PlayerControllerView playerControllerView;
    View topOverLayView;
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

    boolean isSelectChapterBuyOption;
    BookChapter selectedChapter;
    ServiceProvider  serviceProvider;
    PaymentOption  paymentOption;
    Thread timerUpdateThread;
    String subscriberId;
    ListView chapterListView ;
    int selectedChapterIndex ;
    BookChapterListAdapter bookChapterListAdapter;
    @Override
    public void onBookChapterSelect(BookChapter bookChapter, int index,AudioBook.SelectedAction action) {

        isSelectChapterBuyOption=true;
        selectedChapter=bookChapter;
        selectedChapterIndex=index;

        if(action == AudioBook.SelectedAction.ACTION_PURCHASE){
            new Analytic().analyticEvent(4, audioBook.getBook_id(), ""+selectedChapter.getChapter_id());

            playGetButtonPressed();
        }else if(action == AudioBook.SelectedAction.ACTION_PLAY){
            PlayerControllerActivity.navigate(AudioBookDetailActivity.this, bookCoverImage, audioBook,index);

        }else if(action == AudioBook.SelectedAction.ACTION_DOWNLOAD){
            paymentOption = PaymentOption.OPTION_NONE;
            playGetButtonPressed();

        }

    }

    public static void navigate(AppCompatActivity activity, View transitionImage, AudioBook audioBook) {
        Intent intent = new Intent(activity, AudioBookDetailActivity.class);
        intent.putExtra("audioBook", audioBook);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage,activity.getString(R.string.transition_book));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_audio_book_detail);
        findServiceProvider();
        callbackManager = CallbackManager.Factory.create();
        dialogNo="";
        //downloadedFileCount=0;
        audioBook = (AudioBook) getIntent().getSerializableExtra("audioBook");

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String itemTitle = audioBook.getEnglish_title();
        getSupportActionBar().setTitle(itemTitle);

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
                                playerControllerView.stopAudioPlayer();
                            }
                        });

            }

            @Override
            public void onSingleTap() {
                showAudioPlayer();
            }

        });
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


        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        btnDownload = (Button) findViewById(R.id.btnDownload);
         btnCoupon = (Button) findViewById(R.id.btnCoupon);

        bookCoverImage = (ImageView) findViewById(R.id.bookCoverImage);
        RelativeLayout.LayoutParams bookCoverImageLayoutParams =
                (RelativeLayout.LayoutParams) bookCoverImage.getLayoutParams();
        bookCoverImageLayoutParams.width = (int) ((size.x - 60) / 3);

        bookCoverImage.setLayoutParams(bookCoverImageLayoutParams);
        topOverLayView = (View) findViewById(R.id.topOverLayView);

        bookCoverImageBack = (ImageView) findViewById(R.id.bookCoverImageBack);

        btnCoupon.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                btnCoupon.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                int[] loc = new int[2];
                btnCoupon.getLocationOnScreen(loc);

                final int viewHeight = loc[1];
                RelativeLayout.LayoutParams bookCoverImageBackLayoutParams =
                        (RelativeLayout.LayoutParams) bookCoverImageBack.getLayoutParams();
                bookCoverImageBackLayoutParams.height = viewHeight;
                bookCoverImageBack.setLayoutParams(bookCoverImageBackLayoutParams);
                topOverLayView.setLayoutParams(bookCoverImageBackLayoutParams);
            }
        });

        //Analytic activity
        new Analytic().analyticEvent(3, audioBook.getBook_id(), "");

    }
    @Override
    protected void onResume() {
        super.onResume();
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(this);
        HashMap<String, AudioBook> hashMap = downloadedAudioBook.getBookList(this);


        AudioBook returnBook = hashMap.get(audioBook.getBook_id());
        if(returnBook !=null && returnBook.getBook_id() !=null){
             audioBook=returnBook;
        }
        updateData();

        if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.hasStartedPlayer){
            playerControllerView.setVisibility(View.VISIBLE);
        }else{
            playerControllerView.setVisibility(View.INVISIBLE);

        }
        playerControllerView.updateView();
        registerPlayerUpdateBroadcastReceiver();
        bookReviewViewAdapter.notifyDataSetChanged();
        bookChapterListAdapter.notifyDataSetChanged();
    }
    @Override
    public void onPause() {
        super.onPause();

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

//                AlertDialog.Builder builder = new AlertDialog.Builder(
//                        this);
//                builder.setTitle("Service Provider").setMessage("Mobitel").setPositiveButton(
//                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                // FIRE ZE MISSILES!
//                            }
//                        });
//                AlertDialog dialog = builder.create();
//                dialog.show();

                serviceProvider = ServiceProvider.PROVIDER_MOBITEL;
            } else if (subscriberId.startsWith("41302")) {
                serviceProvider = ServiceProvider.PROVIDER_DIALOG;

//                AlertDialog.Builder builder = new AlertDialog.Builder(
//                        this);
//                builder.setTitle("Service Provider").setMessage("Dialog").setPositiveButton(
//                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                // FIRE ZE MISSILES!
//                            }
//                        });
//                AlertDialog dialog = builder.create();
//                dialog.show();
            }


        }else {
//            AlertDialog.Builder builder = new AlertDialog.Builder(
//                    this);
//            builder.setTitle("Service Provider").setMessage("getSubscriber error").setPositiveButton(
//                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            // FIRE ZE MISSILES!
//                        }
//                    });
//            AlertDialog dialog = builder.create();
//            dialog.show();
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
        //AudioBook audioBook=AppController.getInstance().getCurrentAudioBook();
        Log.v(TAG, "SeekPoint" + audioBook.getLastSeekPoint());
        if(AppController.getInstance().getCurrentAudioBook() !=null) {
            PlayerControllerActivity.navigate(AudioBookDetailActivity.this, playerControllerView, null, -1);
        }

    }
    private void showBookReview(){
        BookReviewActivity.navigate(this, playerControllerView, audioBook.getReviews());

    }
    private void updateData() {



        Button addToBillButton = (Button) findViewById(R.id.addToBillButton);
        Button btnPayFromCard = (Button) findViewById(R.id.buyFromCardButton);



        String img_path = AppUtils.getDataDirectory(this)
                + audioBook.getBook_id()+File.separator+"book_cover.jpg";


        File imgFile = new  File(img_path);


        if(imgFile.exists()){
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), bmOptions);

            bookCoverImageBack.setImageBitmap(bitmap);
            bookCoverImage.setImageBitmap(bitmap);
            if (Build.VERSION.SDK_INT >= 18) {
                Bitmap blurred = AppUtils.blurRenderScript(bitmap, 5, getApplicationContext());//second parametre is radius
                bookCoverImageBack.setImageBitmap(blurred);
            }

        }else {

            Picasso.with(this)
                    .load(audioBook.getCover_image())
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            /* Save the bitmap or do something with it here */

                            if(audioBook.isPurchase()){
                                saveCoverImage(bitmap);
                            }
                            //Set it in the ImageView
                            bookCoverImageBack.setImageBitmap(bitmap);
                            bookCoverImage.setImageBitmap(bitmap);
                            if (Build.VERSION.SDK_INT >= 18) {

                                Bitmap blurred = AppUtils.blurRenderScript(bitmap, 5, getApplicationContext());//second parametre is radius
                                bookCoverImageBack.setImageBitmap(blurred);
                            }
                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {

                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {

                        }
                    });
        }


        String narratorText = "";
        String durationText = "";
        TextView title = (TextView) findViewById(R.id.title);

        ExpandableTextView description = (ExpandableTextView) findViewById(R.id.description);
        TextView descriptionTextView = (TextView) findViewById(R.id.expandable_text);


        TextView author = (TextView) findViewById(R.id.author);
        TextView narrator = (TextView) findViewById(R.id.narrator);
        TextView bookPrice = (TextView) findViewById(R.id.book_price);

        LinearLayout rateLayout = (LinearLayout) findViewById(R.id.app_rate_layout);

        View separator_top_description = (View) findViewById(R.id.separator_top_description);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentOption = PaymentOption.OPTION_NONE;
                isSelectChapterBuyOption=false;
                playGetButtonPressed();

            }
        });



        addToBillButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSelectChapterBuyOption=false;
                addToMyBillButtonPressed();
            }
        });

        btnPayFromCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentOption = PaymentOption.OPTION_CARD;
                isSelectChapterBuyOption=false;
                buyFromCardButtonPressed();
            }
        });
        btnCoupon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnCouponClicked();
            }
        });


        TextView rateValue1=(TextView)findViewById(R.id.rateValue1);
        TextView rateValue2=(TextView)findViewById(R.id.rateValue2);
        if(audioBook.getReviews() != null) {
            rateValue1.setText(""+audioBook.getReviews().size());
            rateValue2.setText(""+audioBook.getReviews().size());


        }
        if(audioBook.getContent_rate() != null ){
            //G, PG, M, MA15+, R18+
            if(audioBook.getContent_rate().equalsIgnoreCase("G")){

            }
            else if(audioBook.getContent_rate().equalsIgnoreCase("PG")){

            }
            else if(audioBook.getContent_rate().equalsIgnoreCase("M")){

            }
            else if(audioBook.getContent_rate().equalsIgnoreCase("MA15+")){

            }
            else if(audioBook.getContent_rate().equalsIgnoreCase("R18+")){

            }

        }

        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating_bar);
        // LayerDrawable stars1 = (LayerDrawable) ratingBar.getProgressDrawable();
        //stars1.getDrawable(2).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);


        RatingBar userRatingBar = (RatingBar) findViewById(R.id.user_rate_bar);
        LayerDrawable stars = (LayerDrawable) userRatingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);

        userRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {

            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (fromUser) {
                    initiatePopupWindow(rating);
                }

            }

        });

        if(audioBook.getRatingMap() != null && audioBook.getRatingMap().size()>0) {


            ValueBar valueBar5 = (ValueBar) findViewById(R.id.valueBar5);
            ValueBar valueBar4 = (ValueBar) findViewById(R.id.valueBar4);
            ValueBar valueBar3 = (ValueBar) findViewById(R.id.valueBar3);
            ValueBar valueBar2 = (ValueBar) findViewById(R.id.valueBar2);
            ValueBar valueBar1 = (ValueBar) findViewById(R.id.valueBar1);
            valueBar5.setVisibility(View.VISIBLE);
            valueBar4.setVisibility(View.VISIBLE);
            valueBar3.setVisibility(View.VISIBLE);
            valueBar2.setVisibility(View.VISIBLE);
            valueBar1.setVisibility(View.VISIBLE);

            setPropertyToValueBar(valueBar5);
            valueBar5.setValue(audioBook.getRatingMap().get(5)); // display a value

            valueBar5.setColorFormatter(new BarColorFormatter() {
                @Override
                public int getColor(float v, float v1, float v2) {
                    return Color.rgb(138, 194, 73);
                }
            });

            setPropertyToValueBar(valueBar4);
            valueBar4.setValue(audioBook.getRatingMap().get(4)); // display a value
            valueBar4.setColorFormatter(new BarColorFormatter() {
                @Override
                public int getColor(float v, float v1, float v2) {
                    return Color.rgb(204, 219, 56);
                }
            });

            setPropertyToValueBar(valueBar3);
            valueBar3.setValue(audioBook.getRatingMap().get(3)); // display a value
            valueBar3.setColorFormatter(new BarColorFormatter() {
                @Override
                public int getColor(float v, float v1, float v2) {
                    return Color.rgb(255, 234, 58);
                }
            });

            setPropertyToValueBar(valueBar2);
            valueBar2.setValue(audioBook.getRatingMap().get(2)); // display a value
            valueBar2.setColorFormatter(new BarColorFormatter() {
                @Override
                public int getColor(float v, float v1, float v2) {
                    return Color.rgb(255, 178, 51);
                }
            });

            setPropertyToValueBar(valueBar1);
            valueBar1.setValue(audioBook.getRatingMap().get(1)); // display a value
            valueBar1.setColorFormatter(new BarColorFormatter() {
                @Override
                public int getColor(float v, float v1, float v2) {
                    return Color.rgb(255, 139, 90);
                }
            });

        }

        // Get ListView object from xml
        chapterListView = (ListView) findViewById(R.id.chapter_list);


        Log.v(TAG, "" + audioBook.getChapters().size());
        ExpandablePanel expandablePanel = (ExpandablePanel) findViewById(R.id.buy_chapter);

        if (audioBook.getChapters().size()>0){
            bookChapterListAdapter = new BookChapterListAdapter(this, audioBook);

            bookChapterListAdapter.bookChapterViewSelectListener(this);
            // Assign adapter to ListView
            chapterListView.setAdapter(bookChapterListAdapter);
            expandablePanel.setContentHeight(getListViewHeightBasedOnChildren(chapterListView));
            expandablePanel.requestLayout();

        }else
        {
            expandablePanel.setVisibility(View.GONE);

        }

        spinner = (ProgressBar)findViewById(R.id.progressBar);


        TextView reviewRatingValue=(TextView)findViewById(R.id.reviewRatingValue);
        RatingBar reviewRatingBar=(RatingBar)findViewById(R.id.reviewRatingBar);

        TextView ratingValue2=(TextView)findViewById(R.id.ratingValue2);
        RatingBar ratingBar2=(RatingBar)findViewById(R.id.ratingBar2);
String narratorContentDes="";

        if(audioBook.getLanguageCode()== AudioBook.LanguageCode.LAN_SI){
            descriptionTextView.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            title.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            author.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            narrator.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            narratorText=getString(R.string.narrator_si);
            durationText=getString(R.string.duration_si);
            narratorContentDes="Kiyaweema ";
        }else{
            descriptionTextView.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            title.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            author.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            narrator.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));

            narratorText=getString(R.string.narrator_en);
            durationText=getString(R.string.duration_en);
            narratorContentDes=getString(R.string.narrator_en);

        }
        title.setText(audioBook.getTitle());
        String priceText="Free";
        if( Float.parseFloat(audioBook.getPrice())>0 ){
            priceText="Rs. "+audioBook.getPrice();
        }
        bookPrice.setText(priceText);
        if(Float.parseFloat(audioBook.getRate())>-1){
            ratingBar.setRating(Float.parseFloat(audioBook.getRate()));
            // ratingValue.setText(String.format("%.1f", Float.parseFloat(audioBook.getRate())));

            reviewRatingBar.setRating(Float.parseFloat(audioBook.getRate()));
            reviewRatingValue.setText(String.format("%.1f", Float.parseFloat(audioBook.getRate())));

            ratingBar2.setRating(Float.parseFloat(audioBook.getRate()));
            ratingValue2.setText(String.format("%.1f", Float.parseFloat(audioBook.getRate())));
        }

        narratorText=narratorText+" - "+audioBook.getNarrator();
        durationText=durationText+" - "+audioBook.getDuration();
        author.setText(audioBook.getAuthor());
        author.setContentDescription(audioBook.getAuthor_in_english());

        narrator.setText(narratorText);
        narratorContentDes=narratorContentDes+" "+audioBook.getNarrator_in_english();
        narrator.setContentDescription(narratorContentDes);

        title.setText(audioBook.getTitle());
        if(audioBook.getDescription() !=null && audioBook.getDescription().length()>1){
            description.setText(audioBook.getDescription());
            description.setVisibility(View.VISIBLE);
            separator_top_description.setVisibility(View.VISIBLE);
        }

        //Set Content description
        title.setContentDescription(audioBook.getEnglish_title());
        description.setContentDescription(audioBook.getEnglish_description());

        btnPayFromCard.setText("Pay by Card (" + audioBook.getDiscount() + "% discount)");
        btnDownload.setVisibility(View.GONE);
        addToBillButton.setVisibility(View.GONE);
        btnPayFromCard.setVisibility(View.GONE);

        if(AppController.getInstance().isUserLogin() && audioBook.isTotalBookPurchased()){
            btnDownload.setText("Download");
            btnDownload.setVisibility(View.VISIBLE);
            if (audioBook.getChapters().size() == audioBook.getDownloadedChapter().size()) {
                btnDownload.setText("Play");
            }


        }else{
            if(Float.parseFloat(audioBook.getPrice())>0) {
                btnPayFromCard.setVisibility(View.VISIBLE);

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


            }else{
                btnDownload.setText("Download");
                btnDownload.setVisibility(View.VISIBLE);

            }

        }
        rateLayout.setVisibility(View.GONE);

        if(AppController.getInstance().isUserLogin() && audioBook.isPurchase()){
            rateLayout.setVisibility(View.VISIBLE);
            userRatingBar.setRating(0);

            TextView user_name=(TextView)findViewById(R.id.user_name);
            user_name.setText(AppController.getInstance().getUserName());

            CircleImageView profileImage=(CircleImageView)findViewById(R.id.profile_image);


            Log.v("profileImageUrl", "fb id :" + AppController.getInstance().getFbId());

            if(AppController.getInstance().getFbId() != null && AppController.getInstance().getFbId().length()>2){

                String profileImageUrl=getString(R.string.fb_profile_picture_url);
                profileImageUrl=profileImageUrl+AppController.getInstance().getFbId()+"/picture";
                Log.v("profileImageUrl", "profileImageUrl :" + profileImageUrl);
                Log.v("profileImageUrl", "fb id :" + AppController.getInstance().getFbId());

                Picasso.with(profileImage.getContext())
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_profile_default)
                        .into(profileImage);
            }
        }



        LinearLayout shareLayout=(LinearLayout)findViewById(R.id.shareLayout);
        shareLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBook();
            }
        });

        LinearLayout similarLayout=(LinearLayout)findViewById(R.id.similarLayout);
        similarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSimilarBook();
            }
        });


        int reviewsCount=0;

        if(audioBook.getReviews() !=null)
            reviewsCount=audioBook.getReviews().size();
        final int finalReviewsCount = reviewsCount;


        LinearLayout reviewLayout=(LinearLayout)findViewById(R.id.reviewLayout);
        reviewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (finalReviewsCount > 0) {
                    showBookReview();

                }


            }
        });


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


    }
    private void btnCouponClicked(){
if(connectionDetector.isConnectingToInternet()){
        if(AppController.getInstance().isUserLogin()){
            showCouponEnterScreen();
        }else{

            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 3);

        }
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
    private void showCouponEnterScreen(){
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.custom_dialog_coupon, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                //result.setText(userInput.getText());
                                if(userInput.getText() != null && userInput.getText().toString().length()>0) {
                                    String coupon = userInput.getText().toString();
                                    callCouponService(coupon);
                                    dialog.cancel();
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
    private void callCouponService(String code){
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", AppController.getInstance().getUserId());
        params.put("code", code);



        String url = getResources().getString(R.string.coupon_service_url);

        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url,params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v(TAG, " downloader response:" + response);

                        progressDialog.dismiss();
                        if (response.toUpperCase().contains("SUCCESS")) {

                            String[] separated = response.split(":");
                            String discountString = separated[1];
                            if(separated[1] != null){
                                String[] discountSeparated = discountString.split("=");
                                if(discountSeparated[1] != null){
                                    String discountValue = discountSeparated[1];
                                    audioBook.setApplyCoupon(true);
                                    audioBook.setCouponDiscount(Double.parseDouble(discountValue));

                                }

                            }
                            showPaymentOptionPopupWindow();
                        }else{
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

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                progressDialog.dismiss();
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


        AppController.getInstance().addToRequestQueue(stringRequest, "tag_download_book");

        //http://app.lisn.audio/api/1.4/coupon.php?userid=1&amp;code=abc
    }
    private void showSimilarBook(){
        new Analytic().analyticEvent(5, audioBook.getBook_id(), "");

        Intent intent = new Intent(this,
                CategoryBookActivity.class);
        Log.v(TAG,audioBook.getCategory());
        intent.putExtra(Constants.BOOK_ID,audioBook.getBook_id() );
        startActivity(intent);
    }
    private int getListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return 0;
        }

        int totalHeight = listView.getPaddingTop() + listView.getPaddingBottom();
        Log.v("totalHeight","totalHeight"+totalHeight);
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);

            if(listItem != null){
                // This next line is needed before you call measure or else you won't get measured height at all. The listitem needs to be drawn first to know the height.
                listItem.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                Log.v("totalHeight", "listItem.getMeasuredHeight()" + listItem.getMeasuredHeight());

                totalHeight += listItem.getMeasuredHeight();

            }
        }

        return totalHeight;
    }
    private void stopDownload(){
        for (int i = 0; i < downloadingList.size(); i++) {
            FileDownloadTask downloadTask = downloadingList.get(i);
            downloadTask.cancel(true);

        }
    }

    private ValueBar setPropertyToValueBar(ValueBar valueBar){

        int maxValue =Collections.max(audioBook.getRatingMap().values());
        Log.v(TAG,"maxValue :"+maxValue);
        valueBar.setMinMax(0, maxValue);
        valueBar.setInterval(1f); // interval in which can be selected
        valueBar.setDrawBorder(false);
        valueBar.setTouchEnabled(false);
        valueBar.setDrawValueText(false);
        valueBar.setDrawMinMaxText(false);
        valueBar.setColorFormatter(new RedToGreenFormatter());
        return valueBar;

    }



    private void updateAudioBook(int chapter){
        if(chapter>0) {
            audioBook.addChapterToDownloadedChapter(chapter);
        }
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getApplicationContext());
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

        if(isSelectChapterBuyOption){
            params.put("chapid", ""+selectedChapter.getChapter_id());
        }else{
            params.put("chapid", "0");
        }

        String url = getResources().getString(R.string.user_download_activity_url);

        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url,params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v(TAG, " downloader response:" + response);

                        audioBook.setPurchase(true);
                        if(isSelectChapterBuyOption){
                            selectedChapter.setIsPurchased(true);
                        }else{
                            audioBook.setIsTotalBookPurchased(true);
                        }
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
            if(isSelectChapterBuyOption){
                mProgressDialog.setMessage("Downloading " + selectedChapter.getEnglish_title());

            }else{
                mProgressDialog.setMessage("Downloading Chapter " + filePart);

            }


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
            // previousDownloadedFileCount = downloadedFileCount;
            String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                    + audioBook.getBook_id() + File.separator;
            File fileDir = new File(dirPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();

            }

            if (connectionDetector.isConnectingToInternet()) {

                mProgressDialog.show();
                downloadingList.clear();
                boolean isDownloading=false;
                String chapterName="";


                if(isSelectChapterBuyOption){
                    File file = new File(dirPath + selectedChapter.getChapter_id() + ".lisn");

                    if (!file.exists() || !(audioBook.getDownloadedChapter().contains(selectedChapter.getChapter_id()))) {
                        downloadAudioFileFromUrl(selectedChapter.getChapter_id());
                        isDownloading = true;
                        chapterName=selectedChapter.getEnglish_title();
                    }
                }else {
                    for (int index = 0; index < (audioBook.getChapters().size()); index++) {
                        BookChapter bookChapter=audioBook.getChapters().get(index);
                        File file = new File(dirPath + bookChapter.getChapter_id() + ".lisn");

                        if (AppController.getInstance().isUserLogin() && file.exists() && (audioBook.getDownloadedChapter().contains(bookChapter.getChapter_id()))) {
                            isDownloading = false;
                            // totalAudioFileCount++;
                        }else{
                            downloadAudioFileFromUrl(bookChapter.getChapter_id());
                            isDownloading = true;
                            chapterName=bookChapter.getEnglish_title();
                            break;
                        }

                    }
                }
                if (!isDownloading) {
                    if ( mProgressDialog!=null && mProgressDialog.isShowing() ){
                        mProgressDialog.dismiss();
                    }
                    starAudioPlayer();


                } else {
                    if (AppUtils.getAvailableMemory() < audioBook.getFileSize()) {
                        stopDownload();


                        if (!isFinishing()) {

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

                        }


                    } else {

                        mProgressDialog.setMessage("Downloading " +chapterName);
                    }
                }

            } else {

                boolean needDownload=false;
                if(isSelectChapterBuyOption){
                    File file = new File(dirPath + selectedChapter.getChapter_id() + ".lisn");

                    if (!file.exists() || !(audioBook.getDownloadedChapter().contains(selectedChapter.getChapter_id()))) {
                        needDownload=true;
                    }
                }else {
                    for (int index = 0; index < (audioBook.getChapters().size()); index++) {
                        BookChapter bookChapter = audioBook.getChapters().get(index);

                        File file = new File(dirPath + bookChapter.getChapter_id() + ".lisn");
                        if (!file.exists() && !(audioBook.getDownloadedChapter().contains(bookChapter.getChapter_id()))) {

                           // if (!file.exists()) {
                            needDownload = true;
                            break;
                        }


                    }
                }
                if (!needDownload) {
                    if ( mProgressDialog!=null && mProgressDialog.isShowing() ){
                        mProgressDialog.dismiss();
                    }
                    starAudioPlayer();
                } else {
                    if (!isFinishing()) {

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
    }

    private void starAudioPlayer() {
        if (previousDownloadedFileCount == 0) {
            if ( mProgressDialog!=null && mProgressDialog.isShowing() ){
                mProgressDialog.dismiss();
            }
            PlayerControllerActivity.navigate(AudioBookDetailActivity.this, bookCoverImage, audioBook, -1);

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.DOWNLOAD_COMPLETE_TITLE).setMessage(getString(R.string.DOWNLOAD_COMPLETE_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_YES, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if ( mProgressDialog!=null && mProgressDialog.isShowing() ){
                                mProgressDialog.dismiss();
                            }
                            PlayerControllerActivity.navigate(AudioBookDetailActivity.this, bookCoverImage, audioBook,-1);

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
        new Analytic().analyticEvent(6, audioBook.getBook_id(), "");

        String shareUrl=getString(R.string.play_store_url);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT,shareUrl );
        startActivity(Intent.createChooser(intent, "Share"));
    }


    private void playGetButtonPressed(){

        if(AppController.getInstance().isUserLogin()){


            if(isSelectChapterBuyOption){
                if(selectedChapter.isPurchased()){
                    downloadAudioFile();

                }else{
                    if(selectedChapter.getPrice()>0){
                        showPaymentOptionPopupWindow();
                    }else{
                        logUserDownload();
                    }
                }

            }else {
                if (audioBook.isTotalBookPurchased()) {
                    downloadAudioFile();

                } else {
                    if (Float.parseFloat(audioBook.getPrice()) > 0) {
                        Intent intent = new Intent(this,
                                PurchaseActivity.class);
                        intent.putExtra("audioBook", audioBook);
                        startActivityForResult(intent, 2);

                    } else {
                        logUserDownload();
                    }
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
                if(isSelectChapterBuyOption){
                    intent.putExtra("isSelectChapterBuyOption", isSelectChapterBuyOption);
                    intent.putExtra("selectedChapter", selectedChapter);

                }else{
                    new Analytic().analyticEvent(4, audioBook.getBook_id(), "0");

                }
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

        if(!isSelectChapterBuyOption)
            new Analytic().analyticEvent(4, audioBook.getBook_id(), "0");

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
            String provider = sharedPref.getString(getString(R.string.service_provider), "");

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

        if(subscriberId != null) {
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putString(getString(R.string.service_provider), subscriberId);
            editor.commit();
        }
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
        http://app.lisn.audio/spgw/1.5.6/payment/init.php?userid=1&amp;bookid=1&amp;chapid=1&amp;amount=150.00
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

        if(paymentOption==PaymentOption.OPTION_DIALOG){
            params.put("number", dialogNo);
        }else{
            params.put("action", "charge");
        }
        if(isSelectChapterBuyOption){
            params.put("chapid", ""+selectedChapter.getChapter_id());
            params.put("amount", ""+selectedChapter.getPrice());
        }else {
            String amount=audioBook.getPrice();
            if(audioBook.isApplyCoupon()){
                amount = ""+(float) ((Float.parseFloat(audioBook.getPrice())) * ((100.0 - audioBook.getCouponDiscount()) / 100.0));
            }

            params.put("amount", amount);


        }



        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,true,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("addToBillServerConnect", "addToBillServerConnect 2");

                        String info="0";
                        if(isSelectChapterBuyOption)
                            info=""+selectedChapter.getChapter_id();
                        if(paymentOption == PaymentOption.OPTION_MOBITEL){
                            info=info+",mobitel";

                        }else if(paymentOption == PaymentOption.OPTION_ETISALAT){
                            info=info+",etisalat";

                        }else if(paymentOption == PaymentOption.OPTION_DIALOG){
                            info=info+",dialog";

                        }
                        progressDialog.dismiss();
                        if (response.toUpperCase().contains("SUCCESS")) {
                            Log.v("addToBillServerConnect", "addToBillServerConnect 3");


                            info=info+",success";
                            new Analytic().analyticEvent(8, audioBook.getBook_id(), info);

                            updateAudioBookSuccessPayment();
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
                            Log.v("addToBillServerConnect", "addToBillServerConnect 3");
                            info=info+",already_paid";
                            new Analytic().analyticEvent(8, audioBook.getBook_id(), info);
                            updateAudioBookSuccessPayment();

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
                            info=info+",number error";
                            new Analytic().analyticEvent(8, audioBook.getBook_id(), info);

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
                            info=info+",error";
                            new Analytic().analyticEvent(8, audioBook.getBook_id(), info);
                            String[] separated = response.split(":");
                            String status=separated[0];
                            String message=getString(R.string.SERVER_ERROR_MESSAGE_PAYMENT);
                            if(separated.length>1){
                                message=separated[1];
                            }

                            //FAILED:Insifficient credit! Please reload your mobitel account and try again!

                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(status).setMessage(message).setPositiveButton(
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
                String info="0";
                if(isSelectChapterBuyOption)
                    info=""+selectedChapter.getChapter_id();
                if(paymentOption == PaymentOption.OPTION_MOBITEL){
                    info=info+",mobitel";

                }else if(paymentOption == PaymentOption.OPTION_ETISALAT){
                    info=info+",etisalat";

                }else if(paymentOption == PaymentOption.OPTION_DIALOG){
                    info=info+",dialog";

                }
                info=info+",failed";
                new Analytic().analyticEvent(8, audioBook.getBook_id(), info);

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
                    String price= audioBook.getPrice();
                    if(isSelectChapterBuyOption){
                        price=""+selectedChapter.getPrice();
                    }
                    if(audioBook.isApplyCoupon()){
                        price = ""+(float) ((Float.parseFloat(audioBook.getPrice())) * ((100.0 - audioBook.getCouponDiscount()) / 100.0));
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                    builder.setTitle("Confirm Payment").setMessage("Rs." + price + " will be added to your Mobitel bill. Continue?").setPositiveButton(
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
                    String price= audioBook.getPrice();
                    if(isSelectChapterBuyOption){
                        price=""+selectedChapter.getPrice();
                    }
                    if(audioBook.isApplyCoupon()){
                        price = ""+(float) ((Float.parseFloat(audioBook.getPrice())) * ((100.0 - audioBook.getCouponDiscount()) / 100.0));
                    }
                    builder.setTitle("Confirm Payment").setMessage("Rs." + price + " will be added to your Etisalat bill. Continue?").setPositiveButton(
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
                    String price= audioBook.getPrice();
                    if(isSelectChapterBuyOption){
                        price=""+selectedChapter.getPrice();
                    }
                    if(audioBook.isApplyCoupon()){
                        price = ""+(float) ((Float.parseFloat(audioBook.getPrice())) * ((100.0 - audioBook.getCouponDiscount()) / 100.0));
                    }
                    builder.setTitle("Confirm Payment").setMessage("Rs." + price + " will be added to your Dialog bill. Continue?").setPositiveButton(
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
    private void updateAudioBookSuccessPayment(){
        updateServiceProviderData();

        audioBook.setPurchase(true);
        if(isSelectChapterBuyOption){
            selectedChapter.setIsPurchased(true);
            for (int i = 0; i <audioBook.getChapters().size() ; i++) {
                BookChapter bookChapter=audioBook.getChapters().get(i);
                if(bookChapter.getChapter_id() == selectedChapter.getChapter_id()){
                    bookChapter.setIsPurchased(true);
                    break;
                }
            }


        }else {
            audioBook.setIsTotalBookPurchased(true);
        }
        updateAudioBook(0);

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

        bookCoverImage.buildDrawingCache();
        Bitmap bitmapImage=bookCoverImage.getDrawingCache();
        saveCoverImage(bitmapImage);
        //Bitmap resized = Bitmap.createScaledBitmap(bitmapImage, (int)(160*2), (int)(240*2), true);


    }
    private void saveCoverImage(Bitmap bitmapImage){
        String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                + audioBook.getBook_id()+File.separator;
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
    public void onPostExecute(String result,String file_name) {
        if(!isFinishing()) {
            Log.v(TAG, "onPostExecute" + file_name + "result" + result);
            if (result != null && result.equalsIgnoreCase("UNAUTHORISED")) {
                showMessage("UNAUTHORISED");

            } else if (result != null && result.equalsIgnoreCase("NOTFOUND")) {
                showMessage("NOTFOUND");

            } else {
                if(isSelectChapterBuyOption){
                    selectedChapter.setIsPurchased(true);
                    updateAudioBook(Integer.parseInt(file_name));
                    if ( mProgressDialog!=null && mProgressDialog.isShowing() ){
                        mProgressDialog.dismiss();
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.DOWNLOAD_COMPLETE_TITLE).setMessage(getString(R.string.DOWNLOAD_COMPLETE_MESSAGE)).setPositiveButton(
                            R.string.BUTTON_YES, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    PlayerControllerActivity.navigate(AudioBookDetailActivity.this, bookCoverImage, audioBook,selectedChapterIndex);

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
                }else {
                   // mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getChapters().size());

                    //downloadedFileCount++;
                    if (result == null) {
                        updateAudioBook(Integer.parseInt(file_name));
                        // if (totalAudioFileCount == downloadedFileCount) {
                        downloadAudioFile();
                        updateData();
                        // }
                    }
                }
            }
        }
    }
    private void showPaymentOptionPopupWindow() {
        try {
// We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater) AudioBookDetailActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.book_buy_option_popup,
                    (ViewGroup) findViewById(R.id.popup_buy_option));
            paymentOptionView = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,true);

            paymentOptionView.showAtLocation(layout, Gravity.CENTER, 0, 0);

            TextView buyLabel=(TextView)layout.findViewById(R.id.buy_label);

            String buyLabelText="Buy " + selectedChapter.getEnglish_title()+" for just Rs."+selectedChapter.getPrice();

            if(audioBook.isApplyCoupon()){
                buyLabelText="Coupon discount "+audioBook.getCouponDiscount();
            }
            buyLabel.setText(buyLabelText);

            Button btn_addToBillButton = (Button) layout.findViewById(R.id.btn_addToBillButton);
            btn_addToBillButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addToMyBillButtonPressed();
                }
            });

            Button btn_buyFromCard = (Button) layout.findViewById(R.id.btn_buyFromCard);
            btn_buyFromCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    paymentOption = PaymentOption.OPTION_CARD;
                    isSelectChapterBuyOption=true;
                    buyFromCardButtonPressed();
                    paymentOptionView.dismiss();

                }
            });

            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    paymentOptionView.dismiss();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String result){
        stopDownload();
        if ( mProgressDialog!=null && mProgressDialog.isShowing() ){
            mProgressDialog.dismiss();
        }
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
        new Analytic().analyticEvent(7, audioBook.getBook_id(), "");


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
                String info="0";
                if(isSelectChapterBuyOption)
                    info=""+selectedChapter.getChapter_id();
                info=info+",card,success";
                new Analytic().analyticEvent(8, audioBook.getBook_id(), info);

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

                String info="0";
                if(isSelectChapterBuyOption)
                    info=""+selectedChapter.getChapter_id();
                info=info+",card,success";
                new Analytic().analyticEvent(8, audioBook.getBook_id(), info);

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
                String info="0";
                if(isSelectChapterBuyOption)
                    info=""+selectedChapter.getChapter_id();
                info=info+",card,failed";
                new Analytic().analyticEvent(8, audioBook.getBook_id(), info);

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
        else if (requestCode == 3) {
            if(resultCode == Constants.RESULT_SUCCESS){

               showCouponEnterScreen();
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

        }
    };

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
