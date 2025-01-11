/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import db.DAO;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private ServerSocket serverSocket;
    private boolean isRunning = true;
    private DAO dbManager;

    public Server() {

    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(5005);
            dbManager = new DAO();
            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("Server accepting connections.");
                    new ClientHandler(socket, dbManager);
                } catch (SocketException ex) {
                    if (!isRunning) {
                        System.out.println("Server stopped accepting connections.");
                        break; 
                    }
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            stopServer();
        }
    }

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Server stopped.");
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            dbManager.close();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }
}
