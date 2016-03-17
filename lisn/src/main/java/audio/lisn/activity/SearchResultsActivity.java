package audio.lisn.activity;

import android.app.ProgressDialog;
import android.app.SearchManager;
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
import audio.lisn.util.Log;
import audio.lisn.webservice.JsonUTF8ArrayRequest;

public class SearchResultsActivity extends AppCompatActivity implements
         StoreBookViewAdapter.StoreBookSelectListener {

    private ProgressDialog pDialog;
    private List<AudioBook> bookList = new ArrayList<AudioBook>();
    ConnectionDetector connectionDetector;
    TextView noDataTextView;
    private StoreBookViewAdapter storeBookViewAdapter;
    private RecyclerView searchBookView;
    private static final String TAG = SearchResultsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        noDataTextView = (TextView)findViewById(R.id.no_data);
        initToolbar();
        getSupportActionBar().setTitle("Search Results");
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

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.v("query", "query:" + query);
            downloadSearchResultData(query);
        }
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

    private void downloadSearchResultData(final String searchQuery) {
        storeBookViewAdapter = new StoreBookViewAdapter(this,bookList);
        storeBookViewAdapter.setStoreBookSelectListener(this);
        searchBookView.setAdapter(storeBookViewAdapter);


        Map<String, String> params = new HashMap<String, String>();
        params.put("searchstr", searchQuery);

        Log.v("params", params.toString());

        if (connectionDetector.isConnectingToInternet()) {

            pDialog = new ProgressDialog(this);
            // Showing progress dialog before making http request

            pDialog.setMessage(getString(R.string.loading_text));
            pDialog.show();
            String url = getString(R.string.search_book_url);
            JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(Request.Method.POST,url, params,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            setData(jsonArray);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hidePDialog();

                }
            });
            bookListReq.setShouldCache(false);
            AppController.getInstance().addToRequestQueue(bookListReq, "tag_search_book");
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
Log.v("jsonArray","jsonArray :"+jsonArray);
        bookList.clear();
        // Parsing json
        if(jsonArray.length() >0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {

                    String book_id = "";
                    JSONObject obj = jsonArray.getJSONObject(i);

                    try {
                        book_id = obj.getString("book_id");
                    } catch (JSONException e) {
                        book_id = obj.getString("" + i);
                        e.printStackTrace();
                    }

                    AudioBook book = getDownloadedBook(book_id);

                    if (book == null) {
                        book = new AudioBook();
                    }
                    book.setBook_id(book_id);
                    book.setISBN(book_id);

                    book.setAuthor(obj.getString("author"));
                    book.setCategory(obj.getString("category"));
                    book.setCover_image(obj.getString("cover_image"));
                    book.setDescription(obj.getString("description"));
                    book.setLanguage(obj.getString("language"));
                    book.setPreview_audio(obj.getString("preview_audio"));
                    book.setPrice(obj.getString("price"));
                    book.setTitle(obj.getString("title"));
                    book.setEnglish_title(obj.getString("english_title"));
                    book.setRate(obj.getString("rate"));
                    book.setDuration(obj.getString("duration"));
                    book.setNarrator(obj.getString("narrator"));


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
                PlayerControllerActivity.navigate(this, view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;

            default:
                break;

        }
    }
}
