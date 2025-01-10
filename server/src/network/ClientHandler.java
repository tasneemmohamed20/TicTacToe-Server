/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ALANDALUS
 */
public class ClientHandler extends Thread {

    Map<String, String> map = new HashMap<>();
    ObjectInputStream inputObject;
    ObjectOutputStream outputObject;
    Socket socket;

    public static List<ClientHandler> clientsVector = new CopyOnWriteArrayList<>();

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            outputObject = new ObjectOutputStream(socket.getOutputStream());
            inputObject = new ObjectInputStream(socket.getInputStream());
            clientsVector.add(this);
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void sendRequestToAll() {
        List<Map<String, String>> allPlayers = new ArrayList<>();
        for (ClientHandler ch : clientsVector) {
            allPlayers.add(ch.map);
        }

        for (ClientHandler ch : clientsVector) {
            try {
                ch.outputObject.writeObject(allPlayers);
            } catch (IOException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void run() {
        try {
            while (true) {
                map = (Map<String, String>) inputObject.readObject();
                sendRequestToAll();
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, "Client disconnected: " + socket, ex);
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            clientsVector.remove(this);
            if (inputObject != null) inputObject.close();
            if (outputObject != null) outputObject.close();
            if (socket != null) socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class Message implements Serializable {

    int senderId;
    int receiverId;
    String message;

    public Message(int senderId, int receiverId, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
    }
}

