package audio.lisn.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import audio.lisn.R;

public class NavigationSectionListAdapter extends ArrayAdapter<String> {
	private final Activity context;
	private final String[] title;
	private final Integer[] imageId;

	public NavigationSectionListAdapter(Activity context, String[] web,
			Integer[] imageId) {
		super(context, R.layout.left_drawer_list_item_list, web);
		this.context = context;
		this.title = web;
		this.imageId = imageId;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.left_drawer_list_item_list,
				null, true);
		TextView txtTitle = (TextView) rowView.findViewById(R.id.section_title);
		ImageView imageView = (ImageView) rowView
				.findViewById(R.id.section_icon);
		txtTitle.setText(title[position]);
		imageView.setImageResource(imageId[position]);
		
		
		return rowView;
	}
}
