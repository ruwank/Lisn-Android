/*
 * Copyright (C) 2015 Antonio Leiva
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package audio.lisn.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.CustomTypeFace;
import audio.lisn.util.Log;
import audio.lisn.util.WakeLocker;
import audio.lisn.view.EllipsizingTextView;

public class StoreBookViewAdapter extends RecyclerView.Adapter<StoreBookViewAdapter.ViewHolder> implements Runnable {

    private List<AudioBook> items;
    private StoreBookSelectListener listener;
    MediaPlayer mediaPlayer = null;
    ConnectionDetector connectionDetector;
    AudioBook selectedAudioBook;
    private boolean isPlayingPreview,isLoadingPreview;
    int selectedBookIndex;
    private Context context;
    View selectedView;
    AudioBook.SelectedAction selectedAction= AudioBook.SelectedAction.ACTION_MORE;

    Thread timerUpdateThread;

    String leftTime;

    public StoreBookViewAdapter(Context context, List<AudioBook> items) {
        this.items = items;
        this.context=context;
        connectionDetector = new ConnectionDetector(context);

    }

    public void setStoreBookSelectListener(StoreBookSelectListener onItemClickListener) {
        this.listener = onItemClickListener;
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_store_book, parent, false);
        view.setMinimumWidth(parent.getMeasuredWidth());


        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                releaseMediaPlayer();

                if (listener != null) {

                    new Handler().postDelayed(new Runnable() {
                        @Override public void run() {
                            
                            listener.onStoreBookSelect(v,(AudioBook) v.getTag(),AudioBook.SelectedAction.ACTION_DETAIL);
                        }
                    }, 200);
                }
            }
        });


        return new ViewHolder(view);
    }




    @Override public void onBindViewHolder(final ViewHolder holder, int position) {
        final AudioBook book = items.get(position);
        selectedBookIndex=position;

        if((isLoadingPreview || isPlayingPreview) && selectedAudioBook != null && selectedAudioBook.getBook_id().equalsIgnoreCase(book.getBook_id()) ){
            holder.previewLayout.setVisibility(View.VISIBLE);
            holder.playButton.setImageResource(R.drawable.btn_play_preview_pause);

            if(isPlayingPreview){
                holder.spinner.setVisibility(View.INVISIBLE);
                holder.previewLabel.setText("Preview");
                holder.timeLabel.setText(leftTime);

            }else{
                holder.spinner.setVisibility(View.VISIBLE);
                holder.previewLabel.setText("Loading...");
                holder.timeLabel.setText("");
            }
        }else{
            holder.previewLayout.setVisibility(View.GONE);
            holder.playButton.setImageResource(R.drawable.btn_play_preview_start);
        }
        if(book.getLanguageCode()== AudioBook.LanguageCode.LAN_SI){
            holder.title.setTypeface(CustomTypeFace.getSinhalaTypeFace(holder.title.getContext()));
            holder.author.setTypeface(CustomTypeFace.getSinhalaTypeFace(holder.author.getContext()));
            holder.title.setEllipsized("'''");
            holder.author.setEllipsized("'''");
        }else{
            holder.title.setTypeface(CustomTypeFace.getEnglishTypeFace(holder.title.getContext()));
            holder.author.setTypeface(CustomTypeFace.getEnglishTypeFace(holder.author.getContext()));
            holder.title.setEllipsized("...");
            holder.author.setEllipsized("...");

        }
        if(book.isAwarded()){
            holder.awardIcon.setVisibility(View.VISIBLE);

        }else{
            holder.awardIcon.setVisibility(View.GONE);
        }
        holder.title.setText(book.getTitle());
        holder.title.setContentDescription(book.getEnglish_title());
        holder.author.setText(book.getAuthor());
        String priceText="Free";
        if( Float.parseFloat(book.getPrice())>0 ){
            priceText="Rs. "+book.getPrice();
        }
        if(AppController.getInstance().isUserLogin() && book.isPurchase()){
            holder.downloadedIcon.setVisibility(View.VISIBLE);
            holder.price.setVisibility(View.GONE);

        }else {
            holder.downloadedIcon.setVisibility(View.GONE);
            holder.price.setVisibility(View.VISIBLE);


        }
        holder.price.setText(priceText);
        LayerDrawable stars = (LayerDrawable) holder.ratingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(ContextCompat.getColor(holder.ratingBar.getContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);

        if(Float.parseFloat(book.getRate())>-1){
            holder.ratingBar.setRating(Float.parseFloat(book.getRate()));
        }

        holder.ratingBar.setIsIndicator(true);
        holder.thumbNail.setImageBitmap(null);

            Picasso.with(holder.thumbNail.getContext())
                    .load(book.getCover_image())
                    .placeholder(R.drawable.ic_launcher)
                    .into(holder.thumbNail);

        holder.optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                if(AppController.getInstance().isUserLogin() && book.isPurchase()){
                    if(book.getChapters().size() == book.getDownloadedChapter().size()){
                        popupMenu.inflate(R.menu.store_book_menu_downloaded);

                    }else{
                        popupMenu.inflate(R.menu.store_book_menu_free);

                    }

                }else {
                    if (Float.parseFloat(book.getPrice()) < 1) {
                        popupMenu.inflate(R.menu.store_book_menu_free);

                    }else{
                        popupMenu.inflate(R.menu.store_book_menu);

                    }
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        AudioBook audioBook = items.get(selectedBookIndex);

                        switch (item.getItemId()) {
                            case R.id.action_preview:
                                releaseMediaPlayer();
                                selectedView=holder.itemView;
                                playButtonPressed((AudioBook)holder.itemView.getTag());
                                break;
                            case R.id.action_purchase:
                                selectedAction=AudioBook.SelectedAction.ACTION_PURCHASE;


                                break;
                            case R.id.action_detail:
                                selectedAction=AudioBook.SelectedAction.ACTION_DETAIL;


                                break;
                            case R.id.action_play:
                                selectedAction=AudioBook.SelectedAction.ACTION_PLAY;


                                break;
                            case R.id.action_download:
                                selectedAction=AudioBook.SelectedAction.ACTION_DOWNLOAD;

                                break;
                            default:
                                break;

                        }
                        if(selectedAction !=AudioBook.SelectedAction.ACTION_MORE) {
                            releaseMediaPlayer();
                            if (listener != null) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onStoreBookSelect(holder.itemView, (AudioBook) holder.itemView.getTag(), selectedAction);
                                    }
                                }, 200);
                            }
                        }

                        return true;
                    }
                });
                popupMenu.show();



            }
        });

        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedView=holder.itemView;
                playButtonPressed((AudioBook)holder.itemView.getTag());

            }
        });
        holder.itemView.setTag(book);
    }

    @Override public int getItemCount() {
        return items.size();
    }



    protected static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView thumbNail;
        public TextView price;
        public EllipsizingTextView title, author;
        public RatingBar ratingBar;
        public ImageButton optionButton,playButton;
        public RelativeLayout previewLayout;
        public TextView previewLabel,timeLabel;
        public ProgressBar spinner;
        public ImageView downloadedIcon,awardIcon;



        public ViewHolder(View itemView) {
            super(itemView);

            thumbNail=(ImageView) itemView
                    .findViewById(R.id.book_cover_thumbnail);
            title= (EllipsizingTextView) itemView.findViewById(R.id.book_title);
            author= (EllipsizingTextView) itemView.findViewById(R.id.book_author);
            price= (TextView) itemView.findViewById(R.id.book_price);
            ratingBar=(RatingBar)itemView.findViewById(R.id.rating_bar);
            optionButton=(ImageButton)itemView.findViewById(R.id.btn_action);
            playButton=(ImageButton)itemView.findViewById(R.id.playButton);
            previewLayout=(RelativeLayout)itemView.findViewById(R.id.preview_layout);
            previewLabel=(TextView)itemView.findViewById(R.id.preview_label);
            timeLabel=(TextView)itemView.findViewById(R.id.time_label);
            spinner = (ProgressBar)itemView.findViewById(R.id.progressBar);
            downloadedIcon = (ImageView)itemView.findViewById(R.id.downloaded_icon);
            awardIcon = (ImageView)itemView.findViewById(R.id.award_icon);
            spinner.getIndeterminateDrawable().setColorFilter(
                    ContextCompat.getColor(itemView.getContext(), R.color.whiteColor),
                    android.graphics.PorterDuff.Mode.SRC_IN);



        }
    }
    private void playButtonPressed(AudioBook audioBook){
        try {
            stopPreviewPlayer();
            if (audioBook.getPreview_audio() != null && (audioBook.getPreview_audio().length() > 0)) {
                boolean stopPlayer = false;
                if (selectedAudioBook != null) {
                    if ((isLoadingPreview || isPlayingPreview) && (audioBook.getBook_id().equalsIgnoreCase(selectedAudioBook.getBook_id()))) {
                        stopPlayer = true;
                    }
                }
                selectedAudioBook = audioBook;
                if (stopPlayer && mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        if (timerUpdateThread != null) {
                            timerUpdateThread.interrupt();
                        }
                    }

                    mediaPlayer.reset();
                    isPlayingPreview = false;
                    isLoadingPreview = false;

                } else {
                    playPreview();
                }

                // AppController.getInstance().playPreviewFile(audioBook.getPreview_audio());
            } else {
                if (selectedAudioBook != null && isPlayingPreview) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        if (timerUpdateThread != null) {
                            timerUpdateThread.interrupt();
                        }
                    }

                    mediaPlayer.reset();
                    isPlayingPreview = false;
                    isLoadingPreview = false;

                }
            }
            notifyDataSetChanged();
        }
        catch (Exception e){

        }
    }
    @Override
    public void run() {
        int currentPosition = 0;//
        while (isPlayingPreview && mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < mediaPlayer.getDuration()) {
            try {
                Thread.sleep(1000);
                currentPosition = mediaPlayer.getCurrentPosition();

            } catch (Exception e) {
                e.printStackTrace();
            }
            updateTimer();
        }
    }

    private void updateTimer() {
        try {
            int currentPosition = mediaPlayer.getCurrentPosition();
            int totalDuration =mediaPlayer.getDuration();
            leftTime= AppUtils.milliSecondsToTimer(totalDuration - currentPosition);
            // Get a handler that can be used to post to the main thread
            Handler mainHandler = new Handler(context.getMainLooper());

            Runnable timerRunnable = new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();

                } // This is your code
            };
            mainHandler.post(timerRunnable);
        }
        catch (Exception e){

        }

    }
    private void playPreview( ) {
        isLoadingPreview=true;
        isPlayingPreview=false;
        pausePlayer();
        notifyDataSetChanged();

        if (connectionDetector.isConnectingToInternet()) {

            //((Activity)context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                if( timerUpdateThread != null ) {
                    timerUpdateThread.interrupt();
                }
            }

            mediaPlayer.reset();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(selectedAudioBook.getPreview_audio());
            }catch (IOException e) {
                Log.v("playPreview", "IOException" + e.getMessage());

                e.printStackTrace();
            }
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    WakeLocker.acquire(context);

                    isPlayingPreview = true;
                    isLoadingPreview = false;
                    startTimer();
                    mp.start();
                    notifyDataSetChanged();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    notifyDataSetChanged();

                    String msg = "";

                    if (extra == MediaPlayer.MEDIA_ERROR_IO) {
                        msg = "MEDIA_ERROR_IO";
                    } else if (extra == MediaPlayer.MEDIA_ERROR_MALFORMED) {
                        msg = "MEDIA_ERROR_MALFORMED";
                    } else if (extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
                        msg = "MEDIA_ERROR_UNSUPPORTED";
                    } else if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                        msg = "MEDIA_ERROR_TIMED_OUT";
                    }  else {
                        msg = "video_error_unknown_error";
                    }
                    showErrorMessage(msg);


                    return false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlayingPreview=false;
                    isLoadingPreview=false;
                    stopTimer();
                    notifyDataSetChanged();
                    WakeLocker.release();
                }
            });
            try {
                mediaPlayer.prepareAsync(); // prepare async to not block main
            }catch (Exception e){
                showErrorMessage(e.getMessage());


                isPlayingPreview=false;
                isLoadingPreview=false;
                stopTimer();
                notifyDataSetChanged();
            }


        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(selectedView.getContext());
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(R.string.NO_INTERNET_MESSAGE).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
private void showErrorMessage(String msg){

}
    private void startTimer(){
        if( timerUpdateThread != null ) {
            timerUpdateThread.interrupt();
        }
        timerUpdateThread = new Thread( this );
        timerUpdateThread.start();

        //new Thread(this).start();
    }
    private void stopTimer(){

        if( timerUpdateThread != null ) {
            timerUpdateThread.interrupt();
        }
    }

    public void releaseMediaPlayer(){

        isPlayingPreview=false;
        isLoadingPreview=false;
        if (mediaPlayer != null){
            if( timerUpdateThread != null ) {
                timerUpdateThread.interrupt();
            }
            stopTimer();
            Log.v("mediaPlayer","releaseMediaPlayer");
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer=null;

        }

    }
    private void pausePlayer() {
        Intent intent = new Intent(Constants.PLAYER_STATE_CHANGE);
        intent.putExtra("state", "pause");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    private void stopPreviewPlayer() {
        Intent intent = new Intent(Constants.PLAYER_STATE_CHANGE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    public interface StoreBookSelectListener
    {
        public void onStoreBookSelect(View view, AudioBook audioBook,AudioBook.SelectedAction btnIndex);
    }


}
