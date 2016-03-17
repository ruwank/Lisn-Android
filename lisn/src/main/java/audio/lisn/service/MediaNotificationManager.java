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

package audio.lisn.service;

//import android.annotation.SuppressLint;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import audio.lisn.R;
import audio.lisn.activity.PlayerControllerActivity;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.Constants;
import audio.lisn.util.Log;

//import android.media.session.PlaybackState;

//import android.support.v4.content.LocalBroadcastManager;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "audio.lisn.pause";
    public static final String ACTION_PLAY = "audio.lisn.play";
    public static final String ACTION_PREV = "audio.lisn.prev";
    public static final String ACTION_NEXT = "audio.lisn.next";
    public static final String ACTION_DELETE = "audio.lisn.delete";

    private final AudioPlayerService mService;


    private NotificationManager mNotificationManager;

    private PendingIntent mPauseIntent;
    private PendingIntent mPlayIntent;
    private PendingIntent mPreviousIntent;
    private PendingIntent mNextIntent;
    private PendingIntent mDeleteIntent;

    private int mNotificationColor;

    private boolean mStarted = false;

    public MediaNotificationManager(AudioPlayerService service) {
        mService = service;
        Log.v("Media","MediaNotificationManager start");

       // mController = new MediaController(mService.getApplicationContext());

        // updateSessionToken();

        mNotificationColor =mService.getResources().getColor(R.color.colorPrimary);

        //ResourceHelper.getThemeColor(mService,android.R.attr.colorPrimary, Color.DKGRAY);

        mNotificationManager = (NotificationManager) mService
                .getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mDeleteIntent= PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_DELETE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {

            // mNotificationManager.cancel(NOTIFICATION_ID);

//        if(mStarted){
//            stopNotification();
//        }
        //if (!mStarted) {
           // mMetadata = mController.getMetadata();
           // mPlaybackState = mController.getPlaybackState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
               // mController.registerCallback(mCb);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                filter.addAction(ACTION_DELETE);
                mService.registerReceiver(this, filter);

                mService.startForeground(NOTIFICATION_ID, notification);
                mStarted = true;
            }
        }else{
            mService.stopForeground(true);

            Notification notification = createNotification();
            if (notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
           // mController.unregisterCallback(mCb);
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            mService.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case ACTION_PAUSE:
                sendStateChange("pause");
                break;
            case ACTION_PLAY:
                sendStateChange("start");
                break;
            case ACTION_NEXT:
                AppController.getInstance().playNextFile();
                break;
            case ACTION_PREV:
                AppController.getInstance().playPreviousFile();
                break;
            case ACTION_DELETE:
                mService.stopForeground(true);
                sendStateChange("stop");

                Log.v("onReceive", "delete");
                break;
            default:
        }
    }
    private void sendStateChange(String state) {

        Intent intent = new Intent(Constants.PLAYER_STATE_CHANGE);
        intent.putExtra("state", state);
        LocalBroadcastManager.getInstance(mService.getApplicationContext()).sendBroadcast(intent);

    }

    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(mService, PlayerControllerActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Notification createNotification() {


        Notification.Builder notificationBuilder = new Notification.Builder(mService);
        int playPauseButtonPosition = 1;

        // If skip to previous action is enabled
        notificationBuilder.addAction(R.drawable.ic_play_skip_previous,
                mService.getString(R.string.label_previous), mPreviousIntent);

        addPlayPauseAction(notificationBuilder);

        // If skip to next action is enabled
        notificationBuilder.addAction(R.drawable.ic_play_skip_next,
                mService.getString(R.string.label_next), mNextIntent);

        AudioBook audioBook= AppController.getInstance().getCurrentAudioBook();

        String title="";
        if(audioBook !=null ){
            title=audioBook.getEnglish_title();
        }
        Bitmap art=null;

        art = BitmapFactory.decodeResource(mService.getResources(),
                R.drawable.ic_notification_large);


        notificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setUsesChronometer(true)
                .setContentIntent(createContentIntent())
                .setContentTitle(title)
                .setDeleteIntent(mDeleteIntent)
                .setContentText(AppController.getInstance().getPlayerControllerTitle())
                .setLargeIcon(art);

        if (Build.VERSION.SDK_INT >= 21){
            notificationBuilder.setStyle(new Notification.MediaStyle()
                    .setShowActionsInCompactView(
                            new int[]{playPauseButtonPosition})  // show only play/pause in compact view

            )                .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setColor(mNotificationColor)

            ;
        }

        setNotificationPlaybackState(notificationBuilder);


        return notificationBuilder.build();
    }

    private void addPlayPauseAction(Notification.Builder builder) {
        Log.v("addPlayPauseAction", "updatePlayPauseAction");
        String label;
        int icon;
        PendingIntent intent;
        if(AudioPlayerService.mediaPlayer!=null && AudioPlayerService.mediaPlayer.isPlaying()){
            label = mService.getString(R.string.label_pause);
            icon = R.drawable.ic_play_pause;
            intent = mPauseIntent;
        } else {
            label = mService.getString(R.string.label_play);
            icon = R.drawable.ic_play_start;
            intent = mPlayIntent;
        }
        builder.addAction(icon, label, intent);

    }

    private void setNotificationPlaybackState(Notification.Builder builder) {
        Log.v("addPlayPauseAction", "updatePlayPauseAction");

      //  LogHelper.d(TAG, "updateNotificationPlaybackState. mPlaybackState=" + mPlaybackState);
        if (AudioPlayerService.mediaPlayer == null || !mStarted) {
          //  LogHelper.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            mService.stopForeground(true);
            return;
        }
        if (AudioPlayerService.mediaPlayer.isPlaying()
                && AudioPlayerService.seekPosition >= 0) {
            Log.v("addPlayPauseAction", "updatePlayPauseAction isPlaying");


            builder
                    .setWhen(System.currentTimeMillis() - AudioPlayerService.mediaPlayer.getCurrentPosition())
                    .setShowWhen(true)
                .setUsesChronometer(true);
        } else {
            Log.v("addPlayPauseAction", "updatePlayPauseAction not isPlaying");

            //  LogHelper.d(TAG, "updateNotificationPlaybackState. hiding playback position");
            builder
                .setWhen(0)
                .setShowWhen(false)
                .setUsesChronometer(false);
          //  mService.stopForeground(true);

        }
        // Make sure that the notification can be dismissed by the user when we are not playing:
       // builder.setOngoing(false);
        builder.setOngoing(AudioPlayerService.mediaPlayer.isPlaying());

    }

}
