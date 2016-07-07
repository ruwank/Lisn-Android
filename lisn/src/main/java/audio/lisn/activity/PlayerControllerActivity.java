package audio.lisn.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.telephony.TelephonyManager;
import android.transition.Slide;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RatingBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import audio.lisn.R;
import audio.lisn.adapter.CoverFlowAdapter;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.AudioBook.PaymentOption;
import audio.lisn.model.AudioBook.ServiceProvider;
import audio.lisn.model.BookChapter;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.Analytic;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.CustomTypeFace;
import audio.lisn.util.Log;
import audio.lisn.webservice.FileDownloadTask;
import audio.lisn.webservice.FileDownloadTaskListener;
import audio.lisn.webservice.JsonUTF8StringRequest;


public class PlayerControllerActivity extends AppCompatActivity implements FileDownloadTaskListener{
    private static final String TRANSITION_NAME = "audio.lisn.PlayerControllerActivity";
    public static final String TAG = PlayerControllerActivity.class.getSimpleName();

    private FancyCoverFlow mCoverFlow;
    private CoverFlowAdapter mAdapter;
    private TextSwitcher mTitle;
  //  private List<AudioBook> bookList =new ArrayList<>(0);
    Intent playbackServiceIntent;
    public ImageButton previousItemPlayButton,playPauseButton,
            nextItemPlayButton,playStopButton;
    public  TextView audioTitle,musicCurrentLoc,musicDuration;
    public DiscreteSeekBar musicSeekBar;
    ProgressDialog mProgressDialog;
    int totalAudioFileCount, downloadedFileCount;
    AudioBook audioBook;
    List<FileDownloadTask> downloadingList = new ArrayList<FileDownloadTask>();
    ConnectionDetector connectionDetector;
    ImageView bgImageView;
    private PopupWindow pwindo,paymentOptionView;
    ProgressDialog progressDialog;
    ImageButton commentButton;
    View topOverLayView;
    TextView bookTitleView;
    Toast infoToast;
    int chapterIndex;
    ServiceProvider  serviceProvider;
    PaymentOption  paymentOption;
    String subscriberId;


    public static void navigate(AppCompatActivity activity, View transitionView, AudioBook audioBook,int chapterIndex) {
        Intent intent = new Intent(activity, PlayerControllerActivity.class);
//        if(audioBook == null){
//            audioBook=AppController.getInstance().getCurrentAudioBook();
//        }
        if(audioBook != null) {
            intent.putExtra("audioBook", audioBook);
        }
            intent.putExtra("chapterIndex", chapterIndex);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionView, TRANSITION_NAME);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityTransitions();
        setContentView(R.layout.activity_player_controller);
        ViewCompat.setTransitionName(findViewById(R.id.app_bar_layout), TRANSITION_NAME);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        supportStartPostponedEnterTransition();
        getSupportActionBar().setTitle(R.string.app_name);

        progressDialog = new ProgressDialog(PlayerControllerActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Publishing...");

        bgImageView=(ImageView)findViewById(R.id.bgImageView);
        topOverLayView= findViewById(R.id.topOverLayView);

        connectionDetector = new ConnectionDetector(getApplicationContext());

        boolean reStartPlayer=true;
        if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("audioBook")) {
            audioBook = (AudioBook) getIntent().getSerializableExtra("audioBook");
            Log.v(TAG, "if size: " + audioBook.getChapters().size());
            reStartPlayer=false;
            chapterIndex=getIntent().getIntExtra("chapterIndex",0);
            updateAudioBook();
            setupData();
            downloadAudioFile();
            //updateAudioBook();
           // downloadAudioFile();
        }else{
            audioBook = AppController.getInstance().getCurrentAudioBook();
            chapterIndex=audioBook.getLastPlayFileIndex();
            setupData();
            updateView();
            Log.v(TAG, "else size: " + audioBook.getChapters().size());

        }
        Log.v(TAG, "chapterIndex " + chapterIndex);

