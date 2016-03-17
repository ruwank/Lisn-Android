package audio.lisn.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import audio.lisn.R;
import audio.lisn.activity.AudioBookDetailActivity;
import audio.lisn.activity.PlayerControllerActivity;
import audio.lisn.adapter.MyBookViewAdapter;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.Log;
import audio.lisn.webservice.FileDownloadTask;
import audio.lisn.webservice.FileDownloadTaskListener;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link audio.lisn.fragment.MyBookFragment.OnStoreBookSelectedListener} interface
 * to handle interaction events.
 * Use the {@link audio.lisn.fragment.MyBookFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyBookFragment extends Fragment implements MyBookViewAdapter.MyBookSelectListener,FileDownloadTaskListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    private ProgressDialog pDialog;
    private List<AudioBook> bookList = new ArrayList<AudioBook>();
    private MyBookViewAdapter myBookViewAdapter;
    private RecyclerView myBookView;
    private static final String TAG = MyBookFragment.class.getSimpleName();
    private AudioBook selectedBook;
    ConnectionDetector connectionDetector;

    int requestCount,respondCount;
    View selectedView;
    ProgressDialog mProgressDialog;
    List<FileDownloadTask> downloadingList = new ArrayList<FileDownloadTask>();
    int totalAudioFileCount, downloadedFileCount;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StoreFragment.
     */
    public static MyBookFragment newInstance() {
        MyBookFragment fragment = new MyBookFragment();
        return fragment;
    }

    public MyBookFragment() {
        // Required empty public constructor
    }


    private void sendMessage() {
            Intent intent = new Intent("preview_audio-event");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(
                R.layout.fragment_my_book, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // BEGIN_INCLUDE (setup_viewpager)
        myBookView=(RecyclerView)view.findViewById(R.id.myBookContainer);
        myBookView.setLayoutManager(new GridLayoutManager(view.getContext(), 3));

    }

    
    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {

            connectionDetector = new ConnectionDetector(context);
            mProgressDialog = new ProgressDialog(context);
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

        } catch (Exception e) {

        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }
    @Override
    public void onResume() {
        super.onResume();
        loadData();
        updateMenu();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.v("onPause", "onPause");

    }

    private void hidePDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        super.onCreateOptionsMenu(menu, inflater);
    }


    private void loadData() {
        bookList.clear();
        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(getActivity().getApplicationContext());
       // downloadedAudioBook.readFileFromDisk(getActivity().getApplicationContext());
        HashMap< String, AudioBook> hashMap=downloadedAudioBook.getBookList(getActivity().getApplicationContext());
        for (AudioBook item : hashMap.values()) {
            bookList.add(item);
        }

        myBookViewAdapter = new MyBookViewAdapter(getActivity().getApplicationContext(),bookList);
        myBookViewAdapter.setMyBookSelectListener(this);
        myBookView.setAdapter(myBookViewAdapter);
    }
    private void stopPlayer(AudioBook audioBook) {
        if (AudioPlayerService.mediaPlayer != null) {
            String playingBookId= AppController.getInstance().getPlayingBookId();
            if(playingBookId.equalsIgnoreCase(audioBook.getBook_id()) ){
                AudioPlayerService.mediaPlayer.stop();
                AudioPlayerService.mediaPlayer.release();
                AudioPlayerService.mediaPlayer=null;
            }
        }
    }
    private void deleteAudioBook(AudioBook audioBook){
        stopPlayer(audioBook);

        audioBook.removeDownloadedFile(getContext());
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getActivity().getApplicationContext());
        downloadedAudioBook.readFileFromDisk(getActivity().getApplicationContext());
        downloadedAudioBook.addBookToList(getActivity().getApplicationContext(),
                audioBook.getBook_id(), audioBook);

        String message=getString(R.string.DELETE_BOOK_SUCCESS)+selectedBook.getEnglish_title()+"' from your device";
        Toast toast = Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }
    private void deleteBook(){

        if(selectedBook.isPurchase()) {
            String message="Are you sure you want to delete '"+selectedBook.getEnglish_title()+"' from your device?";

            AlertDialog confirmationDialog = new AlertDialog.Builder(getActivity())
                    //set message, title, and icon
                    .setTitle(R.string.DELETE_CONFIRMATION_TITLE)
                    .setMessage(message)
                    .setPositiveButton(R.string.BUTTON_YES, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            deleteAudioBook(selectedBook);
                            dialog.dismiss();

                        }

                    })

                    .setNegativeButton(R.string.BUTTON_NO, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();

                        }
                    })
                    .create();

            confirmationDialog.show();
        }

    }

    @Override
    public void onMyBookSelect(View view, AudioBook audioBook, AudioBook.SelectedAction btnIndex) {
        switch (btnIndex){

            case ACTION_DETAIL:
                AudioBookDetailActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;

            case ACTION_PLAY:
                PlayerControllerActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;
            case ACTION_DELETE:
                this.selectedBook=audioBook;
                deleteBook();
                break;
            case ACTION_DOWNLOAD: {
                this.selectedBook = audioBook;
                this.selectedView = view;
                downloadAudioFile();
            }
            break;

            default:
                break;

        }
    }
    private void downloadAudioFile() {
        String dirPath = AppUtils.getDataDirectory(getContext())
                + selectedBook.getBook_id()+ File.separator;
        File fileDir = new File(dirPath);
        if (!fileDir.exists()) {
            fileDir.mkdirs();

        }

        if (connectionDetector.isConnectingToInternet()) {

            mProgressDialog.show();

            downloadedFileCount=0;
            totalAudioFileCount=0;
            downloadingList.clear();

            for (int filePart=1; filePart<=(selectedBook.getAudioFileCount()); filePart++){
                File file = new File(dirPath +filePart+".lisn");

                if (!file.exists() ||  !(selectedBook.getDownloadedChapter().contains(filePart)) ) {
                    downloadAudioFileFromUrl(filePart);
                    totalAudioFileCount++;
                }


            }
            if(downloadedFileCount == totalAudioFileCount){
                mProgressDialog.dismiss();
                starAudioPlayer();


            }else{
                if(AppUtils.getAvailableMemory() < selectedBook.getFileSize()){
                    stopDownload();

                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            getActivity());
                    builder.setTitle(R.string.NO_ENOUGH_SPACE_TITLE)
                            .setMessage(R.string.NO_ENOUGH_SPACE_MESSAGE).setPositiveButton(
                            R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }else {
                    mProgressDialog.setMessage("Downloading " + (selectedBook.getDownloadedChapter().size() + 1) + " of " + selectedBook.getAudioFileCount());
                }

            }

        } else {

            downloadedFileCount=0;
            totalAudioFileCount=0;

            for (int filePart=1; filePart<=(selectedBook.getAudioFileCount()); filePart++){
                File file = new File(dirPath +filePart+".lisn");
                if (!file.exists()) {
                    totalAudioFileCount++;
                }


            }
            if(downloadedFileCount ==totalAudioFileCount){
                mProgressDialog.dismiss();
                starAudioPlayer();
            }else{
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
                        getActivity());
                builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(R.string.NO_INTERNET_MESSAGE).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                android.support.v7.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }
    private void downloadAudioFileFromUrl(int filePart){

        if (connectionDetector.isConnectingToInternet()) {
            String dirPath = AppUtils.getDataDirectory(getContext())
                    + selectedBook.getBook_id()+File.separator;
            File file = new File(dirPath + filePart + ".lisn");

            if (file.exists()) {
                file.delete();
            }
            FileDownloadTask downloadTask =  new FileDownloadTask(getContext(),this,selectedBook.getBook_id());
            downloadTask.execute(dirPath, "" + filePart);
            downloadingList.add(downloadTask);

        }else{
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
                    getActivity());
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(R.string.NO_INTERNET_MESSAGE).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private void starAudioPlayer() {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.DOWNLOAD_COMPLETE_TITLE).setMessage(getString(R.string.DOWNLOAD_COMPLETE_MESSAGE)).setPositiveButton(
                R.string.BUTTON_YES, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PlayerControllerActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), selectedView.findViewById(R.id.book_cover_thumbnail), selectedBook);


                    }
                })
                .setNegativeButton(R.string.BUTTON_NO, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                    }
                });
        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();


    }
    @Override
    public void onPostExecute(String result, String file_name) {
        if (result != null && result.equalsIgnoreCase("UNAUTHORISED")){
            showMessage("UNAUTHORISED");

        } else if (result != null && result.equalsIgnoreCase("NOTFOUND")) {
            showMessage("NOTFOUND");

        }else {
            mProgressDialog.setMessage("Downloading " + (selectedBook.getDownloadedChapter().size() + 1) + " of " + selectedBook.getAudioFileCount());

            downloadedFileCount++;
            if (result == null) {
                updateAudioBook(Integer.parseInt(file_name));

                if (totalAudioFileCount == downloadedFileCount) {
                    downloadAudioFile();
                }
            }
        }
    }
    private void showMessage(String result){
        stopDownload();
        mProgressDialog.dismiss();

        if (result.toUpperCase().contains("UNAUTHORISED")){
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.USER_UNAUTHORISED_TITLE).setMessage(getString(R.string.USER_UNAUTHORISED_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });

            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.show();

        }else if(result.toUpperCase().contains("NOTFOUND")){
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });

            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private void stopDownload(){
        for (int i = 0; i < downloadingList.size(); i++) {
            FileDownloadTask downloadTask = downloadingList.get(i);
            downloadTask.cancel(true);
        }
    }
    private void updateAudioBook(int chapter){
        if(chapter>0) {
            selectedBook.addChapterToDownloadedChapter(chapter);
        }
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getContext());
        downloadedAudioBook.readFileFromDisk(getContext());
        downloadedAudioBook.addBookToList(getContext(),
                selectedBook.getBook_id(), selectedBook);

    }
    public interface OnStoreBookSelectedListener {
        public void onStoreBookSelected(int position);

    }
    private void updateMenu() {
        Intent intent = new Intent(Constants.MENU_ITEM_SELECT);
        intent.putExtra("index", 2);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
    }

}