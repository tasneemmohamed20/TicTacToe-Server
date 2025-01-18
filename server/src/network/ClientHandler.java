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
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import models.GameModel;
import models.RequsetModel;

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
    private String name;

    public ClientHandler(Socket socket, DAO dbManager) {
        try {
            this.socket = socket;
            this.dbManager = dbManager;
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            clientsVector.add(this);

            start();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (dis.available() > 0) {
                    String jsonRequest = dis.readUTF();
                    RequsetModel request = gson.fromJson(jsonRequest, RequsetModel.class);

                    Type userType = new TypeToken<UserModel>() {
                    }.getType();
                    UserModel user = gson.fromJson(gson.toJson(request.getData()), userType);

                    String jsonResponse;
                    switch (request.getAction()) {
                        case "register":
                            jsonResponse = handleRegistration(user);
                            dos.writeUTF(jsonResponse);
                            break;
                        case "login":
                            jsonResponse = handleLogin(user);
                            dos.writeUTF(jsonResponse);
                            break;
                        case "fetchOnline":
                            jsonResponse = handleFetchOnlineUsers(name);
                            dos.writeUTF(jsonResponse);
                            break;
                        case "invite":
                            sendInvite(request);
                            break;
                        case "cancel":
                            cancelInvite(request);
                            break;
                        case "accept":
                            acceptInvitation(request);
                            break;
                        default:
                            jsonResponse = gson.toJson(new ResponsModel("error", "Invalid action", null));
                    }

                    
                } else {

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
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
public void closeConnection() {
        try {
            clientsVector.remove(this);
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
            System.out.println("Connection with client " + name + " closed.");
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
public String receiveMessage() {
    try {
        return dis.readUTF();
    } catch (IOException ex) {
        Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        return null;
    }
}

 void sendMessage(ResponsModel response) {
    String responseJson = new Gson().toJson(response);
    System.out.println("Sending response: " + responseJson);
    try {
        dos.writeUTF(responseJson);
        dos.flush();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    /* private void sendMessageToAll(String message) {
        for (ClientHandler client : clientsVector) {
            try {
                client.dos.writeUTF(message);
            } catch (IOException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }*/
    
    private void sendInvite(RequsetModel request) {
        
        Map<String, String> data = (Map<String, String>) request.getData();
        String sender = data.get("sender");
        String receiver = data.get("receiver");

        System.out.println("Sending invitation from " + sender + " to " + receiver);

        for (ClientHandler client : clientsVector) {
            if (receiver.equals(client.name)) {
               try {
                    client.dos.writeUTF(gson.toJson(new ResponsModel(
                        "invitation",
                        sender + " wants to play with you.",
                        data
                    )));
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            } 
        }
        
        for (ClientHandler client : clientsVector) {

            if (sender.equalsIgnoreCase(client.name)){
                try {
                    client.dos.writeUTF(gson.toJson(new ResponsModel(
                        "wait",
                        "Wait for response.",
                        data
                    )));
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }


        try {
            dos.writeUTF(gson.toJson(new ResponsModel(
                "error",
                "User " + receiver + " is not available.",
                null
            )));
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void cancelInvite(RequsetModel request) {
        
        Map<String, String> data = (Map<String, String>) request.getData();
        String sender = data.get("sender");
        String receiver = data.get("receiver");

        System.out.println("cancel invitation from " + sender + " to " + receiver);

        for (ClientHandler client : clientsVector) {

            if (sender.equalsIgnoreCase(client.name)){
                try {
                    client.dos.writeUTF(gson.toJson(new ResponsModel(
                        "cancel",
                        receiver + " Reject the invitation.",
                        data
                    )));
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
     }
     

     try {
         dos.writeUTF(gson.toJson(new ResponsModel(
             "error",
             "User " + receiver + " is not available.",
             null
         )));
     } catch (IOException ex) {
         Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
     }
 }
private void acceptInvitation(RequsetModel request) {
    System.out.println("Received acceptInvitation request: " + new Gson().toJson(request));

    Map<String, String> data = (Map<String, String>) request.getData();
    System.out.println("Extracted data from request: " + new Gson().toJson(data));

    if (data == null || !data.containsKey("sender") || !data.containsKey("receiver")) {
        sendMessage(new ResponsModel("error", "Invalid invitation data. Please try again.", null));
        return;
    }

    String player1Name = data.get("sender");
    String player2Name = data.get("receiver");

    System.out.println("Sender: " + player1Name + ", Receiver: " + player2Name);

    if (player1Name == null || player2Name == null || player1Name.isEmpty() || player2Name.isEmpty()) {
        sendMessage(new ResponsModel("error", "Player names cannot be null or empty.", null));
        return;
    }

    if (player1Name.equals(player2Name)) {
        sendMessage(new ResponsModel("error", "You cannot start a game with yourself.", null));
        return;
    }

    ClientHandler player1 = null;
    ClientHandler player2 = null;

    synchronized (clientsVector) {
        for (ClientHandler client : clientsVector) {
            if (player1Name.equals(client.name)) {
                player1 = client;
            }
            if (player2Name.equals(client.name)) {
                player2 = client;
            }
        }
    }

    if (player1 == null || player2 == null) {
        String missingPlayer = (player1 == null) ? player1Name : player2Name;
        sendMessage(new ResponsModel("error", "Player " + missingPlayer + " not found or offline.", null));
        return;
    }

    String gameId = "game-" + System.currentTimeMillis();

    GameModel gameModel = new GameModel(gameId, player1Name, "X", player2Name, "O");
    System.out.println("GameModel created: " + new Gson().toJson(gameModel));

    ResponsModel response = new ResponsModel("gameStart", "Game started successfully.", gameModel);

    player1.sendMessage(response);
    player2.sendMessage(response);

    new GameThread(player1, player2, gameModel).start();
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
            return gson.toJson(new ResponsModel("error", "found error : " + ex, null));
        }
    }

    private String handleLogin(UserModel user) {
        try {

            UserModel data = dbManager.loginForUser(user.getUserName(), user.getPassword());
            if (data != null) {
                dbManager.updateUserStatus(user.getUserName(), "online");
                name = user.getUserName();
                return gson.toJson(new ResponsModel("success", "Login successful.", data));
            } else {
                return gson.toJson(new ResponsModel("error", "Invalid username or password.", null));
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            return gson.toJson(new ResponsModel("error", "found error : " + ex, null));
        }
    }

    private String handleFetchOnlineUsers(String name) {
        Vector<String> users = new Vector<String>();
        try {
            users = DAO.getAllInlineUsers();
            users.add(name);
            System.out.println(gson.toJson(new ResponsModel("success", "Data fetched successfully.", users)));
            return gson.toJson(new ResponsModel("success", "Data fetched successfully.", users));
        } catch (SQLException ex) {
            return gson.toJson(new ResponsModel("error", "found error : " + ex, null));
        }
    }
}


/*class Request {

    String action;
    Object data;

    public Request(String action, Object data) {
        this.action = action;
        this.data = data;
    }
}*/