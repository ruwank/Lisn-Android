package audio.lisn.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.activity.AudioBookDetailActivity;
import audio.lisn.activity.PlayerControllerActivity;
import audio.lisn.adapter.StoreBookViewAdapter;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Log;
import audio.lisn.webservice.FileDownloadTask;
import audio.lisn.webservice.FileDownloadTaskListener;
import audio.lisn.webservice.JsonUTF8ArrayRequest;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StoreFragment.OnStoreBookSelectedListener} interface
 * to handle interaction events.
 * Use the {@link StoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoreFragment extends Fragment implements  StoreBookViewAdapter.StoreBookSelectListener,FileDownloadTaskListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    private OnStoreBookSelectedListener mListener;
    ConnectionDetector connectionDetector;
    private ProgressDialog pDialog;
    private List<AudioBook> bookList = new ArrayList<AudioBook>();
    private StoreBookViewAdapter storeBookViewAdapter;
    private RecyclerView storeBookView;
    private static final String TAG = StoreFragment.class.getSimpleName();
    private AudioBook selectedBook;
    int bookCategory;
    List<FileDownloadTask> downloadingList = new ArrayList<FileDownloadTask>();
    ProgressDialog mProgressDialog;
    View selectedView;
    int totalAudioFileCount, downloadedFileCount;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StoreFragment.
     */
//    public static StoreFragment newInstance() {
//        StoreFragment fragment = new StoreFragment();
//        return fragment;
//    }

    public StoreFragment() {
        // Required empty public constructor
    }
    public static StoreFragment newInstance() {
        StoreFragment fragment = new StoreFragment(1);
        return fragment;
    }
    public StoreFragment(int bookCategory) {
        super();
        this.bookCategory=bookCategory;
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
        Log.v(TAG, "onCreateView");

        return inflater.inflate(
                R.layout.fragment_store, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // BEGIN_INCLUDE (setup_viewpager)
        storeBookView=(RecyclerView)view.findViewById(R.id.storeBookContainer);
        storeBookView.setLayoutManager(new GridLayoutManager(view.getContext(), 3));

    }

    @Override
        public void onAttach(Context context) {

            super.onAttach(context);
        try {
            if (context instanceof Activity){
                mListener = (OnStoreBookSelectedListener)(Activity) context;
            }
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
        mListener = null;

    }
    @Override
    public void onResume() {
        super.onResume();
        loadData();
        registerBroadcastReceiver();
        Log.v(TAG, "onResume");


    }

    @Override
    public void onPause() {
        super.onPause();
        removePlayer();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mPlayerUpdateReceiver);

        Log.v(TAG, "onPause");

    }
    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (!visible) {
            removePlayer();
        }else{
            if (isResumed()){
                loadData();
            }
        }
    }
    private void stopDownload(){
        for (int i = 0; i < downloadingList.size(); i++) {
            FileDownloadTask downloadTask = downloadingList.get(i);
            downloadTask.cancel(true);
        }
    }
    private void showProgress(){
        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage(getString(R.string.loading_text));
        pDialog.show();
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
    private void downloadData() {

        Map<String, String> params = new HashMap<String, String>();
        params.put("cat", "" + bookCategory);

        String url = getString(R.string.book_category_url);


        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(Request.Method.POST,url, params,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setStoreBookForCategory(bookCategory,jsonArray);
                        setData(jsonArray);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                hidePDialog();

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_boo_list");
    }

    private void setData(JSONArray jsonArray){
        hidePDialog();

        bookList.clear();
            for (int i = 0; (i < jsonArray.length()); i++) {
                try {

                    JSONObject obj = jsonArray.getJSONObject(i);
//                    String book_id = "";
//                    try {
//                        book_id = obj.getString("book_id");
//                    } catch (JSONException e) {
//                        book_id = obj.getString("" + i);
//                        e.printStackTrace();
//                    }
                    AudioBook book = new AudioBook(obj, i,getActivity());


                    bookList.add(book);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


        }

        // notifying list adapter about data changes
        // so that it renders the list view with updated data
        storeBookViewAdapter.notifyDataSetChanged();
    }

    private void loadData() {
        storeBookViewAdapter = new StoreBookViewAdapter(getActivity(),bookList);
        storeBookViewAdapter.setStoreBookSelectListener(this);
        storeBookView.setAdapter(storeBookViewAdapter);


        if (connectionDetector.isConnectingToInternet()) {
            showProgress();

            // Cache data not exist.
            JSONArray jsonArray = AppController.getInstance().getStoreBookForCategory(bookCategory);
            if (jsonArray != null) {
                setData(jsonArray);
            } else {
                downloadData();
            }

        }else{
            JSONArray jsonArray = AppController.getInstance().getStoreBookForCategory(bookCategory);
            if (jsonArray != null) {
                setData(jsonArray);
            }
        }


    }

    private void registerBroadcastReceiver(){
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mPlayerUpdateReceiver,
                new IntentFilter("audio-event"));
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mPlayerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            if(AudioPlayerService.mediaPlayer!=null && AudioPlayerService.mediaPlayer.isPlaying()) {
                if(storeBookViewAdapter !=null)
                {
                    storeBookViewAdapter.releaseMediaPlayer();
                    storeBookViewAdapter.notifyDataSetChanged();

                }
            }
        }
    };
    public void removePlayer(){
        if(storeBookViewAdapter !=null)
        {
            storeBookViewAdapter.releaseMediaPlayer();

        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("onActivityResult", "onActivityResult StoreFragment");


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


            for (int filePart=1; filePart<=(selectedBook.getChapters().size()); filePart++){
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

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                        getActivity());
                builder.setTitle(R.string.NO_ENOUGH_SPACE_TITLE).setMessage(R.string.NO_ENOUGH_SPACE_MESSAGE).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                android.app.AlertDialog dialog = builder.create();
                dialog.show();

            }else{
                    int downloadedChapter=selectedBook.getDownloadedChapter().size()+1;
                    if(downloadedChapter>selectedBook.getChapters().size()){
                        downloadedChapter=selectedBook.getChapters().size();
                    }
                mProgressDialog.setMessage("Downloading " + (downloadedChapter) + " of " + selectedBook.getChapters().size());
            }

            }

        } else {

            downloadedFileCount=0;
            totalAudioFileCount=0;

            for (int filePart=1; filePart<=(selectedBook.getChapters().size()); filePart++){
                File file = new File(dirPath +filePart+".lisn");
                if (!file.exists()) {
                    totalAudioFileCount++;
                }


            }
            if(downloadedFileCount ==totalAudioFileCount){
                mProgressDialog.dismiss();
                starAudioPlayer();
            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
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
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    getActivity());
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
    private void starAudioPlayer() {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.DOWNLOAD_COMPLETE_TITLE).setMessage(getString(R.string.DOWNLOAD_COMPLETE_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_YES, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            PlayerControllerActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), selectedView.findViewById(R.id.book_cover_thumbnail), selectedBook,-1);

                          //  PlayerControllerActivity.navigate(AudioBookDetailActivity.this,bookCoverImage, audioBook);

                        }
                    })
                    .setNegativeButton(R.string.BUTTON_NO, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();


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


    private void saveCoverImage() {
        String dirPath = AppUtils.getDataDirectory(selectedView.getContext())
                + selectedBook.getBook_id() + File.separator;
        ImageView bookCoverImage=(ImageView)selectedView.findViewById(R.id.book_cover_thumbnail);
        bookCoverImage.buildDrawingCache();
        Bitmap bitmapImage = bookCoverImage.getDrawingCache();

        OutputStream fOut = null;
        Uri outputFileUri;
        try {
            File fileDir = new File(dirPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();

            }
            File filepath = new File(fileDir, "book_cover.jpg");

            FileOutputStream fos = null;
            try {

                fos = new FileOutputStream(filepath);

                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onStoreBookSelect(View view, AudioBook audioBook, AudioBook.SelectedAction btnIndex) {
        switch (btnIndex){
            case ACTION_PURCHASE: {
                Log.v(TAG,"onCreateView");

                AudioBookDetailActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);


            }
                break;
            case ACTION_DETAIL:
                AudioBookDetailActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;

            case ACTION_PLAY:
                PlayerControllerActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook,-1);
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

    @Override
    public void onPostExecute(String result, String file_name) {
        if (result != null && result.equalsIgnoreCase("UNAUTHORISED")){
            showMessage("UNAUTHORISED");

        }else if(result != null && result.equalsIgnoreCase("NOTFOUND")){
            showMessage("NOTFOUND");

        }else {
            mProgressDialog.setMessage("Downloading " + (selectedBook.getDownloadedChapter().size() + 1) + " of " + selectedBook.getChapters().size());

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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.USER_UNAUTHORISED_TITLE).setMessage(getString(R.string.USER_UNAUTHORISED_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

        }else if(result.toUpperCase().contains("NOTFOUND")){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public interface OnStoreBookSelectedListener {
        public void onStoreBookSelected(int position);

    }


}