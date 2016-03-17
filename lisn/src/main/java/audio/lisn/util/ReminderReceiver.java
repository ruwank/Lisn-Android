package audio.lisn.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.text.Html;

import java.util.Random;

import audio.lisn.R;
import audio.lisn.activity.MainActivity;

/**
 * Created by Rasika on 11/28/15.
 */

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("AlarmReceiver","AlarmReceiver");
        String quotesArray[]=context.getResources().getStringArray(R.array.quotes);
        Random r = new Random();
        int index = r.nextInt(quotesArray.length);
        String quotes= quotesArray[index];
        Bitmap art = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_launcher);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(context.getString(R.string.app_name));
        bigTextStyle.bigText(Html.fromHtml(quotes));

        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(art)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(Html.fromHtml(quotes))
                        .setAutoCancel(true)
                        .setStyle(bigTextStyle);;

        Intent resultIntent = new Intent(context, MainActivity.class);

// no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        // Sets an ID for the notification
        int mNotificationId = 001;
// Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
// Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }
}
