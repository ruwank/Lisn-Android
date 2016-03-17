package audio.lisn.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by Rasika on 9/19/15.
 */
public class PlayerActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.mediaPlayer.isPlaying()){
            sendStateChange(context,"pause");

        }else if(AudioPlayerService.mediaPlayer!=null){
            sendStateChange(context,"start");

        }
    }
    private void sendStateChange(Context context,String state) {
        Intent intent = new Intent(Constants.PLAYER_STATE_CHANGE);
        intent.putExtra("state", state);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