       // mCoverFlow.setSelection(chapterIndex);

        // setBookTitle();
        bookTitleView= (TextView) findViewById(R.id.book_title);

        if(audioBook.getLanguageCode() == AudioBook.LanguageCode.LAN_SI){
            bookTitleView.setTypeface(CustomTypeFace.getSinhalaTypeFace(this));
        }else{
            bookTitleView.setTypeface(CustomTypeFace.getEnglishTypeFace(this));
        }
        mTitle.setText(audioBook.getTitle());
        setBookTitle();
        bookTitleView.setText(audioBook.getTitle());
        setCoverFlowPosition();




    }
    private void updateAudioBook(){
        if(audioBook !=null && audioBook.getBook_id() != null) {
            DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(this);
            HashMap<String, AudioBook> hashMap = downloadedAudioBook.getBookList(this);


            AudioBook returnBook = hashMap.get(audioBook.getBook_id());
            //int position=0;
            if (returnBook != null) {

                if(chapterIndex>=0) {
                    audioBook.setLastPlayFileIndex(chapterIndex);
                   // position=chapterIndex;
                }else{
                    chapterIndex=returnBook.getLastPlayFileIndex();

                    audioBook.setLastPlayFileIndex(returnBook.getLastPlayFileIndex());
                }
                audioBook.setLastSeekPoint(returnBook.getLastSeekPoint());
            }else{
                if(chapterIndex>=0) {
                    audioBook.setLastPlayFileIndex(chapterIndex);
                    //position=chapterIndex;

                }else{
                    audioBook.setLastPlayFileIndex(0);

                }
                audioBook.setLastSeekPoint(0);

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBroadcastReceiver();


    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayerUpdateReceiver);
    }

    private void setCoverFlowPosition(){

        int position=0;
//        if(audioBook !=null){
//            for (int i = 0; i <bookList.size() ; i++) {
//                AudioBook book=bookList.get(i);
//                if( audioBook.getBook_id().equalsIgnoreCase(book.getBook_id()) ){
//                    position=i;
//                    break;
//                }
//            }
//        }
        Log.v("position", "position:" + position);
       // mCoverFlow.setSelection(position);
       // setBookTitle(position);

    }
    private void setupData(){

        //bookList.clear();
//        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(this);
//       // downloadedAudioBook.readFileFromDisk(this);
//        HashMap< String, AudioBook> hashMap=downloadedAudioBook.getBookList(this);
//        for (AudioBook item : hashMap.values()) {
//            bookList.add(item);
//        }

        mTitle = (TextSwitcher) findViewById(R.id.play_book_title);
        mTitle.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                LayoutInflater inflater = LayoutInflater.from(PlayerControllerActivity.this);
                TextView textView = (TextView) inflater.inflate(R.layout.play_book_title, null);
                return textView;
            }

        });
        Animation in = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
        Animation out = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom);
        mTitle.setInAnimation(in);
        mTitle.setOutAnimation(out);



        mAdapter = new CoverFlowAdapter(this,audioBook);
        mAdapter.setData(audioBook.getChapters());
        mCoverFlow = (FancyCoverFlow) findViewById(R.id.coverflow);
        mCoverFlow.setAdapter(mAdapter);
        mCoverFlow.setReflectionEnabled(true);
        mCoverFlow.setReflectionRatio(0.3f);
        mCoverFlow.setReflectionGap(0);
        mCoverFlow.setMaxRotation(45);
        mCoverFlow.setSpacing(-50);
        mCoverFlow.setUnselectedScale(0.8f);

        mCoverFlow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(infoToast !=null){
                    infoToast.cancel();
                }
               // audioBook = bookList.get(position);
                chapterIndex=position;
                stopAudioPlayer();
