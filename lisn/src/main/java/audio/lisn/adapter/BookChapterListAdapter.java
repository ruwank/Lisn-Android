package audio.lisn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import audio.lisn.R;
import audio.lisn.model.AudioBook;
import audio.lisn.model.AudioBook.SelectedAction;
import audio.lisn.model.AudioBook.LanguageCode;
import audio.lisn.model.BookChapter;
import audio.lisn.util.AppUtils;
import audio.lisn.util.CustomTypeFace;

/**
 * Created by Rasika on 5/10/16.
 */
public class BookChapterListAdapter extends BaseAdapter {
    ArrayList<BookChapter> bookChapters;
    Context context;
    private LayoutInflater inflater;
    private BookChapterSelectListener listener;
    LanguageCode lanCode;
    AudioBook audioBook;

    public BookChapterListAdapter(Context context, AudioBook audioBook) {
        this.bookChapters = audioBook.getChapters();
        this.context = context;
        this.lanCode=audioBook.getLanguageCode();
        this.audioBook=audioBook;
    }
    public void bookChapterViewSelectListener(BookChapterSelectListener onItemClickListener){
        this.listener=onItemClickListener;
    }

    @Override
    public int getCount() {
        return bookChapters.size();
    }

    @Override
    public Object getItem(int location) {
        return bookChapters.get(location);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {
        if (inflater == null)
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.view_book_chapter_item, null);
        TextView chapterTitle= (TextView) convertView.findViewById(R.id.chapter_title);
        TextView chapterPrice= (TextView) convertView.findViewById(R.id.chapter_price);
        Button buyButton= (Button) convertView.findViewById(R.id.btn_chapter_action);

        if(lanCode== AudioBook.LanguageCode.LAN_SI){
            chapterTitle.setTypeface(CustomTypeFace.getSinhalaTypeFace(context));

        }else{
            chapterTitle.setTypeface(CustomTypeFace.getEnglishTypeFace(context));

        }
        final BookChapter bookChapter = bookChapters.get(position);

        String priceText="Free";
        String buyButtonText="Download";
        SelectedAction action=SelectedAction.ACTION_DOWNLOAD;
        String dirPath = AppUtils.getDataDirectory(context)
                + audioBook.getBook_id()+File.separator;
        File file = new File(dirPath + bookChapter.getChapter_id() + ".lisn");
        if (file.exists() && (audioBook.getDownloadedChapter().contains(bookChapter.getChapter_id()))) {
            buyButtonText="Play";
            action=SelectedAction.ACTION_PLAY;
        }else{
            if( bookChapter.getPrice()>0 ){

                priceText="Rs. "+bookChapter.getPrice();

                if(!bookChapter.isPurchased()){
                    buyButtonText="Buy";
                    action=SelectedAction.ACTION_PURCHASE;
                }

            }
        }
        buyButton.setText(buyButtonText);


        chapterTitle.setText(bookChapter.getTitle());
        chapterPrice.setText(priceText);
        final SelectedAction finalAction = action;
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(listener != null){
                    listener.onBookChapterSelect(bookChapter,position, finalAction);
                }
            }
        });

        return convertView;
    }

    public interface BookChapterSelectListener
    {
        public void onBookChapterSelect(BookChapter bookChapter,int index,AudioBook.SelectedAction action);
    }
}
