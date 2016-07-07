package audio.lisn.util;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import audio.lisn.app.AppController;
import audio.lisn.service.MediaNotificationManager;


public class AudioPlayerService extends Service implements Runnable, OnCompletionListener,
        OnPreparedListener,OnBufferingUpdateListener,MediaPlayer.OnErrorListener,MusicFocusable {

    public static final String TAG = AudioPlayerService.class.getSimpleName();

    public static MediaPlayer mediaPlayer;
    public static boolean hasStartedPlayer;
    private final IBinder mBinder = new AudioPlayerServiceBinder();
    AudioFocusHelper mAudioFocusHelper = null;
    public static int seekPosition;
    public static int audioDuration;
    Thread timerUpdateThread;
    boolean keepRunning;
    private MediaNotificationManager mMediaNotificationManager;

    // indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    State mState = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;

    // why did we pause? (only relevant if mState == State.Paused)

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;



    public class AudioPlayerServiceBinder extends Binder {
        public AudioPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AudioPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        createPlayer();
        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mStateChangeReceiver,
                new IntentFilter(Constants.PLAYER_STATE_CHANGE));
        mMediaNotificationManager = new MediaNotificationManager(this);


    }
    private void createPlayer(){
        mediaPlayer = new MediaPlayer();
	        /*  */
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }
    private void playDecodedAudioFile(byte[] mp3SoundByteArray) {
        try {
            Log.v(TAG,"playDecodedAudioFile:");
            //int fileIndex= AppController.getInstance().fileIndex;
            // create temp file that will hold byte array
            File tempMp3 = File.createTempFile("audiobook", "mp3", getCacheDir());
            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(mp3SoundByteArray);
            fos.close();


            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.prepareAsync();

        } catch (Exception ex) {
            Log.v(TAG,"playDecodedAudioFile Exception: "+ex.toString());

            String s = ex.toString();
            ex.printStackTrace();
            Log.v(TAG, ex.toString());

        }
    }

    private void playAudioBook(String filePath){
        //String filePath=AppUtils.getDataDirectory(getApplicationContext())+audioBook.getISBN()+"/1.mp3";
        hasStartedPlayer=true;
        byte[] contents = null;

        File file = new File(filePath);
        int size = (int) file.length();
        contents = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(
                    new FileInputStream(file));
            try {
                buf.read(contents);
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            byte[] decodedData = AppUtils.decodeFile(contents);
            playDecodedAudioFile(decodedData);
        } catch (Exception e) {
            Log.v(TAG, e.toString());
            e.printStackTrace();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        return START_STICKY;
    }
    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }
    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }
    public void onDestroy() {
        if (mediaPlayer!=null  && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        stopForeground(true);
        mediaPlayer=null;
        giveUpAudioFocus();
        mState = State.Stopped;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStateChangeReceiver);
        stopThread();
    }


    @Override
    public void run() {
        int currentPosition = 0;//
        while (keepRunning && mediaPlayer != null && currentPosition < mediaPlayer.getDuration()) {
            try {
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                Log.v(TAG,"InterruptedException: "+e.getMessage());
                e.printStackTrace();
            }
            sendMessage();
        }

    }


    public void onCompletion(MediaPlayer mp) {
        mState = State.Stopped;

        stopThread();

        if(mp.getCurrentPosition()>1){
            AppController.getInstance().playNextFile();
        }
        updatePlaybackState();
        Log.v(TAG,"onCompletion position" + mp.getCurrentPosition());

    }

    @Override
    public void onPrepared(MediaPlayer player) {

        if(player != null && player.getDuration()>0){
            Log.v(TAG,"onPrepared player duration "+player.getDuration());
            Log.v(TAG, "onPrepared seekPosition "+seekPosition);
            hasStartedPlayer=true;
            audioDuration=player.getDuration();
            tryToGetAudioFocus();
            player.start();
            player.seekTo(seekPosition);

            stopThread();
            keepRunning=true;
            timerUpdateThread = new Thread(this );
            timerUpdateThread.start();

            mState = State.Playing;

        }else{
            Log.v(TAG, "onPrepared player");
        }
        updatePlaybackState();


    }



    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.v(TAG,"onError");
        Log.v(TAG, "onError " + mp.getDuration());
        //  mp.stop();
        updatePlaybackState();
        return false;
    }



    public void updatePlaybackState() {
        if (mState ==State.Playing || mState==State.Paused ){
            Log.v(TAG,"updatePlaybackState mState :" + mState);
            mMediaNotificationManager.startNotification();
            sendMessage();
        }else{
            mMediaNotificationManager.stopNotification();
            stopForeground(true);
        }
        AppController.getInstance().bookmarkAudioBook();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }
    public  void playAudioFile(String filePath, int seekPoint){
        seekPosition=seekPoint;
        Log.v(TAG,"playAudioFile filePath: "+filePath);
        if( timerUpdateThread != null ) {
            timerUpdateThread.interrupt();
        }
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            playAudioBook(filePath);
        }else {
            createPlayer();
            playAudioBook(filePath);
            Log.v(TAG,"playAudioFile filePath mediaPlayer null: "+filePath);

        }

    }
    public  int getSeekPosition(){
        if (mediaPlayer != null) {
            if(mState != State.Stopped){
                seekPosition= mediaPlayer.getCurrentPosition();
            }

        }
        return seekPosition;
    }
    public  void setSeekPosition(int position){
        seekPosition=position;
        Log.v(TAG,"setSeekPosition seekPosition:"+seekPosition);
//        if (mediaPlayer != null && mState != State.Stopped) {
//            Log.v(TAG,"seekPosition:"+seekPosition);
//                mediaPlayer.seekTo(seekPosition);
//            }

    }

    public  void seekToForward(boolean forward){
        Log.v(TAG,"seekToForward start" +mediaPlayer.getCurrentPosition());

        if(forward){
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 30000);

        }else{
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 30000);

        }
        Log.v(TAG,"seekToForward end" +mediaPlayer.getCurrentPosition());

    }

    // Send an Intent with an action named "my-event".
    private void sendMessage() {
        Log.v(TAG,"sendMessage start");

        if(mediaPlayer !=null && hasStartedPlayer) {
            Log.v(TAG, "sendMessage");

            Intent intent = new Intent(Constants.PLAYER_STATE_UPDATE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }else{
            //  stopForeground(true);
        }
    }
    private void stopThread(){
        keepRunning=false;
        if( timerUpdateThread != null ) {
            timerUpdateThread.interrupt();
            timerUpdateThread=null;
        }
    }
    private  void startPlayer(){
        Log.v(TAG,"startPlayer");

        stopThread();
        keepRunning=true;
        timerUpdateThread = new Thread( this );
        timerUpdateThread.start();
        mediaPlayer.start();
    }

    private void pausePlayer(){
        Log.v(TAG,"pausePlayer");
        stopThread();
        mediaPlayer.pause();
    }
    private void stopPlayer(){
        Log.v(TAG,"stopPlayer");

        stopThread();
        mediaPlayer.stop();
    }
    private void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mediaPlayer.isPlaying()) mediaPlayer.pause();
            return;
        } else if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
            if (mediaPlayer.isPlaying()){
                pausePlayer();

            }
            else {
                startPlayer();
            }
        }else{
            if (!mediaPlayer.isPlaying()) {
                startPlayer();
            }
        }
        // mediaPlayer.setVolume(1.0f, 1.0f); // we can be loud

        updatePlaybackState();
    }
    @Override
    public void onGainedAudioFocus() {
        mAudioFocus = AudioFocus.Focused;
        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {

        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            configAndStartMediaPlayer();
    }
    public State getPlaybackState(){
        return mState;
    }

    public void changePlayerState(String state){
        if(state =="pause"){
            if (mediaPlayer!=null  && mediaPlayer.isPlaying()) {
                seekPosition= mediaPlayer.getCurrentPosition();

                pausePlayer();
            }
            mState = State.Paused;
        }
        else if(state =="stop"){
            if (mediaPlayer!=null  && mediaPlayer.isPlaying()) {
                seekPosition= mediaPlayer.getCurrentPosition();

                stopPlayer();

            }
            hasStartedPlayer=false;
            mState = State.Stopped;
            mMediaNotificationManager.startNotification();

        }
        else if(state =="start"){
            if (mediaPlayer!=null ) {
                startPlayer();

            }
            mState = State.Playing;
        }
        updatePlaybackState();
    }


    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String state = intent.getStringExtra("state");
            if(state =="start"){
                if (mediaPlayer!=null ) {
                    startPlayer();

                }
                mState = State.Playing;
            }
            else if(state =="pause"){
                if (mediaPlayer!=null  && mediaPlayer.isPlaying()) {
                    seekPosition= mediaPlayer.getCurrentPosition();
                    pausePlayer();
                    AppController.getInstance().bookmarkAudioBook();

                }
                mState = State.Paused;
            }
            else if(state =="stop"){
                if (mediaPlayer!=null  && mediaPlayer.isPlaying()) {
                    seekPosition= mediaPlayer.getCurrentPosition();

                    AppController.getInstance().bookmarkAudioBook();
                    stopPlayer();

                }
                hasStartedPlayer=false;
                mState = State.Stopped;
            }
            updatePlaybackState();
        }
    };

}
