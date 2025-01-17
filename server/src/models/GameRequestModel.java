/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.util.Map;

/**
 *
 * @author Ism
 */
public class GameRequestModel {
    private String action;
    private Map<String, String> data;  

    public GameRequestModel(String action, Map<String, String> data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "GameRequestModel{" +
                "action='" + action + '\'' +
                ", data=" + data +
                '}';
    }
}