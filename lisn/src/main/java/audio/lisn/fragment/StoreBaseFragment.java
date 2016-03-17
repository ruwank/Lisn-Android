package audio.lisn.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.model.BookCategory;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.Log;
import audio.lisn.view.SlidingTabLayout;
import audio.lisn.webservice.JsonUTF8ArrayRequest;

import static audio.lisn.R.id.sliding_tabs;
import static audio.lisn.R.id.viewpager;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link StoreBaseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoreBaseFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private static final String TAG = StoreBaseFragment.class.getSimpleName();
    private StoreFragment currentFragment;
    //private Map mPageReferenceMap;
    Map<Integer, StoreFragment> mPageReferenceMap = new HashMap<>();
    ConnectionDetector connectionDetector;
    private ProgressDialog pDialog;
    BookCategory[] bookCategories;
    private SectionsPagerAdapter sectionsPagerAdapter;
    SwipeRefreshLayout mSwipeRefreshLayout;
    int downloadCount=0,completeCount=0;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StoreBaseFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StoreBaseFragment newInstance() {
        StoreBaseFragment fragment = new StoreBaseFragment();
        return fragment;
    }

    public StoreBaseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_store_base, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {


        // BEGIN_INCLUDE (setup_viewpager)
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (ViewPager) view.findViewById(viewpager);
       // mViewPager.setAdapter(new SamplePagerAdapter());
        sectionsPagerAdapter=new SectionsPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(sectionsPagerAdapter);

        // END_INCLUDE (setup_viewpager)

        // BEGIN_INCLUDE (setup_slidingtablayout)
        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(sliding_tabs);
        mSlidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {

            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.whiteColor);    //define any color in xml resources and set it here, I have used white
            }

            @Override
            public int getDividerColor(int position) {
                return getResources().getColor(R.color.whiteColor);
            }
        });
        bookCategories= AppController.getInstance().getBookCategories();
        if(bookCategories == null){
            if (connectionDetector.isConnectingToInternet()) {
                downloadCategoryList();
            }
        }else{
            mSlidingTabLayout.setViewPager(mViewPager);
            sectionsPagerAdapter.notifyDataSetChanged();

        }
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.v("onPageSelected", "onPageSelected");
                removePlayer();

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }
        });

        // END_INCLUDE (setup_slidingtablayout)
    }

