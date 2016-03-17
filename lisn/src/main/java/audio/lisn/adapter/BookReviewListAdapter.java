package audio.lisn.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.ArrayList;

import audio.lisn.R;
import audio.lisn.model.BookReview;

/**
 * Created by Rasika on 10/21/15.
 */
public class BookReviewListAdapter extends BaseAdapter {
    private Activity activity;
    private LayoutInflater inflater;
    ArrayList<BookReview> reviews;

    public BookReviewListAdapter(Activity activity, ArrayList<BookReview>reviews) {
        this.activity = activity;
        this.reviews = reviews;
    }

    @Override
    public int getCount() {
        return reviews.size();
    }

    @Override
    public Object getItem(int location) {
        return reviews.get(location);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (inflater == null)
            inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.view_book_review, null);
        TextView user= (TextView) convertView.findViewById(R.id.user_name);
        TextView time= (TextView) convertView.findViewById(R.id.time_label);
        TextView title= (TextView) convertView.findViewById(R.id.title);
        TextView  message= (TextView) convertView.findViewById(R.id.message);
        RatingBar ratingBar=(RatingBar)convertView.findViewById(R.id.rating_bar);


        BookReview bookReview = reviews.get(position);

        title.setTypeface(null, Typeface.BOLD);

        user.setText(bookReview.getUserName());
        time.setText(bookReview.getTimeString());
        title.setText(bookReview.getTitle());
        message.setText(bookReview.getMessage());
        if(Float.parseFloat(bookReview.getRateValue())>-1){
            ratingBar.setRating(Float.parseFloat(bookReview.getRateValue()));
        }
        // thumbnail image


        return convertView;
    }

}
