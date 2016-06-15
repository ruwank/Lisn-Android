package audio.lisn.model;

import java.io.Serializable;

/**
 * Created by Rasika on 5/27/16.
 */
public class BookChapter implements Serializable {

    private static final long serialVersionUID = -7060210544600423324L;

    private String title,english_title;
    private int chapter_id;
    private boolean isPurchased;
    private float discount,size,price;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEnglish_title() {
        return english_title;
    }

    public void setEnglish_title(String english_title) {
        this.english_title = english_title;
    }

    public int getChapter_id() {
        return chapter_id;
    }

    public void setChapter_id(int chapter_id) {
        this.chapter_id = chapter_id;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setIsPurchased(boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    public float getDiscount() {
        return discount;
    }

    public void setDiscount(float discount) {
        this.discount = discount;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
}