//    private void refreshContent() {
//        mSwipeRefreshLayout.setRefreshing(false);
//    }

    private void updateCategoryList(JSONArray jsonArray){
    bookCategories= new BookCategory[jsonArray.length()];

    for (int i = 0; i < jsonArray.length(); i++) {
        try {

            JSONObject obj = jsonArray.getJSONObject(i);
            BookCategory bookCategory=new BookCategory(obj);
            bookCategories[i]=bookCategory;

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    AppController.getInstance().setBookCategories(bookCategories);
    mSlidingTabLayout.setViewPager(mViewPager);
    sectionsPagerAdapter.notifyDataSetChanged();


}
    private void  removePlayer(){
        for (int i = 0; i <mPageReferenceMap.size() ; i++) {
            StoreFragment storeFragment = mPageReferenceMap.get(i);
            if(storeFragment !=null){
                storeFragment.removePlayer();

            }
        }
    }

    private void downloadCategoryList() {
        pDialog.show();
        String url=getString(R.string.book_category_list_url);

        JsonUTF8ArrayRequest categoryListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        updateCategoryList(jsonArray);
                        pDialog.dismiss();;

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                pDialog.dismiss();;

            }
        });
        categoryListReq.setShouldCache(true);
        AppController.getInstance().addToRequestQueue(categoryListReq, "tag_category_list");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.store_book_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            connectionDetector = new ConnectionDetector(context);

            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getString(R.string.loading_text));

        } catch (Exception e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMenu();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */



    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }
       // private final String[] CATEGORIES = { "Novels","Educational","Short Stories","Other"};

        @Override
        public Fragment getItem(int position) {

            if(currentFragment !=null){
                currentFragment.removePlayer();
                currentFragment=null;

            }

            BookCategory bookCategory=bookCategories[position];

            currentFragment=new  StoreFragment(bookCategory.getId());
            mPageReferenceMap.put(position, currentFragment);

            return currentFragment;

        }

        @Override
        public int getCount() {
            // Show  total pages.
            if(bookCategories == null ){
                return 0;

            }else{
                return bookCategories.length;

            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if(bookCategories == null){
                return "";

            }else{
                BookCategory bookCategory=bookCategories[position];

                return bookCategory.getEnglish_name();

            }

        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            StoreFragment storeFragment = (StoreFragment) object;
            if(storeFragment != null){
                storeFragment.removePlayer();
            }
            mPageReferenceMap.remove(position);


        }
        public StoreFragment getFragment(int key) {


            return (StoreFragment) mPageReferenceMap.get(key);
        }



    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("onActivityResult","onActivityResult StoreBaseFragment resultCode"+requestCode);

        int index = mViewPager.getCurrentItem();
        SectionsPagerAdapter adapter = ((SectionsPagerAdapter)mViewPager.getAdapter());
        StoreFragment fragment = adapter.getFragment(index);

        if(fragment !=null)
            fragment.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
    }
    private void updateMenu() {
        Intent intent = new Intent(Constants.MENU_ITEM_SELECT);
        intent.putExtra("index", 1);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
    }

    //Refresh data
    private void refreshContent(){
       // getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        downloadCount=0;
        completeCount=0;
        downloadNewReleaseBookList();
        downloadTopDownloadedBookList();
        downloadTopRatedBookList();
        downloadBookCategoryList();
    }
    private void refreshScreen(){
        if(downloadCount ==completeCount){
           // getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_o);

            mSlidingTabLayout.setViewPager(null);
            mSlidingTabLayout.setViewPager(mViewPager);
            sectionsPagerAdapter.notifyDataSetChanged();
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private void downloadNewReleaseBookList(){
        String url=getString(R.string.home_book_list_url);
        downloadCount++;
        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setNewReleaseBookList(jsonArray);
                        completeCount++;
                        refreshScreen();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                completeCount++;
                refreshScreen();

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_new_realease_list");
    }

    private void downloadTopRatedBookList(){
        downloadCount++;
        String url=getString(R.string.top_rated_book_list_url);

        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setTopRatedBookList(jsonArray);
                        completeCount++;
                        refreshScreen();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                completeCount++;
                refreshScreen();

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_topRated_list");
    }

    private void downloadTopDownloadedBookList(){
        downloadCount++;
        String url=getString(R.string.top_download_book_list_url);

        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        AppController.getInstance().setTopDownloadedBookList(jsonArray);
                        completeCount++;
                        refreshScreen();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                completeCount++;
                refreshScreen();

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_top_download_list");
    }


    private void downloadBookCategoryList() {
        downloadCount++;
        String url=getString(R.string.book_category_list_url);

        JsonUTF8ArrayRequest categoryListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        downloadCategoryContent(jsonArray);
                        completeCount++;
                        refreshScreen();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                completeCount++;
                refreshScreen();

            }
        });
        categoryListReq.setShouldCache(true);
        AppController.getInstance().addToRequestQueue(categoryListReq, "tag_category_list");
    }
    private void downloadCategoryContent(JSONArray jsonArray){
        BookCategory[] bookCategories= new BookCategory[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            try {

                JSONObject obj = jsonArray.getJSONObject(i);
                BookCategory bookCategory=new BookCategory(obj);
                bookCategories[i]=bookCategory;
                downloadCategoryData(bookCategory.getId());

            } catch (JSONException e) {
                e.printStackTrace();

            }

        }
        AppController.getInstance().setBookCategories(bookCategories);


    }
    private void downloadCategoryData(final int bookCategory) {
        downloadCount++;
        Log.v("bookCategory","bookCategory"+bookCategory);
        Map<String, String> params = new HashMap<String, String>();
        params.put("cat", "" + bookCategory);

        String url = getString(R.string.book_category_url);


        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(Request.Method.POST,url, params,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setStoreBookForCategory(bookCategory, jsonArray);
                        completeCount++;
                        refreshScreen();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                completeCount++;
                refreshScreen();

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_boo_list"+bookCategory);
        //   showOption();
    }
}
