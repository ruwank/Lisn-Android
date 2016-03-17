package audio.lisn.webservice;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Base64;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.util.AppUtils;
import audio.lisn.util.Log;

//import java.nio.charset.StandardCharsets;

/**
 * Created by Rasika on 10/5/15.
 */
public class FileDownloadTask extends AsyncTask<String, Integer, String> {

    private Context context;
    private String file_name;
    private String book_id;
    FileDownloadTaskListener taskListener;



    public FileDownloadTask(Context context,FileDownloadTaskListener taskListener,String book_id) {
        this.context = context;
        this.taskListener=taskListener;
        this.book_id=book_id;
    }

    @Override
    protected String doInBackground(String... sUrl) {
        String directory = sUrl[0];
        String fileName = sUrl[1];
        file_name=fileName;
        String urlString = context.getResources().getString(R.string.book_download_url);
        String urlParameters  = "userid="+ AppController.getInstance().getUserId()+"&bookid="+book_id+"&part="+fileName;
        Log.v("urlParameters", "" + urlParameters);
        //byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
        byte[] postData       = new byte[0];
        try {
            postData = urlParameters.getBytes("UTF-8" );
        } catch (Exception e) {
            e.printStackTrace();
        }
        int    postDataLength = postData.length;

        PowerManager pm = (PowerManager) context
                .getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wl.acquire();

        try {
            File rootPath = new File(directory);
            if (!rootPath.exists()) {
                rootPath.mkdirs();
            }
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {


                // connect to url
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("charset", "utf-8");
                String credentials = AppUtils.getCredentialsData();

                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(),
                        Base64.NO_WRAP);
                connection.setRequestProperty("Authorization", auth);
                connection.setDoInput(true);
               connection.setDoOutput(true);
                connection.setConnectTimeout(60000);

               connection.setReadTimeout(60000);
                DataOutputStream printout = new DataOutputStream(connection.getOutputStream ());
                // printout.write(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                printout.writeBytes(urlParameters.toString());

                printout.flush ();
                printout.close ();


                connection.connect();

                // check for http_ok (200)
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                    return "Server returned HTTP "
                            + connection.getResponseCode() + " "
                            + connection.getResponseMessage();
                Log.v("connection","connection"+connection.getResponseCode());
                // Log.v("connection","connection"+connection.get);

                int fileLength = connection.getContentLength();
                Log.v("fileLength",""+fileLength);

                if(fileLength<20){
                    InputStream is = null;
                    try {
                        is = connection.getInputStream();
                        int ch;
                        StringBuffer sb = new StringBuffer();
                        while ((ch = is.read()) != -1) {
                            sb.append((char) ch);
                        }
                        return sb.toString();
                    } catch (IOException e) {
                        throw e;
                    }
                }
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(directory + "/" + fileName
                        + ".lisn");// change extension

                // copying
                byte data[] = new byte[4096];
                int count;
                long total = 0;

                while ((count = input.read(data)) != -1) {

                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    output.write(data, 0, count);
                }

            } catch (Exception e) {
                return e.toString();
            } finally // closing streams and connection
            {
                try {
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {

                }

                if (connection != null)
                    connection.disconnect();
            }
            if(isCancelled() && (connection != null)){
                Log.v("isCancelled","disconnect");
                connection.disconnect();
            }
        } finally {
            wl.release(); // release the lock screen
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        // PowerManager pm = (PowerManager)
        // context.getSystemService(Context.POWER_SERVICE);
        // mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
        // getClass().getName());
        // mWakeLock.acquire();

    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        // if we get here, length is known, now set indeterminate to false
        //mProgressDialog.setIndeterminate(false);
        //mProgressDialog.setMax(100);
        //mProgressDialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        taskListener.onPostExecute(result,file_name);


    }

}