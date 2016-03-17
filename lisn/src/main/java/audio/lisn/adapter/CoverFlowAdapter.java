package audio.lisn.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import at.technikum.mti.fancycoverflow.FancyCoverFlowAdapter;
import audio.lisn.model.AudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.Log;

public class CoverFlowAdapter extends FancyCoverFlowAdapter {
	
	private ArrayList<AudioBook> mData = new ArrayList<>(0);
	private Context mContext;
    int coverFlowWidth=0;
    int coverFlowHeight=0;

	public CoverFlowAdapter(Context context) {
		mContext = context;
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        coverFlowWidth= (int)(metrics.density * 160);
        coverFlowHeight= (int)(metrics.density * 240);
        Log.v("coverFlowWidth", "coverFlowWidth" + coverFlowWidth);

    }
	
	public void setData(List<AudioBook> data) {
		mData = (ArrayList<AudioBook>) data;
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
        AudioBook book = mData.get(i);
        if (reuseableView != null) {
            customViewGroup = (CustomViewGroup) reuseableView;
        } else {
            customViewGroup = new CustomViewGroup(viewGroup.getContext());
            customViewGroup.setLayoutParams(new FancyCoverFlow.LayoutParams(coverFlowWidth, coverFlowHeight));
        }
        customViewGroup.getImageView().setImageBitmap(null);

        String img_path = AppUtils.getDataDirectory(customViewGroup.getImageView().getContext())
                + book.getBook_id()+ File.separator+"book_cover.jpg";


        File imgFile = new  File(img_path);

        if(imgFile.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, coverFlowWidth, coverFlowHeight, true);

            customViewGroup.getImageView().setImageBitmap(resized);


        }


        return customViewGroup;
    }
    public  int getDensityDpi(float value) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        int density = metrics.densityDpi;
        return (int)Math.ceil(density * value);
    }


 private static class CustomViewGroup extends LinearLayout {

    // =============================================================================
    // Child views
    // =============================================================================

    private ImageView imageView;

    //  private Button button;

    // =============================================================================
    // Constructor
    // =============================================================================

    private CustomViewGroup(Context context) {
        super(context);

        this.setOrientation(VERTICAL);
        //   this.setWeightSum(5);

        this.imageView = new ImageView(context);
        // this.button = new Button(context);

        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        this.imageView.setLayoutParams(layoutParams);
        //  this.button.setLayoutParams(layoutParams);

        this.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        this.imageView.setAdjustViewBounds(true);



        this.addView(this.imageView);
    }

    // =============================================================================
    // Getters
    // =============================================================================

    private ImageView getImageView() {
        return imageView;
    }
}



    static class ViewHolder {
        public TextView text;
        public ImageView image;
    }
}
