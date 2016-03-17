package audio.lisn.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import audio.lisn.util.AppUtils;
import audio.lisn.util.Log;

public class AudioBook implements Serializable{

    private static final long serialVersionUID = -7060210544600464481L;

    private String ISBN,book_id,duration,narrator,title, description, author, language, price, category,rate,
            cover_image,banner_image, preview_audio,english_title,english_description,downloads;
    private boolean isPurchase;
    private int lastPlayFileIndex;
    private int lastSeekPoint;
    private int downloadCount;
    private float previewDuration;
    private int discount;
    private boolean isAwarded;
    private boolean isDownloaded;
    private int audioFileCount;
    private int fileSize;

    private ArrayList<Integer> downloadedChapter = new ArrayList<Integer>();
    private ArrayList<BookReview> reviews=new ArrayList<>();



    public int imageResId;

    LanguageCode lanCode;

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean isDownloaded) {
        this.isDownloaded = isDownloaded;
    }


    public float getPreviewDuration() {
        return previewDuration;
    }

    public void setPreviewDuration(float previewDuration) {
        this.previewDuration = previewDuration;
    }

    public int getAudioFileCount() {
        return audioFileCount;
    }

    public void setAudioFileCount(int audioFileCount) {
        this.audioFileCount = audioFileCount;
    }

    public ArrayList<Integer> getDownloadedChapter() {
        return downloadedChapter;
    }

    public void setDownloadedChapter(ArrayList<Integer> downloadedChapter) {
        this.downloadedChapter = downloadedChapter;
    }
    public void addChapterToDownloadedChapter(int chapter) {
        this.downloadedChapter.add(chapter);
    }

    public String[] getAudio_file_urls() {
        return null;
    }

    public HashMap getDownloadedFileList() {
        return null;
    }

    public void addFileToDownloadedList(Integer file_name, String file_url) {
    }

    public String getBanner_image() {
        return banner_image;
    }

    public void setBanner_image(String banner_image) {
        this.banner_image = banner_image;
    }

    public String getDownloads() {
        return downloads;
    }

    public void setDownloads(String downloads) {
        this.downloads = downloads;
    }

    public ArrayList<BookReview> getReviews() {
        return reviews;
    }

    public void setReviews(ArrayList<BookReview> reviews) {
        this.reviews = reviews;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public boolean isAwarded() {
        return isAwarded;
    }

    public void setIsAwarded(boolean isAwarded) {
        this.isAwarded = isAwarded;
    }

    public String getEnglish_description() {
        return english_description;
    }

    public void setEnglish_description(String english_description) {
        this.english_description = english_description;
    }


    public enum LanguageCode {
		LAN_EN, LAN_SI
	}
    public enum SelectedAction {
        ACTION_MORE, ACTION_PREVIEW,ACTION_DETAIL,ACTION_PURCHASE,ACTION_PLAY,ACTION_DELETE,ACTION_DOWNLOAD
    }

    public AudioBook() {
    }
    public AudioBook(JSONObject obj,int position,Context context) {
        String book_id="";
        Log.v("obj","obj: "+obj.toString());
        try{
            book_id=obj.getString("book_id");
            if(obj.getString("author") !=null)
                this.author = obj.getString("author");
            if(obj.getString("cover_image") !=null)
                this.cover_image = obj.getString("cover_image");
            if(obj.getString("category") !=null)
                this.category = obj.getString("category");
            if(obj.getString("description") !=null)
                this.description = obj.getString("description");
            if(obj.getString("language") !=null)
                this.language = obj.getString("language");
            if(obj.getString("preview_audio") !=null)
                this.preview_audio = obj.getString("preview_audio");
            if(obj.getString("price") !=null)
                this.price = obj.getString("price");
            if(obj.getString("title") !=null)
                this.title = obj.getString("title");
            if(obj.getString("english_title") !=null)
                this.english_title = obj.getString("english_title");
            if(obj.getString("rate") !=null)
                this.rate = obj.getString("rate");
            if(obj.getString("duration") !=null)
                this.duration = obj.getString("duration");
            if(obj.getString("narrator") !=null)
                this.narrator = obj.getString("narrator");
//            if(obj.getString("downloads") !=null)
//                this.downloads = obj.getString("downloads");
//            Log.v("audio_file",""+obj.getString("audio_file"));
            if(obj.getString("audio_file") !=null){
                String audio_file=obj.getString("audio_file");
                this.audioFileCount=Integer.parseInt(audio_file);

            }
            if(obj.getString("english_description") !=null && !obj.getString("english_description").equalsIgnoreCase("null") ){
                this.english_description=obj.getString("english_description");

            }else{
                this.english_description=this.english_title;

            }
            if(obj.getString("size") !=null){
                String size=obj.getString("size");
                this.fileSize=Integer.parseInt(size);

            }
            if(obj.getString("award") !=null){
                String award=obj.getString("award");
                Log.v("award", "award: " + award);
                if(Integer.parseInt(award) == 1){
                    this.isAwarded=true;
                }

            }
            if(obj.getString("discount") !=null){
                String discountValue=obj.getString("discount");
                this.discount=Integer.parseInt(discountValue);
                //Log.v("discount","discount "+discount);
            }

            if(obj.getString("banner_image") !=null)
                this.banner_image = obj.getString("banner_image");


//            JSONArray arr = obj.getJSONArray("audio_file");
//            String[] list = new String[arr.length()];
//            for(int index = 0; index< arr.length(); index++) {
//                list[index] = arr.getString(index);
//            }
//            this.audio_file_urls=list;

            this.book_id=book_id;
            this.ISBN=book_id;
            if (language.equalsIgnoreCase("si")) {
                this.lanCode = LanguageCode.LAN_SI;
            } else {
                this.lanCode = LanguageCode.LAN_EN;
            }


            if(obj.get("reviews") !=null && (obj.get("reviews") instanceof JSONArray)){
                JSONArray arr = obj.getJSONArray("reviews");
                ArrayList<BookReview> reviewArray= new ArrayList<>();
               // Log.v("reviews:","reviews:"+arr);
                for(int index = 0; index< arr.length(); index++) {

                    String userName="";

                    JSONObject dataObject=arr.getJSONObject(index);

                    BookReview bookReview=new BookReview();

                    if(dataObject.getString("comment_title") !=null)
                        bookReview.setTitle(dataObject.getString("comment_title"));
                    if(dataObject.getString("comment") !=null)
                        bookReview.setMessage(dataObject.getString("comment"));
                    if(dataObject.getString("time") !=null)
                        bookReview.setTimeString(dataObject.getString("time"));
                    if(dataObject.getString("first_name") !=null){
                        userName=dataObject.getString("first_name");
                    }
                    if(dataObject.getString("last_name") !=null){
                        userName=userName+" "+dataObject.getString("last_name");
                    }
                        bookReview.setUserName(userName);
                    if(dataObject.getString("rate") !=null)
                        bookReview.setRateValue(dataObject.getString("rate"));

                    reviewArray.add(bookReview);

                }
               // Log.v("reviewArray","reviewArray:"+reviewArray.size());
                this.reviews=reviewArray;

            }
            if(context !=null) {
                this.isPurchase = isBookDownloaded(book_id, context);
            }



        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    private boolean isBookDownloaded(String key,Context context){
        AudioBook returnBook=null;
        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(context);
        //downloadedAudioBook.readFileFromDisk(context);
        HashMap< String, AudioBook> hashMap=downloadedAudioBook.getBookList(context);

        returnBook=hashMap.get(key);
        if(returnBook !=null){
            returnBook.setDescription(this.description);
            returnBook.setRate(this.rate);
            returnBook.setTitle(this.title);
            returnBook.setEnglish_title(this.english_title);
            returnBook.setEnglish_description(this.english_description);
            returnBook.setCategory(this.category);
            returnBook.setAuthor(this.author);
            returnBook.setCover_image(this.cover_image);
            returnBook.setBanner_image(this.banner_image);
            returnBook.setPreview_audio(this.preview_audio);
            returnBook.setAudioFileCount(this.audioFileCount);
            returnBook.setPrice(this.price);
            if(this.reviews.size()>returnBook.reviews.size()){
                returnBook.setReviews(this.reviews);
            }else{
                this.reviews=returnBook.getReviews();
            }
            returnBook.setIsAwarded(this.isAwarded);

            this.setDownloadedChapter(returnBook.getDownloadedChapter());

            downloadedAudioBook.addBookToList(context,
                    returnBook.getBook_id(), returnBook);
            return  true;
        }else{
            return  false;

        }

    }
	public AudioBook(String ISBN,String title,String english_title, String description, String author,
			String language, String price,String category, String cover_image, String preview_audio,String[] audio_file) {
		this.ISBN = ISBN;
		this.title = title;
		this.english_title=english_title;
		this.description = description;
		this.author = author;
		this.language = language;
		this.language = language;
		this.price = price;
		this.category = category;
		this.cover_image = cover_image;
		this.preview_audio = preview_audio;

    }
    public String getBook_id() {
        return book_id;
    }

    public void setBook_id(String book_id) {
        this.book_id = book_id;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getNarrator() {
        return narrator;
    }

    public void setNarrator(String narrator) {
        this.narrator = narrator;
    }

    public int getLastPlayFileIndex() {
        return lastPlayFileIndex;
    }

    public void setLastPlayFileIndex(int lastPlayFileIndex) {
        this.lastPlayFileIndex = lastPlayFileIndex;
    }

    public int getLastSeekPoint() {
        return lastSeekPoint;
    }

    public void setLastSeekPoint(int lastSeekPoint) {
        this.lastSeekPoint = lastSeekPoint;
    }


    public String getISBN() {
		return ISBN;
	}

	public void setISBN(String iSBN) {
		ISBN = iSBN;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCover_image() {
		return cover_image;
	}

	public void setCover_image(String cover_image) {
		this.cover_image = cover_image;
	}

	public String getPreview_audio() {
		return preview_audio;
	}

	public void setPreview_audio(String preview_audio) {
		this.preview_audio = preview_audio;
	}


	public String getLanguage() {
		return language;
	}


	public void setLanguage(String language) {
		this.language = language;
		if (language.equalsIgnoreCase("si")) {
			this.lanCode = LanguageCode.LAN_SI;
		} else {
			this.lanCode = LanguageCode.LAN_EN;
		}
	}

	public LanguageCode getLanguageCode() {
		return lanCode;

	}

	public String getEnglish_title() {
		return english_title;
	}

	public void setEnglish_title(String english_title) {
		this.english_title = english_title;
	}

//	public String[] getAudio_file_urls() {
//		return audio_file_urls;
//	}

    public boolean isPurchase() {
        return isPurchase;
    }

    public void setPurchase(boolean isPurchase) {
        this.isPurchase = isPurchase;
    }

//    public void setAudio_file_urls(String[] audio_file_urls) {
//		this.audio_file_urls = audio_file_urls;
//	}
//    public HashMap<String, String> getDownloadedFileList() {
//        return downloadedFileList;
//    }
//    public void addFileToDownloadedList(String key,String url){
//
//        if(downloadedFileList == null){
//            downloadedFileList =new HashMap<String, String>();
//        }
//        downloadedFileList.put(key, url);
//
//    }
    public void removeDownloadedFile(Context context){
        if(this.downloadedChapter != null){
            this.downloadedChapter.clear();
        }

        String dirPath = AppUtils.getDataDirectory(context)
                + this.getBook_id();
        File dir = new File(dirPath);
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {

                new File(dir, children[i]).delete();
            }
        }
     //   this.setDownloadCount(0);

    }
    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

}