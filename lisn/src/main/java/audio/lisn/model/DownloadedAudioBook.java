package audio.lisn.model;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import audio.lisn.util.AppUtils;

public class DownloadedAudioBook implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1322L;
	private HashMap<String, AudioBook> bookList=null;
	
	public DownloadedAudioBook (Context context) {

		//File file = new File(AppUtils.getDataDirectory(context));
		readFileFromDisk(context);
	}
	
	public HashMap<String, AudioBook> getBookList(Context context) {
			readFileFromDisk(context);


		return bookList;
	}

	public void setBookList(HashMap<String, AudioBook> bookList) {
		this.bookList = bookList;
	}
	public void addBookToList(Context context,String key, AudioBook audioBook) {
		readFileFromDisk(context);

		if(bookList !=null) {
			bookList.put(key, audioBook);
			writeFileToDisk(context);
		}
	}
	public void removeBook(Context context){

		if(bookList !=null) {
			bookList.clear();
			writeFileToDisk(context);
		}
	}

	
	public boolean writeFileToDisk(Context context) {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File file = new File(AppUtils.getDataDirectory(context)+"book.ser");
			try {
                if (!file.exists()) {
					file.getParentFile().mkdirs();

					//file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(bookList);
				out.close();
                fos.close();
				return true;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return false;
	}

	public boolean readFileFromDisk(Context context) {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			File file = new File(AppUtils.getDataDirectory(context)+"book.ser");
			if (file.exists()) {

				try {
                    FileInputStream fis = new FileInputStream(AppUtils.getDataDirectory(context)+"book.ser");
                    ObjectInputStream in = new ObjectInputStream(fis);
                    bookList = (HashMap) in.readObject();
					in.close();
                    fis.close();
					return true;
				} catch (Exception ex) {
					bookList = new HashMap<String, AudioBook>();

					ex.printStackTrace();
				} 
			} else{
				file.getParentFile().mkdirs();
				bookList = new HashMap<String, AudioBook>();
			}
		}else{
			bookList = new HashMap<String, AudioBook>();

		}
		return false;
	}
}