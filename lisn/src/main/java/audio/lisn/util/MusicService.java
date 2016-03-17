 /*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package audio.lisn.util;

 import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;

import java.lang.ref.WeakReference;

import audio.lisn.activity.PlayerControllerActivity;
import audio.lisn.service.MediaNotificationManager;

 /**
  * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
  * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
  * exposes it through its MediaSession.Token, which allows the client to create a MediaController
  * that connects to and send control commands to the MediaSession remotely. This is useful for
  * user interfaces that need to interact with your media session, like Android Auto. You can
  * (should) also use the same service from your app's UI, which gives a seamless playback
  * experience to the user.
  *
  * To implement a MediaBrowserService, you need to:
  *
  * <ul>
  *
  * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
  *      related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
  *      {@link android.service.media.MediaBrowserService#onLoadChildren};
  * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
  *      with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
  *
  * <li> Set a callback on the
  *      {@link android.media.session.MediaSession#setCallback(android.media.session.MediaSession.Callback)}.
  *      The callback will receive all the user's actions, like play, pause, etc;
  *
  * <li> Handle all the actual music playing using any method your app prefers (for example,
  *      {@link android.media.MediaPlayer})
  *
  * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
  *      {@link android.media.session.MediaSession#setPlaybackState(android.media.session.PlaybackState)}
  *      {@link android.media.session.MediaSession#setMetadata(android.media.MediaMetadata)} and
  *      {@link android.media.session.MediaSession#setQueue(java.util.List)})
  *
  * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
  *      android.media.browse.MediaBrowserService
  *
  * </ul>
  *
  * To make your app compatible with Android Auto, you also need to:
  *
  * <ul>
  *
  * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
  *      with a &lt;automotiveApp&gt; root element. For a media app, this must include
  *      an &lt;uses name="media"/&gt; element as a child.
  *      For example, in AndroidManifest.xml:
  *          &lt;meta-data android:name="com.google.android.gms.car.application"
  *              android:resource="@xml/automotive_app_desc"/&gt;
  *      And in res/values/automotive_app_desc.xml:
  *          &lt;automotiveApp&gt;
  *              &lt;uses name="media"/&gt;
  *          &lt;/automotiveApp&gt;
  *
  * </ul>

  * @see <a href="README.md">README.md</a> for more details.
  *
  */

 public class MusicService extends Service implements Playback.Callback {

     // The action of the incoming Intent indicating that it contains a command
     // to be executed (see {@link #onStartCommand})
     public static final String ACTION_CMD = "audio.lisn.ACTION_CMD";
     // The key in the extras of the incoming Intent indicating the command that
     // should be executed (see {@link #onStartCommand})
     public static final String CMD_NAME = "CMD_NAME";
     // A value of a CMD_NAME key in the extras of the incoming Intent that
     // indicates that the music playback should be paused (see {@link #onStartCommand})
     public static final String CMD_PAUSE = "CMD_PAUSE";

     private static final String TAG = "MusicService";
     // Action to thumbs up a media item

     // Delay stopSelf by using a handler.
     private static final int STOP_DELAY = 30000;

     // Music catalog manager
     private MediaSession mSession;
     // "Now playing" queue:
     //private List<MediaSession.QueueItem> mPlayingQueue;
     private int mCurrentIndexOnQueue;
     private MediaNotificationManager mMediaNotificationManager;
     // Indicates whether the service was started.
     private boolean mServiceStarted;
     private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
     private Playback mPlayback;
     private final IBinder mBinder = new MusicServiceBinder();
     private String[] mFileList;
     /*
      * (non-Javadoc)
      * @see android.app.Service#onCreate()
      */
     @Override
     public void onCreate() {
         super.onCreate();
         Log.v(TAG, "onCreate");

         // mMusicProvider = new MusicProvider();

         // Start a new MediaSession
         mSession = new MediaSession(this, "MusicService");
         //mSession.setCallback(new MediaSessionCallback());
         mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
             MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

         mPlayback = new Playback(this);
         mPlayback.setState(PlaybackState.STATE_NONE);
         mPlayback.setCallback(this);
         mPlayback.start();

         Context context = getApplicationContext();
         Intent intent = new Intent(context, PlayerControllerActivity.class);
         PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                 intent, PendingIntent.FLAG_UPDATE_CURRENT);
         mSession.setSessionActivity(pi);

         Bundle extras = new Bundle();
         mSession.setExtras(extras);


        // mMediaNotificationManager = new MediaNotificationManager(this);
     }

     /**
      * (non-Javadoc)
      * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
      */
     @Override
     public int onStartCommand(Intent startIntent, int flags, int startId) {
         if (startIntent != null) {
             String action = startIntent.getAction();
             String command = startIntent.getStringExtra(CMD_NAME);
             if (ACTION_CMD.equals(action)) {
                 if (CMD_PAUSE.equals(command)) {
                     if (mPlayback != null && mPlayback.isPlaying()) {
                         handlePauseRequest();
                     }
                 }
             }
         }
         return START_STICKY;
     }

     /**
      * (non-Javadoc)
      * @see android.app.Service#onDestroy()
      */
     @Override
     public void onDestroy() {
         Log.v(TAG, "onDestroy");
         // Service is being killed, so make sure we release our resources
         handleStopRequest(null);

         mDelayedStopHandler.removeCallbacksAndMessages(null);
         // Always release the MediaSession to clean up resources
         // and notify associated MediaController(s).
         mSession.release();
     }

     public class MusicServiceBinder extends Binder {
         public MusicService getService() {
             // Return this instance of LocalService so clients can call public methods
             return MusicService.this;
         }
     }

     @Override
     public IBinder onBind(Intent intent) {
         return mBinder;
     }
     public  void playAudioFiles(String[] filesPath){
         this.mFileList=filesPath;
         mCurrentIndexOnQueue=0;
         handlePlayRequest();

     }



     /**
      * Handle a request to play music
      */
     private void handlePlayRequest() {
         Log.v(TAG, "handlePlayRequest: mState=" + mPlayback.getState());

         mDelayedStopHandler.removeCallbacksAndMessages(null);
         if (!mServiceStarted) {
             Log.v(TAG, "Starting service");
             // The MusicService needs to keep running even after the calling MediaBrowser
             // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
             // need to play media.
             startService(new Intent(getApplicationContext(), MusicService.class));
             mServiceStarted = true;
         }

         if (!mSession.isActive()) {
             mSession.setActive(true);
         }
         if (mCurrentIndexOnQueue >= 0 && mCurrentIndexOnQueue < (mFileList.length)) {


             mPlayback.play(mFileList[mCurrentIndexOnQueue]);
         }
     }

     /**
      * Handle a request to pause music
      */
     private void handlePauseRequest() {
         Log.v(TAG, "handlePauseRequest: mState=" + mPlayback.getState());
         mPlayback.pause();
         // reset the delayed stop handler.
         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
     }

     /**
      * Handle a request to stop music
      */
     private void handleStopRequest(String withError) {
         Log.v(TAG, "handleStopRequest: mState=" + mPlayback.getState() + " error="+ withError);
         mPlayback.stop(true);
         // reset the delayed stop handler.
         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

         updatePlaybackState(withError);

         // service is no longer necessary. Will be started again if needed.
         stopSelf();
         mServiceStarted = false;
     }


     /**
      * Update the current media player state, optionally showing an error message.
      *
      * @param error if not null, error message to present to the user.
      */
     private void updatePlaybackState(String error) {
        // LogHelper.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getState());
         long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
         if (mPlayback != null && mPlayback.isConnected()) {
             position = mPlayback.getCurrentStreamPosition();
         }

         PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                 .setActions(getAvailableActions());

         int state = mPlayback.getState();

         // If there is an error message, send it to the playback state:
         if (error != null) {
             // Error states are really only supposed to be used for errors that cause playback to
             // stop unexpectedly and persist until the user takes action to fix it.
             stateBuilder.setErrorMessage(error);
             state = PlaybackState.STATE_ERROR;
         }
         stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());



         mSession.setPlaybackState(stateBuilder.build());

         if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED) {
             mMediaNotificationManager.startNotification();
         }
     }



     private long getAvailableActions() {
         long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                 PlaybackState.ACTION_PLAY_FROM_SEARCH;
         if (mFileList == null || mFileList.length<1) {
             return actions;
         }
         if (mPlayback.isPlaying()) {
             actions |= PlaybackState.ACTION_PAUSE;
         }
         if (mCurrentIndexOnQueue > 0) {
             actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
         }
         if (mCurrentIndexOnQueue < mFileList.length - 1) {
             actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
         }
         return actions;
     }



     /**
      * Implementation of the Playback.Callback interface
      */
     @Override
     public void onCompletion() {
         // The media player finished playing the current song, so we go ahead
         // and start the next.
         if (mFileList != null && !(mFileList.length>0)) {
             // In this sample, we restart the playing queue when it gets to the end:
             mCurrentIndexOnQueue++;
             if (mCurrentIndexOnQueue >= mFileList.length) {
                 mCurrentIndexOnQueue = 0;
             }
             handlePlayRequest();
         } else {
             // If there is nothing to play, we stop and release the resources:
             handleStopRequest(null);
         }
     }

     @Override
     public void onPlaybackStatusChanged(int state) {
         updatePlaybackState(null);
     }

     @Override
     public void onError(String error) {
         updatePlaybackState(error);
     }

     /**
      * A simple handler that stops the service if playback is not active (playing)
      */
     private static class DelayedStopHandler extends Handler {
         private final WeakReference<MusicService> mWeakReference;

         private DelayedStopHandler(MusicService service) {
             mWeakReference = new WeakReference<>(service);
         }

         @Override
         public void handleMessage(Message msg) {
             MusicService service = mWeakReference.get();
             if (service != null && service.mPlayback != null) {
                 if (service.mPlayback.isPlaying()) {
                   //  LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                     return;
                 }
                // LogHelper.d(TAG, "Stopping service with delay handler.");
                 service.stopSelf();
                 service.mServiceStarted = false;
             }
         }
     }
 }
