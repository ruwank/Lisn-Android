package audio.lisn.util;

/**
 * Created by Rasika on 1/30/16.
 */
public class Log {
    private static boolean TAG = false;
    public static void d(String enable_tag, String message,Object...args){
        if(TAG)
            android.util.Log.d(enable_tag, message+args);
    }
    public static void e(String enable_tag, String message,Object...args){
        if(TAG)
            android.util.Log.e(enable_tag, message + args);
    }
    public static void v(String enable_tag, String message,Object...args){
        if(TAG)
            android.util.Log.v(enable_tag, message + args);
    }
    public static void i(String enable_tag, String message,Object...args){
        if(TAG)
        android.util.Log.i(enable_tag, message+args);
    }



}



