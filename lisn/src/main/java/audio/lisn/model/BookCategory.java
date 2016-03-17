package audio.lisn.model;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Rasika on 12/27/15.
 */
public class BookCategory implements Serializable {

    private static final long serialVersionUID = -7060210544600464423L;
    private int id;
    private String name;
    private String english_name;

    public BookCategory(JSONObject obj){
        try {
            if(obj.getString("name") !=null)
                this.name = obj.getString("name");
            if(obj.getString("english_name") !=null)
                this.english_name = obj.getString("english_name");
            if(obj.getString("id") !=null){
                String idValue=obj.getString("id");
                this.id=Integer.parseInt(idValue);

            }
        }catch (Exception e){

        }

    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglish_name() {
        return english_name;
    }

    public void setEnglish_name(String english_name) {
        this.english_name = english_name;
    }
}
