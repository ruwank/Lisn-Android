package audio.lisn.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import audio.lisn.R;

/**
 * Created by Rasika on 5/10/16.
 */
public class BookChapterListAdapter extends ArrayAdapter<String>{
    ArrayList<String> data;
    Context context;

    public BookChapterListAdapter(Context context, int resource) {
        super(context, resource);
    }
    public BookChapterListAdapter(Context context, ArrayList<String> dataItem) {
        super(context, R.layout.view_book_chapter_item, dataItem);
        this.data = dataItem;
        this.context = context;
    }
}
