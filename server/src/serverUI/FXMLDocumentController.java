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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import network.ClientHandler;
import network.Server;

/**
 * FXML Controller class
 *
 * @author ALANDALUS
 */
public class FXMLDocumentController implements Initializable {

    Server server;
    Thread thread;
    boolean isRunning = false;

    @FXML
    private Button startButton;
    @FXML
    private Button endButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML
    private void handleStartButton(ActionEvent event) {
        try {
            if (server == null) {
                server = new Server();
            }
            thread = new Thread(() -> server.startServer());
            thread.start();
            isRunning = true;
            System.out.println("Server Started");

            startButton.setDisable(true);
            endButton.setDisable(false);

        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @FXML
    private void handleEndButton(ActionEvent event) {
        stop();
    }

    public void stop() {
        if (server != null && isRunning) {
            try {
                server.stopServer();
                if (thread != null && thread.isAlive()) {
                    thread.join(); 
                }
                startButton.setDisable(false);
                endButton.setDisable(true);
                System.out.println("Server stopped by user.");
            } catch (InterruptedException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

}