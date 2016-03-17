    package audio.lisn.view;

    import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.Constants;

    /**
     * Created by Rasika on 4/12/15.
     */
    public class PlayerControllerView extends LinearLayout{

        private  ImageButton playPauseButton;
        //bookmarkButton
        private  TextView audioTitle;
        private  TextView subTitle;
        private Context context;
        private ImageView bookCoverImage;

        public PlayerControllerView(Context context) {
            super(context);
           // LayoutInflater.from(context).inflate(R.layout.view_player_controller, this);
            initViews(context, null);
        }

        public PlayerControllerView(Context context, AttributeSet attrs) {
            super(context, attrs);
            initViews(context, attrs);
        }

        public PlayerControllerView(Context context, AttributeSet attrs, int defStyle) {
            this(context, attrs);
            initViews(context, attrs);
        }

        void initViews(Context context,AttributeSet attrs){
            this.context=context;

            LayoutInflater.from(context).inflate(R.layout.view_player_controller, this, true);
            playPauseButton=(ImageButton)this.findViewById(R.id.playPauseButton);
            bookCoverImage=(ImageView)this.findViewById(R.id.bookCoverImage);
            audioTitle=(TextView)this.findViewById(R.id.audioTitle);
            subTitle=(TextView)this.findViewById(R.id.subTitle);



            playPauseButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    playPauseAudio();

                }

            });
        AudioBook audioBook=AppController.getInstance().getCurrentAudioBook();
            if(audioBook !=null){
                audioTitle.setText(audioBook.getEnglish_title());

                String img_path = AppUtils.getDataDirectory(context)
                        + audioBook.getBook_id()+ File.separator+"book_cover.jpg";


                File imgFile = new  File(img_path);

                if(imgFile.exists()){
                    Picasso.with(context)
                            .load(imgFile)
                            .into(bookCoverImage);

                }
                else {
                    Picasso.with(context)
                            .load(audioBook.getCover_image())
                            .placeholder(R.drawable.ic_launcher)
                            .into(bookCoverImage);
                }

            }

            updateView();
        }
        private void playPauseAudio(){
            if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.mediaPlayer.isPlaying()){
                playPauseButton.setImageResource(R.drawable.btn_play_preview_start);
                sendStateChange("pause");
            }else if(AudioPlayerService.mediaPlayer!=null){
                playPauseButton.setImageResource(R.drawable.btn_play_preview_pause);
                AudioPlayerService.mediaPlayer.start(); // ?
                sendStateChange("start");

            }

        }
        public void stopAudioPlayer(){
            if(AudioPlayerService.mediaPlayer!=null){
                playPauseButton.setImageResource(R.drawable.btn_play_preview_start);
                sendStateChange("stop");
               // AppController.getInstance().stopPlayer();

            }


        }


        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
        }

        public void updateView(){
            AudioBook audioBook=AppController.getInstance().getCurrentAudioBook();
            if(audioBook !=null){
                audioTitle.setText(audioBook.getEnglish_title());

                String img_path = AppUtils.getDataDirectory(context)
                        + audioBook.getBook_id()+ File.separator+"book_cover.jpg";


                File imgFile = new  File(img_path);

                if(imgFile.exists()){
                    Picasso.with(context)
                            .load(imgFile)
                            .into(bookCoverImage);

                }
                else {
                    Picasso.with(context)
                            .load(audioBook.getCover_image())
                            .placeholder(R.drawable.ic_launcher)
                            .into(bookCoverImage);
                }

            }

            if(AudioPlayerService.mediaPlayer!=null){

                if(AudioPlayerService.mediaPlayer.isPlaying()){
                playPauseButton.setImageResource(R.drawable.btn_play_preview_pause);

            }else {
                playPauseButton.setImageResource(R.drawable.btn_play_preview_start);


            }
            }

            subTitle.setText(AppController.getInstance().getPlayerControllerTitle());
        }


        class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                /**/
                if (fromUser) {
                    if(AudioPlayerService.mediaPlayer!=null) {
                        AudioPlayerService.mediaPlayer.seekTo(progress);// ?
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(AudioPlayerService.mediaPlayer!=null) {
                    playPauseButton.setImageResource(R.drawable.btn_play_start);
                    AudioPlayerService.mediaPlayer.pause(); // ?
                   // mState = State.Playing;

                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(AudioPlayerService.mediaPlayer!=null) {
                    playPauseButton.setImageResource(R.drawable.btn_play_pause);
                    AudioPlayerService.mediaPlayer.start(); // ?
                }
            }
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
            intent.putExtra("state", state);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }



    }