Log.v(TAG,"chapterIndex"+chapterIndex);
                BookChapter selectedChapter=audioBook.getChapters().get(chapterIndex);
                if(selectedChapter.isPurchased() || audioBook.isTotalBookPurchased()){

                    Log.v(TAG,"downloadAudioFile"+chapterIndex);

                    downloadAudioFile();


                }else{

                    if (selectedChapter.getPrice() > 0) {
                        Log.v(TAG,"showPaymentOptionPopupWindow"+chapterIndex);
                        new Analytic().analyticEvent(4, audioBook.getBook_id(), "" + selectedChapter.getChapter_id());

                        showPaymentOptionPopupWindow();

                    }else{
                        Log.v(TAG,"logUserDownload"+chapterIndex);

                        logUserDownload();
                    }
                }

                //  setBookTitle(position);


            }
        });

//        mCoverFlow.setOnScrollChangeListener(new View.OnScrollChangeListener() {
//            @Override
//            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
//                Log.i("Scrolling", "X from ["+i+"] to ["+i1+"] to ["+i2+"] to ["+i3+"]" );
//              //  super.onScrollChanged(l, t, oldl, oldt);
//            }
//
//        });

        previousItemPlayButton=(ImageButton)this.findViewById(R.id.previousItemPlayButton);
        playPauseButton=(ImageButton)this.findViewById(R.id.playPauseButton);
        nextItemPlayButton=(ImageButton)this.findViewById(R.id.nextItemPlayButton);
        playStopButton=(ImageButton)this.findViewById(R.id.playstopButton);
        commentButton=(ImageButton)this.findViewById(R.id.commentButton);

        audioTitle=(TextView)this.findViewById(R.id.audioTitle);
        musicCurrentLoc=(TextView)this.findViewById(R.id.musicCurrentLoc);
        musicDuration=(TextView)this.findViewById(R.id.musicDuration);
        musicSeekBar=(DiscreteSeekBar)this.findViewById(R.id.musicSeekBar);
        previousItemPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //AppController.getInstance().playPreviousFile();
                AppController.getInstance().seekToBackward();

            }

        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                playPauseAudio();

            }

        });
        nextItemPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
               // AppController.getInstance().playNextFile();
                AppController.getInstance().seekToForward();


            }

        });

        commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                initiatePopupWindow();

            }

        });

        musicSeekBar.setOnProgressChangeListener(new SeekBarChangeEvent());


        playStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                stopAudioPlayer();

            }

        });

        mProgressDialog = new ProgressDialog(PlayerControllerActivity.this);
        mProgressDialog.setMessage("Downloading file..");
        mProgressDialog.setTitle("Download in progress ...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                stopDownload();
            }
        });


    }

    private void stopDownload(){
        for (int i = 0; i < downloadingList.size(); i++) {
            FileDownloadTask downloadTask = downloadingList.get(i);
            downloadTask.cancel(true);
        }
    }
    private void playPauseAudio(){
        BookChapter selectedChapter=audioBook.getChapters().get(chapterIndex);

        if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.mediaPlayer.isPlaying()){
            new Analytic().analyticEvent(10, audioBook.getBook_id(), ""+selectedChapter.getChapter_id());

            playPauseButton.setImageResource(R.drawable.btn_play_start);
            sendStateChange("pause");
        }else if(AudioPlayerService.mediaPlayer!=null){
            new Analytic().analyticEvent(9, audioBook.getBook_id(), ""+selectedChapter.getChapter_id());

            playPauseButton.setImageResource(R.drawable.btn_play_pause);
            sendStateChange("start");

        }

    }
    private void stopAudioPlayer(){
        BookChapter selectedChapter=audioBook.getChapters().get(chapterIndex);

        if(AudioPlayerService.mediaPlayer!=null){
            new Analytic().analyticEvent(12, audioBook.getBook_id(), ""+selectedChapter.getChapter_id());

            AppController.getInstance().bookmarkAudioBook();
            playPauseButton.setImageResource(R.drawable.btn_play_start);
            sendStateChange("stop");
            AppController.getInstance().stopPlayer();

        }


    }
