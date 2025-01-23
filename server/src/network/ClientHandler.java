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
    public String name;
    private volatile boolean isGameActive = false;
    private GameThread activeGameThread = null;
    private boolean isReady = false;

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public boolean isReady() {
        return isReady;
    }

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
            while (!socket.isClosed()) {
                if (isGameActive && activeGameThread != null) {
                    Thread.sleep(100);
                    continue;
                }

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
                        case "logout":
                            jsonResponse = handleLogout(request);
                            dos.writeUTF(jsonResponse);
                            break;
                        case "fetchOnline":
                            System.out.println("isGameActive = " + isGameActive);
                            if (isGameActive) {
                                jsonResponse = gson.toJson(new ResponsModel("error", "Cannot fetch online users during an active game.", null));
                            } else {
                                jsonResponse = handleFetchOnlineUsers(name);
                            }
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
                        case "updateScore":
                            updateScore((Map<String, String>) request.getData());
                            break;
                        case "withdraw":
                            handleQuitRequest(request);
                             break;
                        default:
                            jsonResponse = gson.toJson(new ResponsModel("error", "Invalid action", null));
                    }
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            closeConnection();
        }
    }

    private void handleQuitRequest(RequsetModel request) {
        Map<String, String> data = (Map<String, String>) request.getData();
        String quittingPlayer = data.get("player");
        String gameId = data.get("gameId");

        // إرسال رسالة إلى اللاعب الآخر
        System.err.println("[DEBUG] Sending quit message to opponent: " + quittingPlayer);
         sendMessage(new ResponsModel("withdraw", quittingPlayer + " has quit the game.", null));
        

        // إغلاق اللعبة
        //endGame(gameId);
    }

    private void updateScore(Map<String, String> data) {
        String name = data.get("name");
        try {
            boolean isUpdate = DAO.updateScoreByUsername(name);
            if (isUpdate) {
                System.out.println("score" + name + "updated successfly.==============================");
            } else {
                System.out.println("something error!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void closeConnection() {
        try {
            if (name != null) {
                dbManager.updateUserStatus(name, "offline");
            }
            clientsVector.remove(this);
            if (dis != null) {
                dis.close();
            }
            if (dos != null) {
                dos.close();
            }
            if (socket != null) {
                socket.close();
            }
            System.out.println("Connection with client " + name + " closed.");
        } catch (IOException | SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String receiveMessage() {
        try {

            if (socket == null || socket.isClosed()) {
                return null;
            }

            String ss = dis.readUTF();
            System.out.println("RECEIVE MESSAGE UTF " + ss);
            return ss;

        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            closeConnection();
            return null;
        }
    }

    public void sendMessage(ResponsModel response) {
        try {
            dos.writeUTF(gson.toJson(response));
            dos.flush();
            System.out.println("[DEBUG] Sent message to client: " + gson.toJson(response));
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendInvite(RequsetModel request) {
        Map<String, String> data = (Map<String, String>) request.getData();
        String sender = data.get("sender");
        String receiver = data.get("receiver");

        System.out.println("[DEBUG] Sending invitation from: " + sender + " to: " + receiver);

        boolean receiverFound = false;
        for (ClientHandler client : clientsVector) {
            if (receiver.equals(client.name)) {
                receiverFound = true;
                try {
                    client.dos.writeUTF(gson.toJson(new ResponsModel(
                            "invitation",
                            sender + " wants to play with you.",
                            data
                    )));
                    System.out.println("[DEBUG] Invitation sent to: " + receiver);
                } catch (IOException ex) {
                    System.err.println("[ERROR] Failed to send invitation to: " + receiver + ". Error: " + ex.getMessage());
                }
            }
        }

        if (!receiverFound) {
            System.out.println("[DEBUG] Receiver not found: " + receiver);
            try {
                dos.writeUTF(gson.toJson(new ResponsModel(
                        "error",
                        "User " + receiver + " is not available.",
                        null
                )));
            } catch (IOException ex) {
                System.err.println("[ERROR] Failed to notify sender about unavailable receiver. Error: " + ex.getMessage());
            }
        }
    }

    private void acceptInvitation(RequsetModel request) {
        Map<String, String> data = (Map<String, String>) request.getData();
        String sender = data.get("sender");
        String receiver = data.get("receiver");

        System.out.println("[DEBUG] Accepting invitation from " + receiver + " for " + sender);

        ClientHandler player1 = null;
        ClientHandler player2 = null;

        synchronized (clientsVector) {
            for (ClientHandler client : clientsVector) {
                if (client.name.equals(sender)) {
                    player1 = client;
                }
                if (client.name.equals(receiver)) {
                    player2 = client;
                }
                if (player1 != null && player2 != null) {
                    break;
                }
            }
        }

        if (player1 != null && player2 != null) {
            String gameId = "game-" + System.currentTimeMillis();
            GameModel gameModel = new GameModel(gameId, sender, "X", receiver, "O");

            System.out.println("[DEBUG] Starting game between " + sender + " and " + receiver);

            ResponsModel gameStartResponse = new ResponsModel("gameStart", "Game started successfully.", gameModel);

            player1.sendMessage(gameStartResponse);
            player2.sendMessage(gameStartResponse);

            GameThread gameThread = new GameThread(player1, player2, gameModel);
            player1.startGame(gameThread);
            player2.startGame(gameThread);
            gameThread.start();
        } else {
            System.err.println("[ERROR] One or both players not found. Sender: " + sender + ", Receiver: " + receiver);
        }
    }

    private void cancelInvite(RequsetModel request) {

        Map<String, String> data = (Map<String, String>) request.getData();
        String sender = data.get("sender");
        String receiver = data.get("receiver");

        System.out.println("cancel invitation from " + sender + " to " + receiver);

        for (ClientHandler client : clientsVector) {

            if (sender.equalsIgnoreCase(client.name)) {
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

    public void startGame(GameThread gameThread) {
        isGameActive = true;
        activeGameThread = gameThread;
    }

    public void endGame() {
        isGameActive = false;
        activeGameThread = null;
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

    private String handleLogout(RequsetModel request) {
        try {
            Map<String, String> data = (Map<String, String>) request.getData();
            if (data == null || !data.containsKey("username")) {
                return gson.toJson(new ResponsModel("error", "Logout failed: Missing username in request.", null));
            }

            String username = data.get("username");
            if (dbManager.updateUserStatus(username, "offline")) {
                closeConnection();
                return gson.toJson(new ResponsModel("success", "Logout successful.", null));
            } else {
                return gson.toJson(new ResponsModel("error", "Logout failed: User not found or already offline.", null));
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, "Error updating user status during logout", ex);
            return gson.toJson(new ResponsModel("error", "Error during logout: " + ex.getMessage(), null));
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
