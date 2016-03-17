package audio.lisn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

import audio.lisn.R;
import audio.lisn.adapter.BookReviewViewAdapter;
import audio.lisn.model.BookReview;

public class BookReviewActivity extends AppCompatActivity {
    private static final String TRANSITION_NAME = "audio.lisn.BookReviewActivity";
    ArrayList<BookReview> reviews;
    BookReviewViewAdapter bookReviewViewAdapter;
    RecyclerView reviewContainer;

    public static void navigate(AppCompatActivity activity, View transitionView, ArrayList<BookReview> reviews) {
        Intent intent = new Intent(activity, BookReviewActivity.class);
        if(reviews !=null)
            intent.putExtra("reviews", reviews);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionView, TRANSITION_NAME);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_review);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);
        if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("reviews")) {
            reviews = (ArrayList<BookReview>) getIntent().getSerializableExtra("reviews");
        }
        reviewContainer=(RecyclerView)findViewById(R.id.reviewContainer);
        LinearLayoutManager linearLayoutManagerVertical =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        reviewContainer.setLayoutManager(linearLayoutManagerVertical);

        bookReviewViewAdapter=new BookReviewViewAdapter(getApplicationContext(),reviews);
        reviewContainer.setAdapter(bookReviewViewAdapter);



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

}
