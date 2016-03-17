package audio.lisn.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AppDataManager {
	public static void saveData(Context context, String key, String value) {
		SharedPreferences settings = context.getSharedPreferences(
				Constants.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(key, value);
		// Commit the edits!
		editor.commit();
	}

	public static String getData(Context context, String key) {
		SharedPreferences settings = context.getSharedPreferences(
				Constants.PREFS_NAME, 0);
		return settings.getString(key, "");

	}

}
