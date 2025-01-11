/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import com.google.gson.Gson;
import db.DAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.ResponsModel;
import models.UserDataModel;
import models.UserModel;

/**
 *
 * @author ALANDALUS
 */
public class ClientHandler extends Thread {

    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;
    private Gson gson = new Gson();
    private static Vector<ClientHandler> clientsVector = new Vector<>();
    private DAO dbManager;

    public ClientHandler(Socket socket, DAO dbManager) {
        try {
            this.socket = socket;
            this.dbManager = dbManager;
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            clientsVector.add(this);

            String jsonRequest = dis.readUTF();
            Request request = gson.fromJson(jsonRequest, Request.class);
            String jsonResponse;

            switch (request.action) {
                case "register":
                    jsonResponse = handleRegistration((UserModel) request.data);
                    break;
                case "login":
                    jsonResponse = handleLogin((UserModel) request.data);
                    break;
                default:
                    jsonResponse = gson.toJson(new ResponsModel("error", "Invalid action", null));
            }

            dos.writeUTF(jsonResponse);
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = dis.readUTF();
                sendMessageToAll(message);
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                clientsVector.remove(this);
                dis.close();
                dos.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void sendMessageToAll(String message) {
        for (ClientHandler client : clientsVector) {
            try {
                client.dos.writeUTF(message);
            } catch (IOException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private String handleRegistration(UserModel user) {
        try {
            boolean success = dbManager.registerForUser(user.getUserName(), user.getPassword());
            if (success) {
                return gson.toJson(new ResponsModel("success", "User registered successfully.", null));
            } else {
                return gson.toJson(new ResponsModel("error", "Registration failed. Username already exist.", null));
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            return gson.toJson(new ResponsModel("error", "found error : "+ex, null));
        }
    }

    private String handleLogin(UserModel user) {
        try {
            boolean isValid = dbManager.loginForUser(user.getUserName(), user.getPassword());
            if (isValid) {
                return gson.toJson(new ResponsModel("success", "Login successful.", null));
            } else {
                return gson.toJson(new ResponsModel("error", "Invalid username or password.", null));
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            return gson.toJson(new ResponsModel("error", "found error : "+ex, null));
        }
    }
}

class Request {

    String action;
    Object data;

    public Request(String action, Object data) {
        this.action = action;
        this.data = data;
    }
}


