package audio.lisn.app;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;

import org.json.JSONArray;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import audio.lisn.R;
import audio.lisn.model.AudioBook;
import audio.lisn.model.BookCategory;
import audio.lisn.model.BookChapter;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.Analytic;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.AudioPlayerService.AudioPlayerServiceBinder;
import audio.lisn.util.Constants;
import audio.lisn.util.DailyReminderReceiver;
import audio.lisn.util.Foreground;
import audio.lisn.util.Log;
import audio.lisn.util.LruBitmapCache;
import audio.lisn.util.NukeSSLCerts;
import audio.lisn.util.PreviewAudioPlayerService;
import audio.lisn.util.ReminderReceiver;
import audio.lisn.webservice.JsonUTF8StringRequest;
import io.fabric.sdk.android.Fabric;
public class AppController extends Application  {

    public static final String TAG = AppController.class.getSimpleName();

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private String userName,password,userId,gcmRegId,fbId;
    Intent playbackServiceIntent;
    //private String[] fileList;
    AudioPlayerService mService;
    boolean mBound = false;
    public int fileIndex=-1;
    private JSONArray newReleaseBookList,topRatedBookList,topDownloadedBookList;
    private AudioBook currentAudioBook;
    int retryCount=0;
    private static AppController mInstance;

    Intent previewPlaybackServiceIntent;
    PreviewAudioPlayerService mPreviewService;
    boolean mPreviewBound = false;
    private HashMap<Integer, JSONArray> storeBook=new HashMap<Integer, JSONArray>();

    private static final int NOTIFY_ID=158;
    AlarmManager alarmManager = null;
    AlarmManager alarmManagerRepeat = null;
    private BookCategory[] bookCategories;
    private String sessionId;
    private Location lastLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        NukeSSLCerts.nuke();
        FacebookSdk.sdkInitialize(getApplicationContext());
        Foreground.init(this);

        Fabric.with(this, new Crashlytics());
        mInstance = this;
        registerAppStateChangeBroadcastReceiver();
        registerPlayerStopBroadcastReceiver();
        showQuotesEveryWeek();
        sessionId = String.valueOf(UUID.randomUUID());

        //Analytic activity 1
        new Analytic().analyticEvent(1,"","");

    }


    public static synchronized AppController getInstance() {
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public ImageLoader getImageLoader() {
        getRequestQueue();
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(this.mRequestQueue,
                    new LruBitmapCache());
        }
        return this.mImageLoader;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    public Intent getPlaybackServiceIntent(){
        if (playbackServiceIntent == null) {
            playbackServiceIntent = new Intent(getApplicationContext(), AudioPlayerService.class);
            bindService(playbackServiceIntent, mConnection, Context.BIND_AUTO_CREATE);

        }
        return this.playbackServiceIntent;
    }
    public Intent getPreviewPlaybackServiceIntent(){
        if (previewPlaybackServiceIntent == null) {
            previewPlaybackServiceIntent = new Intent(getApplicationContext(), PreviewAudioPlayerService.class);
            bindService(previewPlaybackServiceIntent, mPreviewConnection, Context.BIND_AUTO_CREATE);

        }
        return this.previewPlaybackServiceIntent;
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public boolean isUserLogin(){
        if(userId != null && userId.length()>0){
            return true;
        }
        return false;
    }

    public String getGcmRegId() {
        return gcmRegId;
    }

    public void setGcmRegId(String gcmRegId) {
        this.gcmRegId = gcmRegId;
    }

    public String getAudioFileName() {
        return currentAudioBook.getEnglish_title();
    }
    public String getPlayerControllerTitle() {
        String title="";
        if(currentAudioBook != null) {
            if (fileIndex >= 0 && fileIndex < (currentAudioBook.getChapters().size())) {
                BookChapter bookChapter=currentAudioBook.getChapters().get(fileIndex);
                title=bookChapter.getEnglish_title();
               // title=  "[ " + (fileIndex + 1) + " / " + currentAudioBook.getChapters().size() + " ]";
            }
        }
        return title;
    }



    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance

            AudioPlayerServiceBinder binder = (AudioPlayerServiceBinder) service;
            mService = binder.getService();
            mBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mPreviewConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance

            PreviewAudioPlayerService.PreviewAudioPlayerServiceBinder binder = (PreviewAudioPlayerService.PreviewAudioPlayerServiceBinder) service;
            mPreviewService = binder.getService();
            mPreviewBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mPreviewBound = false;
        }
    };
    public void playNextFile(){
        Log.v("playNextFile", "mBound :" + mBound);

        if (mBound && currentAudioBook.getChapters() != null) {
            retryCount=0;
            fileIndex++;
            Log.v("playNextFile", "fileIndex :" + fileIndex);

            if(fileIndex>=0 && fileIndex<(currentAudioBook.getChapters().size())) {
                Log.v("playNextFile", "fileIndex :" + fileIndex);
                BookChapter bookChapter=currentAudioBook.getChapters().get(fileIndex);
                String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                        + currentAudioBook.getBook_id()+ File.separator;
                String fileName=dirPath +(bookChapter.getChapter_id())+".lisn";

                File file = new File(fileName);

                if(file.exists()) {

                    int seekPoint=0;
                    if (currentAudioBook.getLastPlayFileIndex() == fileIndex) {
                        seekPoint=currentAudioBook.getLastSeekPoint();
                    }
                    mService.playAudioFile(fileName,seekPoint,fileIndex);

                }

            }else{
                fileIndex=currentAudioBook.getChapters().size();
            }
            currentAudioBook.setLastSeekPoint(0);
            currentAudioBook.setLastPlayFileIndex(0);
        }
        if(!mBound){

            if(++retryCount<5){

                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run(){
                        Intent playbackServiceIntent1=AppController.getInstance().getPlaybackServiceIntent();
                        stopService(playbackServiceIntent1);
                        startService(playbackServiceIntent1);
                        playNextFile();
                    }
                }, 5);
            }


        }
    }

