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

import java.io.IOException;


public class PreviewAudioPlayerService extends Service implements Runnable, OnCompletionListener,
        OnPreparedListener,OnBufferingUpdateListener,MediaPlayer.OnErrorListener,MusicFocusable {

	public static MediaPlayer mediaPlayer;
    public static boolean hasStartedPlayer;
    private static final int NOTIFY_ID=1;
    private final IBinder mBinder = new PreviewAudioPlayerServiceBinder();
    AudioFocusHelper mAudioFocusHelper = null;
    //public static int seekPosition;
    public static int audioDuration;

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



    public class PreviewAudioPlayerServiceBinder extends Binder {
        public PreviewAudioPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PreviewAudioPlayerService.this;
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
	        mediaPlayer = new MediaPlayer();
	        /*  */  
	        mediaPlayer.setOnCompletionListener(this);
	    	mediaPlayer.setOnPreparedListener(this);
          mediaPlayer.setOnErrorListener(this);

          mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
          mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);

          // Register mMessageReceiver to receive messages.
          LocalBroadcastManager.getInstance(this).registerReceiver(mStateChangeReceiver,
                  new IntentFilter(Constants.PREVIEW_PLAYER_STATE_CHANGE));

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
	    if (mediaPlayer!=null) {
            if(mediaPlayer.isPlaying())
	      mediaPlayer.stop();
	      mediaPlayer.release();
	    }
          stopForeground(true);
	    mediaPlayer=null;
          giveUpAudioFocus();
          mState = State.Stopped;
          LocalBroadcastManager.getInstance(this).unregisterReceiver(mStateChangeReceiver);

      }

	  @Override  
	    public void run() {  
	        int currentPosition = 0;//   
	        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < mediaPlayer.getDuration()) {
	            try {  
	                Thread.sleep(1000);  
	                currentPosition = mediaPlayer.getCurrentPosition();

	            } catch (InterruptedException e) {  
	                e.printStackTrace();  
	            }
                sendMessage();
	        }
	  
	    }  
	  
	   
	  public void onCompletion(MediaPlayer mp) {
          mState = State.Stopped;
          if(mediaPlayer.isPlaying()){
              mediaPlayer.stop();
          }
          new Thread(this).interrupt();

         Log.v("onCompletion"," position"+mp.getCurrentPosition());
          //PlayerControllerView.musicSeekBar.setMax(0);
          //PlayerControllerView.playPauseButton.setImageResource(R.drawable.ic_action_play);
         // Log.v("onCompletion","onCompletion "+mp.getTrackInfo().length);
         //     AppController.getInstance().playNextFile();


	   // stopSelf();
	  }

	@Override
	public void onPrepared(MediaPlayer player) {

        if(player != null && player.getDuration()>0){
            Log.v("onPrepared","player duration "+player.getDuration());
            audioDuration=player.getDuration();
            tryToGetAudioFocus();
            player.start();
            new Thread(this).start();
            mState = State.Playing;

        }else{
            Log.v("onPrepared","player");
        }


    }
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.v("onError","onError");
      //  Log.v("onError","onError "+mp.getDuration());
       // mp.stop();
        return false;
    }

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		
	}
    public  void playAudioFile(String fileUrl){
        Log.v("playAudioFile ","filePath: "+fileUrl);
        new Thread(this).interrupt();

        if (mediaPlayer != null) {
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(fileUrl);
                mediaPlayer.prepareAsync();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }



    // Send an Intent with an action named "my-event".
    private void sendMessage() {
        if(mediaPlayer.isPlaying()) {
            Intent intent = new Intent("audio-event");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }else{
            stopForeground(true);
        }
    }
    private void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mediaPlayer.isPlaying()) mediaPlayer.pause();
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mediaPlayer.setVolume(0.1f, 0.1f); // we can be loud
        else
            mediaPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mediaPlayer.isPlaying()) mediaPlayer.start();
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



    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String state = intent.getStringExtra("state");
            if(state =="pause"){
                if (mediaPlayer!=null  && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                    mState = State.Paused;
            }
            else if(state =="stop"){
                if (mediaPlayer!=null  && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();

                }
                hasStartedPlayer=false;
                mState = State.Stopped;
            }
        }
    };

}
