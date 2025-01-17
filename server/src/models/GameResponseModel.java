package models;

import java.util.Map;
/**
 *
 * @author Ism
 */
public class GameResponseModel {
    private String status;
    private String message;
    private Map<String, String> data;

    public GameResponseModel(String status, String message, Map<String, String> data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}