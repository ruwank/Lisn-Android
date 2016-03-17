package audio.lisn.model;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Rasika on 10/18/15.
 */
public class BookReview implements Serializable {

    private static final long serialVersionUID = -7060210544600423456L;
    private String userId;
    private String userName;
    private String title;
    private String message;
    private String timeString;
    private String rateValue;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimeString() {

        String _Date = timeString;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat fmt2 = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date date = fmt.parse(_Date);
            return fmt2.format(date);
        }
        catch(ParseException pe) {

            return timeString;
        }
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    public String getRateValue() {
        return rateValue;
    }

    public void setRateValue(String rateValue) {
        this.rateValue = rateValue;
    }
}
