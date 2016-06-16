package audio.lisn.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.adapter.StoreBookViewAdapter;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.Log;
import audio.lisn.webservice.JsonUTF8ArrayRequest;

public class CategoryBookActivity extends AppCompatActivity implements
         StoreBookViewAdapter.StoreBookSelectListener {

    private ProgressDialog pDialog;
    private List<AudioBook> bookList = new ArrayList<AudioBook>();
    ConnectionDetector connectionDetector;
    TextView noDataTextView;
    private StoreBookViewAdapter storeBookViewAdapter;
    private RecyclerView searchBookView;
    private static final String TAG = CategoryBookActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        noDataTextView = (TextView)findViewById(R.id.no_data);
        initToolbar();
        getSupportActionBar().setTitle("Similar Book");
        connectionDetector = new ConnectionDetector(getApplicationContext());
        searchBookView=(RecyclerView)findViewById(R.id.searchBookContainer);
        searchBookView.setLayoutManager(new GridLayoutManager(this, 3));
        handleIntent(getIntent());
        noDataTextView.setVisibility(View.GONE);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    private void handleIntent(Intent intent) {

       // if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String category = intent.getStringExtra(Constants.BOOK_ID);
            downloadCategoryData(category);
       // }
    }
    @Override
    public void onResume() {
        super.onResume();

    }
    private void hidePDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }

    private void downloadCategoryData(final String bookCategory) {
        storeBookViewAdapter = new StoreBookViewAdapter(this,bookList);
        storeBookViewAdapter.setStoreBookSelectListener(this);
        searchBookView.setAdapter(storeBookViewAdapter);




        if (connectionDetector.isConnectingToInternet()) {

            pDialog = new ProgressDialog(this);
            // Showing progress dialog before making http request

            pDialog.setMessage(getString(R.string.loading_text));
            pDialog.show();
            Map<String, String> params = new HashMap<String, String>();
            params.put("bookid", "" + bookCategory);

            String url = getString(R.string.similar_book_url);
            JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(Request.Method.POST,url, params,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            Log.v(TAG,""+jsonArray.length());
                            setData(jsonArray);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v(TAG,""+error.getMessage());

                    hidePDialog();

                }
            });
            bookListReq.setShouldCache(false);
            AppController.getInstance().addToRequestQueue(bookListReq, "tag_similar_book");
        }
    }
    private AudioBook getDownloadedBook(String key){
        AudioBook returnBook=null;
        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(getApplicationContext());
        HashMap< String, AudioBook> hashMap=downloadedAudioBook.getBookList(getApplicationContext());

        returnBook=hashMap.get(key);
        return  returnBook;
    }

    private void setData(JSONArray jsonArray){
        hidePDialog();
Log.v(TAG,"jsonArray :"+jsonArray.length());
        bookList.clear();
        // Parsing json
        if(jsonArray.length() >0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {

                    JSONObject obj = jsonArray.getJSONObject(i);

                    AudioBook book = new AudioBook(obj, i,this);

                    bookList.add(book);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            // notifying list adapter about data changes
            // so that it renders the list view with updated data
            storeBookViewAdapter.notifyDataSetChanged();
        }else{
            noDataTextView.setVisibility(View.VISIBLE);

        }
    }



    @Override
    public void onStoreBookSelect(View view, AudioBook audioBook, AudioBook.SelectedAction btnIndex) {
        switch (btnIndex){
            case ACTION_PURCHASE:
                AudioBookDetailActivity.navigate(this, view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;
            case ACTION_DETAIL:
                AudioBookDetailActivity.navigate(this, view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;

            case ACTION_PLAY:
                PlayerControllerActivity.navigate(this, view.findViewById(R.id.book_cover_thumbnail), audioBook,-1);
                break;

            default:
                break;

        }
    }
}
