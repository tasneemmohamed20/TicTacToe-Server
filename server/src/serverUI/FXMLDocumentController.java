/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverUI;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import network.ClientHandler;

/**
 * FXML Controller class
 *
 * @author ALANDALUS
 */
public class FXMLDocumentController implements Initializable {
    
    ServerSocket serverSocket;
    Socket socket;
    Thread thread;
    boolean isStarted;

    @FXML
    private Button startButton;
    @FXML
    private Button endButton;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void handleStartButton(ActionEvent event) {
        try {
            serverSocket = new ServerSocket(5005);
            System.out.println("Server Started");
            thread = new Thread(()
                    -> {
                while (!Thread.currentThread().interrupted()) {
                    try {
                        socket = serverSocket.accept();
                        new ClientHandler(socket); 

                    } catch (IOException ex) {
                        if (!serverSocket.isClosed()) {
                            Logger.getLogger(ServerUI.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            });
            thread.start();
            isStarted = true;
            startButton.setDisable(true);
            endButton.setDisable(false);

        } catch (IOException ex) {
            Logger.getLogger(ServerUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleEndButton(ActionEvent event) {
        stop();
    }
    
public void stop() {
        if (isStarted) {
//            for (String key : ClientHandler.onlineUsers.keySet()) {
//                ClientHandler.onlineUsers.get(key).writeMessageToClients("SERVER CLOSED");
//            }
            thread.interrupt();
            System.out.println("Stop Server");
            if (!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(ServerUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        isStarted = false;
        startButton.setDisable(false);
        endButton.setDisable(true);
    }
    
}
