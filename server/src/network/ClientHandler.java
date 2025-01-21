package network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import db.DAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.GameModel;
import models.RequsetModel;
import models.ResponsModel;
import models.UserModel;

public class ClientHandler extends Thread {

    private static final String ACTION_REGISTER = "register";
    private static final String ACTION_LOGIN = "login";
    private static final String ACTION_FETCH_ONLINE = "fetchOnline";
    private static final String ACTION_INVITE = "invite";
    private static final String ACTION_CANCEL = "cancel";
    private static final String ACTION_ACCEPT = "accept";

    private Socket clientSocket;
    DataInputStream dis;
    DataOutputStream dos;
    private Gson gson = new Gson();
    private static Vector<ClientHandler> clientsVector = new Vector<>();
    private DAO dbManager;
    private String name;
    private boolean isRunning = true;
    private boolean isPlaying;
    private GameThread gameThread; // مرجع إلى GameThread

    public ClientHandler(Socket socket, DAO dbManager) {
        try {
            this.clientSocket = socket;
            this.dbManager = dbManager;
            dis = new DataInputStream(clientSocket.getInputStream());
            dos = new DataOutputStream(clientSocket.getOutputStream());
            clientsVector.add(this);
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                if (!isPlaying) {
                    try {

                        String jsonRequest = dis.readUTF();

                        System.out.println("Received from Client: " + jsonRequest);

                        RequsetModel request;
                        try {

                            request = gson.fromJson(jsonRequest, RequsetModel.class);

                        } catch (JsonSyntaxException e) {
                            System.err.println("Invalid JSON received: " + jsonRequest);
                            continue;
                        }

                        String jsonResponse;

                        switch (request.getAction()) {
                            case ACTION_REGISTER:
                                jsonResponse = handleRegistration(request);
                                break;
                            case ACTION_LOGIN:
                                jsonResponse = handleLogin(request);
                                break;
                            case ACTION_FETCH_ONLINE:
                                jsonResponse = handleFetchOnlineUsers();
                                break;
                            case ACTION_INVITE:
                                sendInvite(request);
                                continue;
                            case ACTION_CANCEL:
                                cancelInvite(request);
                                continue;
                            case ACTION_ACCEPT:
                                acceptInvitation(request);
                                continue;
                            case "move":
                                // تمرير الحركة إلى GameThread
                                System.out.println("data   " + request.getData());
                                if (gameThread != null) {
                                    gameThread.handleOpponentMove(request.getData());
                                } else {
                                    System.out.println("GameThread is not initialized.");
                                }
                                continue;

                            default:
                                jsonResponse = gson.toJson(new ResponsModel("error", "Invalid action", null));
                        }
                        System.out.println("jsonResponse:::" + jsonResponse);
                        dos.writeUTF(jsonResponse);

                    } catch (SocketException ex) {
                        System.err.println("Connection lost with client: " + name);
                        break;
                    } catch (IOException ex) {
                        Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                        break;
                    }
                }
            }
        } finally {
            // cleanup();
        }
    }

    private String handleRegistration(RequsetModel request) {
        try {
            UserModel user = gson.fromJson(gson.toJson(request.getData()), UserModel.class);
            boolean success = dbManager.registerForUser(user.getUserName(), user.getPassword());
            return success
                    ? gson.toJson(new ResponsModel("success", "User registered successfully.", null))
                    : gson.toJson(new ResponsModel("error", "Registration failed. Username already exists.", null));
        } catch (Exception ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            return gson.toJson(new ResponsModel("error", "Database error: " + ex.getMessage(), null));
        }
    }

    private String handleLogin(RequsetModel request) {
        try {
            UserModel user = gson.fromJson(gson.toJson(request.getData()), UserModel.class);
            UserModel data = dbManager.loginForUser(user.getUserName(), user.getPassword());
            if (data != null) {
                dbManager.updateUserStatus(user.getUserName(), "online");
                name = user.getUserName();
                return gson.toJson(new ResponsModel("success", "Login successful.", data));
            } else {
                return gson.toJson(new ResponsModel("error", "Invalid username or password.", null));
            }
        } catch (Exception ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            return gson.toJson(new ResponsModel("error", "Database error: " + ex.getMessage(), null));
        }
    }

    private String handleFetchOnlineUsers() {
        try {
            List<String> users = dbManager.getAllInlineUsers();
            users.add(name);
            return gson.toJson(new ResponsModel("success", "Data fetched successfully.", users));
        } catch (Exception ex) {
            return gson.toJson(new ResponsModel("error", "Database error: " + ex.getMessage(), null));
        }
    }

    private void sendInvite(RequsetModel request) {
        Map<String, String> data = (Map<String, String>) request.getData();
        String sender = data.get("sender");
        String receiver = data.get("receiver");

        ClientHandler receiverHandler = findClientHandler(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendToClient(new ResponsModel("invitation", sender + " wants to play with you.", data));
            findClientHandler(sender).sendToClient(new ResponsModel("wait", "Wait for response.", data));
        } else {
            findClientHandler(sender).sendToClient(new ResponsModel("error", "User " + receiver + " is not available.", null));
        }
    }

    private void cancelInvite(RequsetModel request) {
        Map<String, String> data = (Map<String, String>) request.getData();
        String sender = data.get("sender");
        String receiver = data.get("receiver");

        ClientHandler senderHandler = findClientHandler(sender);
        if (senderHandler != null) {
            senderHandler.sendToClient(new ResponsModel("cancel", receiver + " rejected the invitation.", data));
        }
    }

    private void acceptInvitation(RequsetModel request) {
        Map<String, String> data = (Map<String, String>) request.getData();
        String player1 = data.get("sender");
        String player2 = data.get("receiver");

        ClientHandler player1Handler = findClientHandler(player1);
        ClientHandler player2Handler = findClientHandler(player2);

        if (player1Handler != null && player2Handler != null) {
            GameModel game = new GameModel(player1, player2);

            // player1Handler.isRunning=false;
            //player2Handler.isRunning=false;
            player1Handler.isRunning = false;
            player2Handler.isRunning = false;
            player1Handler.isPlaying = true;
            player2Handler.isPlaying = true;

            gameThread = new GameThread(player1Handler.clientSocket, player2Handler.clientSocket, game, player1Handler, player2Handler);
            gameThread.start();

            System.out.println("Game Thread is Opened from fun accept invitation");
        } else {
            findClientHandler(player1).sendToClient(new ResponsModel("error", "One of the players is not available.", null));
        }
    }

    private ClientHandler findClientHandler(String playerName) {
        for (ClientHandler client : clientsVector) {
            if (playerName.equalsIgnoreCase(client.name)) {
                return client;
            }
        }
        return null;
    }

    void sendToClient(ResponsModel response) {
        try {
            String jsonResponse = gson.toJson(response);
            dos.writeUTF(jsonResponse);
            dos.flush();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, "Failed to send message to client.", ex);
        }
    }

    private void cleanup() {
        try {
            if (dis != null) {
                dis.close();
            }
            if (dos != null) {
                dos.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            clientsVector.remove(this);
            System.out.println("Client disconnected: " + name);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, "Error closing resources", ex);
        }
    }
}
