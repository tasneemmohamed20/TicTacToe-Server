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
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import db.DAO;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series; // Import Series from XYChart
import javafx.scene.control.Button;
import javafx.util.Duration;
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
    Series<String, Integer> onlinePlayers = new Series<>(); 
    Series<String, Integer> offlinePlayers = new Series<>(); 
    Series<String, Integer> inGamePlayers = new Series<>();
    private Thread chartUpdaterThread;

    @FXML
    private Button startButton;
    @FXML
    private Button endButton;

    @FXML
    private BarChart<String, Integer> barChart;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private CategoryAxis xAxis;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        onlinePlayers.setName("Online");
        offlinePlayers.setName("Offline");
        inGamePlayers.setName("In Game");
    }

    @FXML
    private void handleStartButton(ActionEvent event) {
    try {
        if (server == null) {
            server = new Server();
        }
        isRunning = true;
        updateChart();
        thread = new Thread(() -> {
            server.startServer();
        });
        thread.setDaemon(true);
        thread.start();

        chartUpdaterThread = new Thread(() -> {
            System.out.println("chartUpdaterThread is Alive");
            while (isRunning) {
                Platform.runLater(this::updateChart);
                try {
//                    System.out.println("i'm here");
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        chartUpdaterThread.setDaemon(true);
        chartUpdaterThread.start();        
        
        System.out.println("Server Started");
        startButton.setDisable(true);
        endButton.setDisable(false);

    } catch (Exception ex) {
        ex.printStackTrace();
    }
}

    private void updateChart() {
//        System.out.println("Updating chart...");

        Series<String, Integer> online = new Series<>();
        Series<String, Integer> offline = new Series<>();
        Series<String, Integer> inGame = new Series<>();
        online.setName("Online");
        offline.setName("Offline");
        inGame.setName("In Game");

        updateChartData(online, offline, inGame);

        ObservableList<XYChart.Series<String, Integer>> newData = FXCollections.observableArrayList();
        newData.addAll(online, offline, inGame);
        barChart.setData(newData);
    }
    
    private void updateChartData(Series<String, Integer> onlinePlayers, Series<String, Integer> offlinePlayers, Series<String, Integer> inGamePlayers) {
        try {
            Vector<String> onlineUsers = DAO.getAllInlineUsers();
            Vector<String> allUsers = DAO.getAllUsers();
            Vector<String> inGameUsers = DAO.getInGameUsers();
            
            int onlineCount = onlineUsers.size();
            int inGameCount = inGameUsers.size();
            int offlineCount = allUsers.size() - (onlineCount + inGameCount);
    
            onlinePlayers.getData().add(new XYChart.Data<>("Users", onlineCount));
            offlinePlayers.getData().add(new XYChart.Data<>("Users", offlineCount));
            inGamePlayers.getData().add(new XYChart.Data<>("Users", inGameCount));
        } catch (SQLException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleEndButton(ActionEvent event) {
        stop();
    }

    public synchronized void stop() {
        try {
            isRunning = false;

            if (server != null) {
                server.stopServer();
                server = null;
            }
            
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
            
            if (chartUpdaterThread != null) {
                chartUpdaterThread.interrupt();
                chartUpdaterThread = null;
            }

            barChart.getData().clear();
            Platform.runLater(() -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), barChart);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                onlinePlayers = null;
                offlinePlayers = null;
                inGamePlayers = null;
                barChart.setOpacity(1.0);
            });
            fadeOut.play();                
        });
                
            System.out.println("Server Stopped");
            startButton.setDisable(false);
            endButton.setDisable(true);

        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
}