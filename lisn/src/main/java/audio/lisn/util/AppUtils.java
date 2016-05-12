package audio.lisn.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AppUtils {
	private static String algorithm = "AES";

	public static String getDataDirectory(Context context) {
		File sdRoot = Environment.getExternalStorageDirectory();
		String path = sdRoot.getAbsolutePath();
		String packageName = context.getPackageName();
		path = path + File.separator + "Android" + File.separator + "data"
				+ File.separator + packageName + File.separator + "files"
				+ File.separator;
		return path;
	}

	public static SecretKey generateKey(char[] passphraseOrPin, byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Number of PBKDF2 hardening rounds to use. Larger values increase
		// computation time. You should select a value that causes computation
		// to take >100ms.
		final int iterations = 1000;

		// Generate a 256-bit key
		final int outputKeyLength = 256;

		SecretKeyFactory secretKeyFactory = SecretKeyFactory
				.getInstance("ntmsbfyrywe38293");
		KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations,
				outputKeyLength);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		return secretKey;
	}

	public static SecretKey generateKey() throws NoSuchAlgorithmException {
		// Generate a 256-bit key
		final int outputKeyLength = 256;
		SecureRandom secureRandom = new SecureRandom();
		// Do *not* seed secureRandom! Automatically seeded from system entropy.
		KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
		keyGenerator.init(outputKeyLength, secureRandom);
		SecretKey key = keyGenerator.generateKey();
		return key;
	}

	public static byte[] encodeFile(byte[] fileData) throws Exception {
		byte[] encrypted = null;

		SecretKeySpec sks = new SecretKeySpec("ntmsbfyrywe38293".getBytes(),
				"AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, sks);
		encrypted = cipher.doFinal(fileData);
		return encrypted;
	}
//LFqc0z7:yuBv
	public static byte[] decodeFile(byte[] fileData) throws Exception {
		byte[] decrypted = null;

		SecretKeySpec sks = new SecretKeySpec("K66wl3d43I$P0937".getBytes(),
				"AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, sks);


        decrypted = cipher.doFinal(fileData);

		return decrypted;
	}


	public static byte[] getBytes(InputStream inputStream) throws IOException {
		byte[] bytes = null;

		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			byte data[] = new byte[4096];
			int count;

			while ((count = inputStream.read(data)) != -1) {
				bos.write(data, 0, count);
			}

			bos.flush();
			bos.close();
			inputStream.close();

			bytes = bos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytes;
	}

    public static String milliSecondsToTimer(long milliseconds){
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int)( milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);
        // Add hours if there
        if(hours > 0){
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if(seconds < 10){
            secondsString = "0" + seconds;
        }else{
            secondsString = "" + seconds;}

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }
	public static long getAvailableMemory(){
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
		long megAvailable = bytesAvailable / (1024 * 1024);
		return megAvailable;
	}
public static String getCredentialsData() {
	String USERNAME="app";
	String PASSWORD="Kn@sw7*d#b";
	String credentials = USERNAME+":"+PASSWORD;
	return credentials;


}

	public static Bitmap blurRenderScript(Bitmap smallBitmap, int radius,Context context) {

		try {
			smallBitmap = RGB565toARGB888(smallBitmap);
		} catch (Exception e) {
			e.printStackTrace();
		}


		Bitmap bitmap = Bitmap.createBitmap(
				smallBitmap.getWidth(), smallBitmap.getHeight(),
				Bitmap.Config.ARGB_8888);

		RenderScript renderScript = RenderScript.create(context);

		Allocation blurInput = Allocation.createFromBitmap(renderScript, smallBitmap);
		Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

		ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,
				Element.U8_4(renderScript));
		blur.setInput(blurInput);
		blur.setRadius(radius); // radius must be 0 < r <= 25
		blur.forEach(blurOutput);

		blurOutput.copyTo(bitmap);
		renderScript.destroy();

		return bitmap;

	}

	public static Bitmap RGB565toARGB888(Bitmap img) throws Exception {
		int numPixels = img.getWidth() * img.getHeight();
		int[] pixels = new int[numPixels];

		//Get JPEG pixels.  Each int is the color values for one pixel.
		img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

		//Create a Bitmap of the appropriate format.
		Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);

		//Set RGB pixels.
		result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
		return result;
	}
}
