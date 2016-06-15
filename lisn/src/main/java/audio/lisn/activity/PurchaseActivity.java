package audio.lisn.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.BookChapter;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.Log;

public class PurchaseActivity extends AppCompatActivity {
    private static final String TRANSITION_NAME = "audio.lisn.PurchaseActivity";
    AudioBook audioBook;
    ConnectionDetector connectionDetector;
    WebView webView;
    ProgressBar progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);
        ViewCompat.setTransitionName(findViewById(R.id.app_bar_layout), TRANSITION_NAME);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        supportStartPostponedEnterTransition();
        getSupportActionBar().setTitle(R.string.app_name);

        progressBar=(ProgressBar)findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(0xFFee9f1f, android.graphics.PorterDuff.Mode.MULTIPLY);

        audioBook = (AudioBook) getIntent().getSerializableExtra("audioBook");
        String url=getString(R.string.purchase_book_url);
        url=url+"?userid="+ AppController.getInstance().getUserId()+"&bookid="+audioBook.getBook_id();

        boolean isSelectChapterBuyOption=(boolean)getIntent().getBooleanExtra("isSelectChapterBuyOption",false);
        if(isSelectChapterBuyOption){
            BookChapter bookChapter = (BookChapter) getIntent().getSerializableExtra("selectedChapter");
            float amount= (float) ((bookChapter.getPrice()) * ((100.0-bookChapter.getDiscount())/100.0));
            url=url+"&amount="+amount+"&chapid="+bookChapter.getChapter_id();
        }else{
            float amount= (float) ((Float.parseFloat(audioBook.getPrice())) * ((100.0-audioBook.getDiscount())/100.0));
            url=url+"&amount="+amount;

        }
        // http://app.lisn.audio/spgw/1.5.6/payment/init.php?userid=1&bookid=1&amount=150.00
        //String url=getString(R.string.purchase_book_url);
        //url=url+"?userid="+ AppController.getInstance().getUserId()+"&bookid="+audioBook.getBook_id()+"&amount="+amount;
        webView = (WebView) findViewById(R.id.webview);
        webView.loadUrl(url);
        webView.setWebViewClient(new LisnWebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        connectionDetector = new ConnectionDetector(getApplicationContext());


    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void completedPayment(int status){
        Intent returnIntent = new Intent();
        if(status==0){
            setResult(Constants.RESULT_SUCCESS, returnIntent);

        }else if(status==2){
            setResult(Constants.RESULT_SUCCESS_ALREADY, returnIntent);

        }else {
            setResult(Constants.RESULT_ERROR, returnIntent);

        }
        finish();
    }

    private class LisnWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.v("should", "url:" + url);

            if(url.equalsIgnoreCase(getString(R.string.purchase_success_url)) || url.equalsIgnoreCase(getString(R.string.purchase_success_url_http))){
                completedPayment(0);
                return true;
            }
            else if(url.equalsIgnoreCase(getString(R.string.purchase_failed_url)) || url.equalsIgnoreCase(getString(R.string.purchase_failed_url_http))){
                completedPayment(1);

                return true;
            }else if(url.equalsIgnoreCase(getString(R.string.purchase_already_url)) || url.equalsIgnoreCase(getString(R.string.purchase_already_url_http))){
                completedPayment(2);

                return true;
            }
            return false;


        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }
    }

}
