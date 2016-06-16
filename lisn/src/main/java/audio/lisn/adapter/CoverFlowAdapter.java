package audio.lisn.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import at.technikum.mti.fancycoverflow.FancyCoverFlowAdapter;
import audio.lisn.R;
import audio.lisn.model.BookChapter;
import audio.lisn.util.AppUtils;
import audio.lisn.util.Log;

public class CoverFlowAdapter extends FancyCoverFlowAdapter {

    //private ArrayList<AudioBook> mData = new ArrayList<>(0);
    private ArrayList<BookChapter> mData = new ArrayList<>(0);
	private Context mContext;
    int coverFlowWidth=0;
    int coverFlowHeight=0;
    String bookId;

	public CoverFlowAdapter(Context context,String bookId) {
		mContext = context;
        this.bookId=bookId;
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        coverFlowWidth= (int)(metrics.density * 160);
        coverFlowHeight= (int)(metrics.density * 240);
        Log.v("coverFlowWidth", "coverFlowWidth" + coverFlowWidth);

    }
	
	public void setData(List<BookChapter> data) {
		mData = (ArrayList<BookChapter>) data;
	}
	
	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int pos) {
		return mData.get(pos);
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}

    @Override
    public View getCoverFlowItem(int i, View reuseableView, ViewGroup viewGroup) {
        CustomViewGroup customViewGroup = null;
        BookChapter bookChapter = mData.get(i);
        if (reuseableView != null) {
            customViewGroup = (CustomViewGroup) reuseableView;
        } else {
            customViewGroup = new CustomViewGroup(viewGroup.getContext());
            customViewGroup.setLayoutParams(new FancyCoverFlow.LayoutParams(coverFlowWidth, coverFlowHeight));
        }
        String chapterName="Chapter "+(bookChapter.getChapter_id());
        customViewGroup.getTextView().setText(chapterName);

        String dirPath = AppUtils.getDataDirectory(mContext)
                + bookId+ File.separator;
        File file = new File(dirPath +(bookChapter.getChapter_id())+".lisn");


        String buyButtonText="Get";

        if (file.exists()) {
            customViewGroup.getButton().setVisibility(View.GONE);

        }else{
            customViewGroup.getButton().setVisibility(View.VISIBLE);

            if( bookChapter.getPrice()>0 ){

                if(!bookChapter.isPurchased()){
                    buyButtonText="Buy";
                }

            }
            customViewGroup.getButton().setText(buyButtonText);
        }


       // customViewGroup.getImageView().setImageBitmap(null);

//        String img_path = AppUtils.getDataDirectory(customViewGroup.getImageView().getContext())
//                + book.getBook_id()+ File.separator+"book_cover.jpg";
//
//
//        File imgFile = new  File(img_path);
//
//        if(imgFile.exists()){
//            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            Bitmap resized = Bitmap.createScaledBitmap(bitmap, coverFlowWidth, coverFlowHeight, true);
//
//            customViewGroup.getImageView().setImageBitmap(resized);
//
//
//        }

//        String chapterName="Chapter "+(i+1);
//        Bitmap bitmap = drawTextToBitmap(chapterName);
//        Bitmap resized = Bitmap.createScaledBitmap(bitmap, coverFlowWidth, coverFlowHeight, true);
//
//        customViewGroup.getImageView().setImageBitmap(resized);


        return customViewGroup;
    }
    public  int getDensityDpi(float value) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        int density = metrics.densityDpi;
        return (int)Math.ceil(density * value);
    }

    public Bitmap drawTextToBitmap(String mText) {
        try {
            Resources resources = mContext.getResources();
            float scale = resources.getDisplayMetrics().density;
            Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.ui_chapter_cover_page);

            android.graphics.Bitmap.Config bitmapConfig =   bitmap.getConfig();
            // set default bitmap config if none
            if(bitmapConfig == null) {
                bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
            }
            // resource bitmaps are imutable,
            // so we need to convert it to mutable one
            bitmap = bitmap.copy(bitmapConfig, true);

            Canvas canvas = new Canvas(bitmap);
            // new antialised Paint
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            // text color - #3D3D3D
            paint.setColor(Color.rgb(110, 110, 110));
            // text size in pixels
            paint.setTextSize((int) (30 * scale));
            // text shadow
           // paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY);

            // draw text to the Canvas center
            Rect bounds = new Rect();
            paint.getTextBounds(mText, 0, mText.length(), bounds);
            int x = ((bitmap.getWidth()-bounds.width())/2);
            int y = ((bitmap.getHeight())/2)-bounds.height();

            canvas.drawText(mText, x * scale, y * scale, paint);


            return bitmap;
        } catch (Exception e) {
            // TODO: handle exception



            return null;
        }

    }

 private static class CustomViewGroup extends RelativeLayout {

    // =============================================================================
    // Child views
    // =============================================================================

    private ImageView imageView;

      private Button button;
     private TextView textView;

    // =============================================================================
    // Constructor
    // =============================================================================

    private CustomViewGroup(Context context) {
        super(context);

       // this.setOrientation(VERTICAL);
        //   this.setWeightSum(5);

        this.imageView = new ImageView(context);
        this.button = new Button(context);
        this.textView = new TextView(context);

        LayoutParams imageViewLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.imageView.setLayoutParams(imageViewLayoutParams);
        this.imageView.setBackgroundColor(getContext().getResources().getColor(R.color.whiteColor));
        this.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        this.imageView.setAdjustViewBounds(true);

        RelativeLayout.LayoutParams buttonLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        buttonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonLayoutParams.height=30;
        buttonLayoutParams.setMargins(5, 5, 5, 20);

        this.button.setLayoutParams(buttonLayoutParams);
        this.button.setBackgroundColor(getContext().getResources().getColor(R.color.colorPrimary));
        this.button.setTextColor(getContext().getResources().getColor(R.color.whiteColor));
        this.button.setText("Buy");
        button.setGravity(Gravity.CENTER);
        this.button.setTextSize(10);
        button.setPadding(0, 0, 0, 0);

        RelativeLayout.LayoutParams textViewLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textViewLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        this.textView.setLayoutParams(textViewLayoutParams);

        this.addView(this.imageView);
        this.addView(this.button);
        this.addView(this.textView);

    }

    // =============================================================================
    // Getters
    // =============================================================================

    private ImageView getImageView() {
        return imageView;
    }

     private TextView getTextView() {
         return textView;
     }
     private Button getButton() {
         return button;
     }

}



    static class ViewHolder {
        public TextView text;
        public ImageView image;
    }
}
