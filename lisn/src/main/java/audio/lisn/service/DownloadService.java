package audio.lisn.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;

import java.io.BufferedInputStream;
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
import audio.lisn.util.Log;

/**
 * Created by Admin on 12/31/15.
 */
public class DownloadService extends IntentService {
    public static final int UPDATE_PROGRESS = 8344;
    public DownloadService() {
        super("DownloadService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        String filePart=intent.getStringExtra("filePart");
        String book_id=intent.getStringExtra("book_id");
        String dirPath=intent.getStringExtra("dirPath");

        String urlToDownload = getResources().getString(R.string.book_download_url);
        String urlParameters  = "userid="+ AppController.getInstance().getUserId()+"&bookid="+book_id+"&part="+filePart;


        ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
        try {
            File rootPath = new File(dirPath);
            if (!rootPath.exists()) {
                rootPath.mkdirs();
            }

            URL url = new URL(urlToDownload);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            DataOutputStream printout = new DataOutputStream(connection.getOutputStream ());
            printout.writeBytes(urlParameters.toString());

            printout.flush ();
            printout.close ();
            connection.connect();
            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = connection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(connection.getInputStream());
            //OutputStream output = new FileOutputStream("/sdcard/BarcodeScanner-debug.apk");
            OutputStream output = new FileOutputStream(dirPath + "/" + filePart
                    + ".lisn");// change extension
            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;

                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            Log.v("DownloadService", "DownloadService: " + e.getMessage());
            e.printStackTrace();
        }
        Log.v("DownloadService","DownloadService: "+book_id);

        Bundle resultData = new Bundle();
        resultData.putString("file_name", filePart);
        resultData.putString("result", "OK");
        receiver.send(UPDATE_PROGRESS, resultData);
    }

}
