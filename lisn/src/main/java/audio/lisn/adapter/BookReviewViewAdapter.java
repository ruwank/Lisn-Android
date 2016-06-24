package audio.lisn.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import audio.lisn.R;
import audio.lisn.model.BookReview;
import audio.lisn.util.Log;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Rasika on 10/19/15.
 */
public class BookReviewViewAdapter extends RecyclerView.Adapter<BookReviewViewAdapter.ViewHolder>{
    private Context context;
    ArrayList<BookReview> reviews;


    public BookReviewViewAdapter(Context context, ArrayList<BookReview> reviews) {
        this.reviews = reviews;
        this.context=context;

    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_book_review, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BookReview bookReview = reviews.get(position);

        holder.title.setTypeface(null, Typeface.BOLD);

        holder.user.setText(bookReview.getUserName());
        holder.time.setText(bookReview.getTimeString());
        holder.title.setText(bookReview.getTitle());
        holder.message.setText(bookReview.getMessage());
        if(Float.parseFloat(bookReview.getRateValue())>-1){
            holder.ratingBar.setRating(Float.parseFloat(bookReview.getRateValue()));
        }
        Log.v("profileImageUrl", "profileImageUrl :" + bookReview.getFbId());
        holder.profileImage.setImageResource(R.drawable.ic_profile_default);
        if(bookReview.getFbId() != null && bookReview.getFbId().length()>0){

            String profileImageUrl=holder.profileImage.getContext().getString(R.string.fb_profile_picture_url);
            profileImageUrl=profileImageUrl+bookReview.getFbId()+"/picture";
            Log.v("profileImageUrl", "profileImageUrl :" + profileImageUrl);

            Picasso.with(holder.profileImage.getContext())
                    .load(profileImageUrl)
                    .placeholder(R.drawable.ic_profile_default)
                    .into(holder.profileImage);
        }

    }

    @Override
    public int getItemCount() {
        Log.v("reviews.size()","reviews.size(): "+reviews.size());
        return reviews.size();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView user,time, title,message;
        public RatingBar ratingBar;
        CircleImageView profileImage;

        public ViewHolder(View itemView) {
            super(itemView);

            user= (TextView) itemView.findViewById(R.id.user_name);
            time= (TextView) itemView.findViewById(R.id.time_label);
            title= (TextView) itemView.findViewById(R.id.title);
            message= (TextView) itemView.findViewById(R.id.message);
            ratingBar=(RatingBar)itemView.findViewById(R.id.rating_bar);
            profileImage=(CircleImageView)itemView.findViewById(R.id.profile_image);

        }
    }
}