//    public void playPreviousFile(){
//        if (mBound && fileList != null) {
//            fileIndex--;
//            if(fileIndex>=0 && fileIndex<(fileList.length)) {
//                Log.v("playPreviousFile", "fileIndex :" + fileIndex);
//
//                String fileName=fileList[fileIndex];
//                mService.playAudioFile(fileName);
//            }else{
//                fileIndex=-1;
//            }
//        }
//    }
    public void seekToForward(){
        if (mBound){
            mService.seekToForward(true);
        }

    }
    public void seekToBackward(){
        if (mBound){
            mService.seekToForward(false);
        }

    }
    public void starPlayer(){
        if (mBound)
            mService.changePlayerState("start");
    }
    public void pausePlayer(){
        if (mBound)
            mService.changePlayerState("pause");

    }
    public void stopPlayer(){
        fileIndex=-1;
    }

//    public String[] getFileList() {
//        return fileList;
//    }
//
//    public void setFileList(String[] fileList) {
//        this.fileList = fileList;
//    }

    public JSONArray getNewReleaseBookList() {
        return newReleaseBookList;
    }

    public void setNewReleaseBookList(JSONArray bookList) {
        this.newReleaseBookList = bookList;
    }

    public String getPlayingBookId() {
if(currentAudioBook != null){
    return currentAudioBook.getBook_id();
}else{
    return "";
}

    }

    public void bookmarkAudioBook(){
        AudioBook audioBook=getCurrentAudioBook();

        if(audioBook != null) {
            Log.v(TAG, "Book Id:" + audioBook.getBook_id());
            Log.v(TAG, "LastPlayFileIndex:" + audioBook.getLastPlayFileIndex());
            Log.v(TAG, "LastSeekPoint:" + audioBook.getLastSeekPoint());

            DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                    getApplicationContext());
            downloadedAudioBook.addBookToList(getApplicationContext(),
                    audioBook.getBook_id(), audioBook);
        }

    }

    public AudioBook getCurrentAudioBook() {
        if(currentAudioBook != null) {
            Log.v("currentAudioBook","currentAudioBook"+currentAudioBook);
            if (fileIndex < 1) {
                currentAudioBook.setLastPlayFileIndex(0);
            } else {
                currentAudioBook.setLastPlayFileIndex(fileIndex);
            }
            if (mService != null)
                currentAudioBook.setLastSeekPoint(mService.getSeekPosition());
        }
        return currentAudioBook;
    }
    public int getCurrentAudioPosition(){
        int position=0;
        if (mService != null){
            position= mService.getSeekPosition();
        }
        return position;
    }

    public void setCurrentAudioBook(AudioBook currentAudioBook) {
        this.currentAudioBook = currentAudioBook;
    }


    public void playPreviewFile(final String fileUrl){
        Log.v("playNextFile", "mBound :" + mPreviewBound);



        if (mPreviewBound) {
            stopService(previewPlaybackServiceIntent);
            startService(previewPlaybackServiceIntent);
            mPreviewService.playAudioFile(fileUrl);

        }
        if(!mPreviewBound){

            if(++retryCount<5){

                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run(){
                        Intent playbackServiceIntent1=AppController.getInstance().getPreviewPlaybackServiceIntent();
                        stopService(playbackServiceIntent1);
                        startService(playbackServiceIntent1);
                        playPreviewFile(fileUrl);
                    }
                }, 5);
            }


        }
    }

    public HashMap<Integer, JSONArray> getStoreBook() {
        return storeBook;
    }
    public JSONArray getStoreBookForCategory(int categoryId) {
        return storeBook.get(categoryId);
    }
    public void setStoreBookForCategory(int categoryId,JSONArray bookArray) {
        storeBook.put(categoryId, bookArray);
    }

    private void registerAppStateChangeBroadcastReceiver(){
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mAppEnterForegroundReceiver,
                new IntentFilter(Constants.APP_STATE_FOREGROUND));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mAppEnterBackgroundReceiver,
                new IntentFilter(Constants.APP_STATE_BACKGROUND));
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mAppEnterForegroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG,"mAppEnterForegroundReceiver");
            appEnterForeground();


        }
    };
    private BroadcastReceiver mAppEnterBackgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            appEnterBackground();


        }
    };

    private void appEnterForeground(){
        if(!isAppIsInBackground()) {
            if (isUserLogin()) {
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                if (accessToken != null && !accessToken.isExpired()) {
                    verifyUser();

                } else {
                    Log.v(TAG, "AccessToken isExpired");
                    LoginManager.getInstance().logOut();
                    this.userId = null;

                }



            }
            sessionId = String.valueOf(UUID.randomUUID());
            if (alarmManagerRepeat != null) {
                Intent intentAlarm = new Intent(getApplicationContext(), ReminderReceiver.class);

                alarmManagerRepeat.cancel(PendingIntent.getBroadcast(getApplicationContext(), 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
                alarmManagerRepeat=null;
            }
        }
    }
    private void appEnterBackground(){

        if(isAppIsInBackground()) {
            if (mBound)
                mService.updatePlaybackState();

            showReminder();
        }

    }
    private boolean isAppIsInBackground() {
        Context context=getApplicationContext();
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }

    public void logOutUser(){
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken != null){
            LoginManager.getInstance().logOut();
        }
        this.userId=null;
    }
    private void showQuotesEveryWeek()

    {


        if(alarmManagerRepeat==null) {
            Intent intentAlarm = new Intent(getApplicationContext(), ReminderReceiver.class);

            alarmManagerRepeat = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

            alarmManagerRepeat.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY * 6,
                    AlarmManager.INTERVAL_DAY * 7, PendingIntent.getBroadcast(getApplicationContext(), 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    private void showReminder ()
    {
        if(alarmManager==null) {
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            // Log.v("showReminder","showReminder: "+SystemClock.elapsedRealtime()+5*1000);
            Intent intentAlarm = new Intent(getApplicationContext(), DailyReminderReceiver.class);

            //3 days
           // long time = System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 1);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY,
                    AlarmManager.INTERVAL_DAY, PendingIntent.getBroadcast(getApplicationContext(), 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
            // alarmManager.set(AlarmManager.RTC_WAKEUP, time, PendingIntent.getBroadcast(getApplicationContext(), 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
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
    private void removeUser(){
        logOutUser();
    }
    //synchronizing profile
    private void verifyUser(){


        String url=getString(R.string.verify_user_url);

        Map<String, String> postParam = new HashMap<String, String>();

        try {
            postParam.put("userid",userId);
            postParam.put("device",getUniqueID());
            Log.v("response", "verifyUser postParam" + postParam);
            Log.v("response", "verifyUser" +getUniqueID());


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        JsonUTF8StringRequest userVerifyReq = new JsonUTF8StringRequest(Request.Method.POST,url, postParam,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("response", "verifyUser :" + response);

                        if(response.toUpperCase().contains("DUPLICATE_USER")){
                            removeUser();

                        }
                        else if(response.toUpperCase().contains("SUCCESS")){

                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("response","verifyUser error :"+error.getMessage());
                NetworkResponse response = error.networkResponse;
                if(response !=null) {
                    Log.v("response", "verifyUser "+response.statusCode + " data: " + response.data.toString());
                }

            }
        });
        userVerifyReq.setShouldCache(false);
        AppController.getInstance().addToRequestQueue(userVerifyReq, "tag_user_verify");

    }


    public JSONArray getTopRatedBookList() {
        return topRatedBookList;
    }

    public void setTopRatedBookList(JSONArray topRatedBookList) {
        this.topRatedBookList = topRatedBookList;
    }

    public JSONArray getTopDownloadedBookList() {
        return topDownloadedBookList;
    }

    public void setTopDownloadedBookList(JSONArray topDownloadedBookList) {
        this.topDownloadedBookList = topDownloadedBookList;
    }

    public BookCategory[] getBookCategories() {
        return bookCategories;
    }

    public void setBookCategories(BookCategory[] bookCategories) {
        this.bookCategories = bookCategories;
        this.storeBook.clear();
    }


    public String getFbId() {
        return fbId;
    }

    public void setFbId(String fbId) {
        this.fbId = fbId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location mLastLocation) {
        this.lastLocation = mLastLocation;
    }
    private void registerPlayerStopBroadcastReceiver(){
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mPlayerUpdateStopReceiver,
                new IntentFilter(Constants.PLAYER_STATE_STOP));
    }
    private BroadcastReceiver mPlayerUpdateStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            playNextChapter();
        }
    };
    private void playNextChapter() {
        BookChapter bookChapter = currentAudioBook.getChapters().get(fileIndex + 1);
        String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                + currentAudioBook.getBook_id() + File.separator;
        String fileName = dirPath + (bookChapter.getChapter_id()) + ".lisn";

        File file = new File(fileName);
        if ((fileIndex + 1)<currentAudioBook.getChapters().size()) {
            if (file.exists() && currentAudioBook.getDownloadedChapter().contains(bookChapter.getChapter_id()) && file.length()>100) {
                Log.v(TAG, "file.exists");
                playNextFile();
            } else {
                Intent intent = new Intent(Constants.PLAYER_STATE_NEXT_CHAPTER);
                intent.putExtra("chapterIndex", fileIndex + 1);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }
    }
}
