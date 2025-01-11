/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

/**
 *
 * @author El-Wattaneya
 */
public class RequsetModel {
     String action;
    Object data;

    public RequsetModel(String action, Object data) {
        this.action = action;
        this.data = data;
    }
}

