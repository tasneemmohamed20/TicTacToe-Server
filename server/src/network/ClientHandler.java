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
import java.util.concurrent.ConcurrentHashMap;
import models.GameModel;
import models.GameRequestModel;
import models.GameResponseModel;
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
    private static Map<String, GameModel> activeGames = new ConcurrentHashMap<>();
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
                         case "startGame":
                            handleStartGame(gson.fromJson(jsonRequest, GameRequestModel.class));
                            break;
                        case "move": 
                            handleMove(gson.fromJson(jsonRequest, GameRequestModel.class));
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
                 handleDisconnection();
                clientsVector.remove(this);
                dis.close();
                dos.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

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
    Map<String, String> data = (Map<String, String>) request.getData();
    String player1 = data.get("sender");
    String player2 = data.get("receiver");
    String gameKey = player1 + "_" + player2;

    synchronized (activeGames) {
        if (!activeGames.containsKey(gameKey)) {
            GameModel game = new GameModel(player1, player2);
            activeGames.put(gameKey, game);

            System.out.println("Game added to activeGames: " + gameKey);

            for (ClientHandler client : clientsVector) {
                if (player1.equalsIgnoreCase(client.name) || player2.equalsIgnoreCase(client.name)) {
                    try {
                        client.dos.writeUTF(gson.toJson(new ResponsModel(
                            "accept",
                            "Game started.",
                            game
                        )));
                    } catch (IOException ex) {
                        Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
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
    
private void handleStartGame(GameRequestModel request) throws IOException {
    String player1 = request.getData().get("player1");
    String player2 = request.getData().get("player2");
    String gameKey = player1.compareTo(player2) < 0 ? player1 + "_" + player2 : player2 + "_" + player1;

    if (!activeGames.containsKey(gameKey)) {
        dos.writeUTF(gson.toJson(new ResponsModel("error", "No active game found between the players.", null)));
        return;
    }

    GameModel game = activeGames.get(gameKey);
    System.out.println("Game started between " + player1 + " and " + player2);
    dos.writeUTF(gson.toJson(new ResponsModel("success", "Game started.", game)));
}



private void handleMove(GameRequestModel request) throws IOException {
    String player = request.getData().get("player");
    String cell = request.getData().get("cell");
    String opponent = request.getData().get("opponent");

    String gameKey = player.compareTo(opponent) < 0 ? player + "_" + opponent : opponent + "_" + player;

    if (!activeGames.containsKey(gameKey)) {
        dos.writeUTF(gson.toJson(new ResponsModel("error", "No active game found.", null)));
        return;
    }

    GameModel game = activeGames.get(gameKey);

    synchronized (game) {
        try {
            int cellIndex = Integer.parseInt(cell);
            if (!game.makeMove(player, String.valueOf(cellIndex))) {
                dos.writeUTF(gson.toJson(new ResponsModel("error", "Invalid move or not your turn.", null)));
                return;
            }

            String gameState = game.checkGameState();
            sendGameUpdate(gameKey, game);

            if (!"ongoing".equals(gameState)) {
                notifyGameEnd(gameKey, gameState);
                activeGames.remove(gameKey);
            }
        } catch (NumberFormatException e) {
            dos.writeUTF(gson.toJson(new ResponsModel("error", "Cell index must be a number between 1 and 9.", null)));
        }
    }
}


private void sendGameUpdate(String gameKey, GameModel game) throws IOException {
    for (ClientHandler client : clientsVector) {
        if (gameKey.contains(client.name)) {
            synchronized (client) {
                client.dos.writeUTF(gson.toJson(new ResponsModel(
                    "update",
                    "Game state updated.",
                    game.getBoardState()
                )));
            }
        }
    }
}

private void notifyGameEnd(String gameKey, String result) throws IOException {
    for (ClientHandler client : clientsVector) {
        if (gameKey.contains(client.name)) {
            synchronized (client) {
                client.dos.writeUTF(gson.toJson(new ResponsModel(
                    "end",
                    result,
                    null
                )));
            }
        }
    }
}


private void handleDisconnection() {
    clientsVector.remove(this);
    String disconnectedPlayer = this.name;

    activeGames.entrySet().removeIf(entry -> {
        String gameKey = entry.getKey();
        if (gameKey.contains(disconnectedPlayer)) {
            GameModel game = entry.getValue();
            try {
                notifyGameEnd(gameKey, disconnectedPlayer + " disconnected. Game over.");
            } catch (IOException e) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, e);
            }
            return true;
        }
        return false;
    });

    System.out.println(disconnectedPlayer + " disconnected.");
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