private void setBookTitle(){

    Log.v("book.getLanguageCode()","audioBook : "+audioBook.getLanguageCode());

    if(audioBook.getLanguageCode() == AudioBook.LanguageCode.LAN_SI){
        bookTitleView.setTypeface(CustomTypeFace.getSinhalaTypeFace(this));
    }else{
        bookTitleView.setTypeface(CustomTypeFace.getEnglishTypeFace(this));
    }
    bookTitleView.setText(audioBook.getTitle());


    String img_path = AppUtils.getDataDirectory(getApplicationContext())
            + audioBook.getBook_id()+ File.separator+"book_cover.jpg";

    File imgFile = new  File(img_path);
    Bitmap bitmap=null;
    if(imgFile.exists()) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), bmOptions);
    }else{
        bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.ui_bg_logo);
    }
    if (Build.VERSION.SDK_INT >= 18) {

        Bitmap blurred = blurRenderScript(bitmap, 25);//second parametre is radius
        bgImageView.setImageBitmap(blurred);
    }else{
        topOverLayView.setBackgroundResource(R.color.color_transparent_alpha_0_8);
        bgImageView.setImageBitmap(bitmap);

    }


}


    private void initActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }
    }
    private void starAudioPlayer(){
        mProgressDialog.dismiss();

        if(playbackServiceIntent == null)
            playbackServiceIntent= AppController.getInstance().getPlaybackServiceIntent();

//        String[] fileList=new String[audioBook.getDownloadedChapter().size()];
//        for (int i=0; i<audioBook.getDownloadedChapter().size();i++){
//            Log.v("getDownloadedChapter","getDownloadedChapter:"+audioBook.getBook_id()+"/"+(i+1)+".lisn");
//
//            fileList[i]= AppUtils.getDataDirectory(getApplicationContext())+audioBook.getBook_id()+"/"+(i+1)+".lisn";
//
//        }
       // AppController.getInstance().setFileList(fileList);
        AppController.getInstance().setCurrentAudioBook(audioBook);

       // AppController.getInstance().fileIndex=(audioBook.getLastPlayFileIndex()-1);
        AppController.getInstance().fileIndex=(chapterIndex-1);
        stopService(playbackServiceIntent);
        startService(playbackServiceIntent);
        AppController.getInstance().playNextFile();
        //updateView();
        Log.v("book.getLanguageCode()",""+audioBook.getLanguageCode());


    }
    //Downlaod file
    private void downloadAudioFileFromUrl(int filePart){

        if (connectionDetector.isConnectingToInternet()) {
            String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                    + audioBook.getBook_id()+ File.separator;
            File file = new File(dirPath + filePart + ".lisn");

            if (file.exists()) {
                file.delete();
            }
            FileDownloadTask downloadTask =  new FileDownloadTask(this,this,audioBook.getBook_id());
            downloadTask.execute(dirPath, "" + filePart);
            downloadingList.add(downloadTask);

        }else{
           AlertDialog.Builder builder = new AlertDialog.Builder(
                    this);
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_INTERNET_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private void downloadAudioFile() {
        String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                + audioBook.getBook_id()+File.separator;
       // if (connectionDetector.isConnectingToInternet()) {



            //downloadedFileCount=0;
           // totalAudioFileCount=0;


           // if(audioBook.isTotalBookPurchased()) {

               // for (int index = 0; index < (audioBook.getChapters().size()); index++) {
                    BookChapter bookChapter=audioBook.getChapters().get(chapterIndex);
                    File file = new File(dirPath + bookChapter.getChapter_id() + ".lisn");
                    if (!file.exists() || !(audioBook.getDownloadedChapter().contains(bookChapter.getChapter_id()))) {
                        if(AppUtils.getAvailableMemory() < audioBook.getFileSize()){
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

                        }else {
                            if (audioBook.isTotalBookPurchased() || bookChapter.isPurchased()) {
                                if(connectionDetector.isConnectingToInternet()) {
                                    downloadingList.clear();
                                    mProgressDialog.show();

                                    downloadAudioFileFromUrl(bookChapter.getChapter_id());
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

                            } else {
                                if(bookChapter.getPrice()>0){
                                    showPaymentOptionPopupWindow();
                                }else{
                                    logUserDownload();
                                }
//show  payment option
                            }
                        }
                        //totalAudioFileCount++;
                    }else{
                        mProgressDialog.dismiss();
                        starAudioPlayer();
                    }
                //}

          //  }
//            else{
//                //for (int index = 0; index < (audioBook.getChapters().size()); index++) {
//                    BookChapter bookChapter=audioBook.getChapters().get(chapterIndex);
//                    if(bookChapter.isPurchased()) {
//                        File file = new File(dirPath + bookChapter.getChapter_id() + ".lisn");
//                        if (!file.exists() || !(audioBook.getDownloadedChapter().contains(bookChapter.getChapter_id()))) {
//                            downloadAudioFileFromUrl(bookChapter.getChapter_id());
//                           // totalAudioFileCount++;
//                        }else{
//                            mProgressDialog.dismiss();
//                            starAudioPlayer();
//                        }
//                    }else{
//                        //show payment option
//                    }
//                //}
//            }

//            if(downloadedFileCount ==totalAudioFileCount){
//                mProgressDialog.dismiss();
//                starAudioPlayer();
//            }else{
//                if(AppUtils.getAvailableMemory() < audioBook.getFileSize()){
//                    stopDownload();
//
//                    AlertDialog.Builder builder = new AlertDialog.Builder(
//                            this);
//                    builder.setTitle(R.string.NO_ENOUGH_SPACE_TITLE).setMessage(R.string.NO_ENOUGH_SPACE_MESSAGE).setPositiveButton(
//                            R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int id) {
//                                    // FIRE ZE MISSILES!
//                                }
//                            });
//                    AlertDialog dialog = builder.create();
//                    dialog.show();
//
//                }else {
//                    mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getChapters().size());
//                }
//
//            }
//
//        } else {
//
//            downloadedFileCount=0;
//            totalAudioFileCount=0;
//
//            if(audioBook.isTotalBookPurchased()) {
//
//                for (int index = 0; index < (audioBook.getChapters().size()); index++) {
//                    BookChapter bookChapter=audioBook.getChapters().get(index);
//                    File file = new File(dirPath + bookChapter.getChapter_id() + ".lisn");
//                    if (!file.exists() || !(audioBook.getDownloadedChapter().contains(bookChapter.getChapter_id()))) {
//                        totalAudioFileCount++;
//                    }
//                }
//
//            }else{
//                for (int index = 0; index < (audioBook.getChapters().size()); index++) {
//                    BookChapter bookChapter=audioBook.getChapters().get(index);
//                    if(bookChapter.isPurchased()) {
//                        File file = new File(dirPath + bookChapter.getChapter_id() + ".lisn");
//                        if (!file.exists() || !(audioBook.getDownloadedChapter().contains(bookChapter.getChapter_id()))) {
//                            totalAudioFileCount++;
//                        }
//                    }
//                }
//            }
//
////            for (int filePart=1; filePart<=(audioBook.getAudioFileCount()); filePart++){
////                File file = new File(dirPath +filePart+".lisn");
////                if (!file.exists()) {
////                    totalAudioFileCount++;
////                }
////
////
////            }
//            if(downloadedFileCount ==totalAudioFileCount){
//                mProgressDialog.dismiss();
//                starAudioPlayer();
//            }else{
//
//
//                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
//                        this);
//                builder.setTitle(getString(R.string.NO_INTERNET_TITLE)).setMessage(getString(R.string.NO_INTERNET_MESSAGE)).setPositiveButton(
//                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                // FIRE ZE MISSILES!
//                            }
//                        });
//
//                android.app.AlertDialog dialog = builder.create();
//                dialog.show();
//            }
//        }
    }
    private void registerBroadcastReceiver(){
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mPlayerUpdateReceiver,
                new IntentFilter(Constants.PLAYER_STATE_UPDATE));
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mPlayerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            updateView();
        }
    };
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

    @Override
    public void onPostExecute(String result, String file_name) {
        if (result != null && result.equalsIgnoreCase("UNAUTHORISED")) {
            showMessage("UNAUTHORISED");

        } else if (result != null && result.equalsIgnoreCase("NOTFOUND")) {
            showMessage("NOTFOUND");

        } else {
           // mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getChapters().size());

            //downloadedFileCount++;
            if (result == null) {
                updateAudioBook(Integer.parseInt(file_name));
                mAdapter.notifyDataSetChanged();
               // if (totalAudioFileCount == downloadedFileCount) {
                 //   mProgressDialog.dismiss();
                    downloadAudioFile();
                //}
            }


        }

    }

    class SeekBarChangeEvent implements DiscreteSeekBar.OnProgressChangeListener {
        @Override
        public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int progress, boolean fromUser) {
            if (fromUser) {
                if(AudioPlayerService.mediaPlayer!=null) {
                    AudioPlayerService.mediaPlayer.seekTo(progress);// ?
                }
            }
        }

        @Override
        public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) {
            if(AudioPlayerService.mediaPlayer!=null) {
                playPauseButton.setImageResource(R.drawable.btn_play_start);
                AudioPlayerService.mediaPlayer.pause(); // ?

            }
        }

        @Override
        public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
            if(AudioPlayerService.mediaPlayer!=null) {
                playPauseButton.setImageResource(R.drawable.btn_play_pause);
                AudioPlayerService.mediaPlayer.start(); // ?
            }
        }
    }


    public void updateView(){
        audioTitle.setText(AppController.getInstance().getPlayerControllerTitle());
        if(AudioPlayerService.mediaPlayer!=null){
            Log.v(TAG,"updateView getCurrentPosition: "+AudioPlayerService.mediaPlayer.getCurrentPosition());

            musicSeekBar.setMax(AudioPlayerService.audioDuration);
            musicSeekBar.setProgress(AudioPlayerService.mediaPlayer.getCurrentPosition());
            musicCurrentLoc.setText(milliSecondsToTimer(AudioPlayerService.mediaPlayer.getCurrentPosition()));
            musicDuration.setText(milliSecondsToTimer(AudioPlayerService.audioDuration));

            if(AudioPlayerService.mediaPlayer.isPlaying()){
                playPauseButton.setImageResource(R.drawable.btn_play_pause);

            }else {
                playPauseButton.setImageResource(R.drawable.btn_play_start);


            }
        }

        audioTitle.setText(AppController.getInstance().getPlayerControllerTitle());
    }

    public String milliSecondsToTimer(long milliseconds){
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int)( milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);
        // Add hours if there
        if(hours > 0){
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if(seconds < 10){
            secondsString = "0" + seconds;
        }else{
            secondsString = "" + seconds;}

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }
    // Send an Intent with an action named "my-event".
    private void sendStateChange(String state) {
        Intent intent = new Intent(Constants.PLAYER_STATE_CHANGE);
        // add data
        intent.putExtra("state", state);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
    private Bitmap blurRenderScript(Bitmap smallBitmap, int radius) {

        try {
            smallBitmap = RGB565toARGB888(smallBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }


        Bitmap bitmap = Bitmap.createBitmap(
                smallBitmap.getWidth(), smallBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript renderScript = RenderScript.create(getApplicationContext());

        Allocation blurInput = Allocation.createFromBitmap(renderScript, smallBitmap);
        Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,
                Element.U8_4(renderScript));
        blur.setInput(blurInput);
        blur.setRadius(radius); // radius must be 0 < r <= 25
        blur.forEach(blurOutput);

        blurOutput.copyTo(bitmap);
        renderScript.destroy();

        return bitmap;

    }

    private Bitmap RGB565toARGB888(Bitmap img) throws Exception {
        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

        //Get JPEG pixels.  Each int is the color values for one pixel.
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

        //Create a Bitmap of the appropriate format.
        Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);

        //Set RGB pixels.
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }
    private void initiatePopupWindow() {
        try {
// We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater) PlayerControllerActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rate_book_popup,
                    (ViewGroup) findViewById(R.id.popup_element));
            pwindo = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,true);

            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
            final RatingBar ratingBar = (RatingBar) layout.findViewById(R.id.rating_bar);
            final TextView reviewTitle = (TextView) layout.findViewById(R.id.review_title);
            final TextView reviewDescription = (TextView) layout.findViewById(R.id.review_description);

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
    private void publishUserReview( float rate,String title,String comment){


        if (connectionDetector.isConnectingToInternet()) {
            if(rate>0 && title.length()>0 && comment.length()>0) {


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

                                Log.v("response", "response:" + response);
                                Toast toast = Toast.makeText(getApplicationContext(), R.string.REVIEW_PUBLISH_SUCCESS, Toast.LENGTH_SHORT);
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
            }else{
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_valid_data), Toast.LENGTH_SHORT);
                toast.show();
            }
        }else{
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

        private void showMessage(String result){
            stopDownload();
            mProgressDialog.dismiss();

            if (result.toUpperCase().contains("UNAUTHORISED")){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.USER_UNAUTHORISED_TITLE).setMessage(getString(R.string.USER_UNAUTHORISED_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

            }else if(result.toUpperCase().contains("NOTFOUND")){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

//buy book chapter methods

    private void showPaymentOptionPopupWindow() {
        try {
// We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater) PlayerControllerActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.book_buy_option_popup,
                    (ViewGroup) findViewById(R.id.popup_buy_option));
            paymentOptionView = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,true);

            paymentOptionView.showAtLocation(layout, Gravity.CENTER, 0, 0);

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
                    buyFromCardButtonPressed();
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
        params.put("amount", audioBook.getPrice());
        if(paymentOption==PaymentOption.OPTION_DIALOG){
          //  params.put("number", dialogNo);
        }else{
            params.put("action", "charge");
        }
        final BookChapter selectedChapter=audioBook.getChapters().get(chapterIndex);
            params.put("chapid", ""+selectedChapter.getChapter_id());
            params.put("amount", ""+selectedChapter.getPrice());




        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,true,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("addToBillServerConnect", "addToBillServerConnect 2");

                        String info="0";
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
                            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
                            builder.setTitle(getString(R.string.PAYMENT_COMPLETE_TITLE)).
                                    setMessage(getString(R.string.PAYMENT_COMPLETE_MESSAGE)).setPositiveButton(
                                    getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                           // setupData();
                                            downloadAudioFile();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.BUTTON_LATER), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                           // setupData();
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else if (response.toUpperCase().contains("ALREADY_PAID")) {
                            Log.v("addToBillServerConnect", "addToBillServerConnect 3");
                            info=info+",already_paid";
                            new Analytic().analyticEvent(8, audioBook.getBook_id(), info);
                            //audioBook.setPurchase(true);
                            // updateAudioBook(0);
                            updateAudioBookSuccessPayment();

                            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
                            builder.setTitle(getString(R.string.ALREADY_PAID_TITLE)).
                                    setMessage(getString(R.string.ALREADY_PAID_MESSAGE)).setPositiveButton(
                                    getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                           // setupData();
                                            downloadAudioFile();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.BUTTON_LATER), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                           // setupData();

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



                            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
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

                            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
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

                String info=""+selectedChapter.getChapter_id();
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

                AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
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
                    Log.v("addToMobitelBill","addToMobitelBill 4");

                    AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
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


                    AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
                    //Confirm Payment

                    builder.setTitle("Confirm Payment").setMessage("Rs." + audioBook.getPrice() + " will be added to your Dialog bill. Continue?").setPositiveButton(
                            getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(
                                            getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                                    String provider = sharedPref.getString(getString(R.string.service_provider),"");

                                    if(provider.equalsIgnoreCase(subscriberId) ) {
                                        addToBillServerConnect();
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

    }
    private void updateAudioBookSuccessPayment(){

        audioBook.setPurchase(true);
        BookChapter selectedChapter=audioBook.getChapters().get(chapterIndex);
            selectedChapter.setIsPurchased(true);
//            for (int i = 0; i <audioBook.getChapters().size() ; i++) {
//                BookChapter bookChapter=audioBook.getChapters().get(i);
//                if(bookChapter.getChapter_id() == selectedChapter.getChapter_id()){
//                    bookChapter.setIsPurchased(true);
//                    break;
//                }
//            }



        updateAudioBook(0);

    }
    private void buyFromCardButtonPressed(){

        if(AppController.getInstance().isUserLogin()){
                Intent intent = new Intent(this,
                        PurchaseActivity.class);
                intent.putExtra("audioBook", audioBook);
           BookChapter selectedChapter=audioBook.getChapters().get(chapterIndex);
                    intent.putExtra("isSelectChapterBuyOption", true);
                    intent.putExtra("selectedChapter", selectedChapter);


                startActivityForResult(intent, 2);



        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("onActivityResult", "onActivityResult");
        //callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == Constants.RESULT_SUCCESS) {
                if (paymentOption == PaymentOption.OPTION_NONE) {
                    //playGetButtonPressed();
                } else if (paymentOption == PaymentOption.OPTION_MOBITEL) {
                    addToMobitelBill();

                } else if (paymentOption == PaymentOption.OPTION_ETISALAT) {
                    addToEtisalatBill();

                } else if (paymentOption == PaymentOption.OPTION_DIALOG) {
                    addToDialogBill();

                } else if (paymentOption == PaymentOption.OPTION_CARD) {
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
        } else if (requestCode == 2) {
            if (resultCode == Constants.RESULT_SUCCESS) {
                audioBook.setPurchase(true);
                updateAudioBook(0);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.PAYMENT_COMPLETE_TITLE).setMessage(getString(R.string.PAYMENT_COMPLETE_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_NOW, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                               // setupData();
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


            } else if (resultCode == Constants.RESULT_SUCCESS_ALREADY) {
                Log.v("addToBillServerConnect", "addToBillServerConnect 3");

                audioBook.setPurchase(true);
                updateAudioBook(0);
                AlertDialog.Builder builder = new AlertDialog.Builder(PlayerControllerActivity.this);
                builder.setTitle(getString(R.string.ALREADY_PAID_TITLE)).
                        setMessage(getString(R.string.ALREADY_PAID_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                               // setupData();
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
        private void logUserDownload(){
            final BookChapter selectedChapter=audioBook.getChapters().get(chapterIndex);
            progressDialog.setMessage("Loading...");
            progressDialog.show();
            Map<String, String> params = new HashMap<String, String>();
            params.put("userid", AppController.getInstance().getUserId());
            params.put("bookid", audioBook.getBook_id());

            params.put("chapid", ""+selectedChapter.getChapter_id());
Log.v(TAG,""+params);

            String url = getResources().getString(R.string.user_download_activity_url);

            JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url,params,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.v(TAG, " downloader response:" + response);
                            audioBook.getChapters().get(chapterIndex).setIsPurchased(true);

                            audioBook.setPurchase(true);
                           // selectedChapter.setIsPurchased(true);
                            updateAudioBook(0);
                            progressDialog.dismiss();
                           // setupData();
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

}
