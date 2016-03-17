package audio.lisn.util;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

public class CustomTypeFace {
	private static Hashtable<String, Typeface> fontCache = new Hashtable<String, Typeface>();

	public static Typeface getCustomTypeFace(Context context, String fontName) {

		Typeface tf = fontCache.get(fontName);
		if (tf == null) {
			try {
				tf = Typeface.createFromAsset(context.getAssets(), "Fonts/"
						+ fontName);
			} catch (Exception e) {
				return null;
			}
			fontCache.put(fontName, tf);
		}
		return tf;

	}

	public static Typeface getSinhalaTypeFace(Context context) {
		return getCustomTypeFace(context, "FMAbhaya.ttf");
	}

	public static Typeface getEnglishTypeFace(Context context) {
		return getCustomTypeFace(context, "OpenSans-Regular.ttf");
	}
}
