package audio.lisn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import audio.lisn.R;

public class SettingsActivity extends AudioBookBaseActivity {
	private RadioGroup radioLanguageGroup;
	private RadioButton radioSinhala, radioEnglish;
	private ImageButton btnUpdate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		radioLanguageGroup = (RadioGroup) findViewById(R.id.radioLanguage);
		btnUpdate = (ImageButton) findViewById(R.id.btn_update);
		radioSinhala = (RadioButton) findViewById(R.id.radioSinhala);
		radioEnglish = (RadioButton) findViewById(R.id.radioEnglish);

	}

    @Override
    protected int getLayoutResource() {
        return 0;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; goto parent activity.
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	

	private void updateSettingData() {


		Intent intent = getBaseContext().getPackageManager()
				.getLaunchIntentForPackage(getBaseContext().getPackageName());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

	}

}