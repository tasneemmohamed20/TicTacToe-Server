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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series; // Import Series from XYChart
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
    Series<String, Integer> onlinePlayers = new Series<>(); // Use Series instead of XYSeries
    Series<String, Integer> offlinePlayers = new Series<>(); // Use Series instead of XYSeries
    private Thread chartUpdaterThread;
    private final Object chartLock = new Object();

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
        if (barChart != null) {
            barChart.getStylesheets().add(getClass().getResource("chart.css").toExternalForm());
        }

        // Initialize series before applying styles
//        onlinePlayers = new Series<>();
//        offlinePlayers = new Series<>();

        // Set names
        onlinePlayers.setName("Online");
        offlinePlayers.setName("Offline");

        // Apply styles only after adding to chart
        Platform.runLater(() -> {
            if (onlinePlayers.getNode() != null) {
                onlinePlayers.getNode().getStyleClass().add("onlinePlayers");
            }
            if (offlinePlayers.getNode() != null) {
                offlinePlayers.getNode().getStyleClass().add("offlinePlayers");
            }
        });
    }

    @FXML
    private void handleStartButton(ActionEvent event) {
        
        try {
            if (server == null) {
                server = new Server();
            }
            updateChart();
            thread = new Thread(() -> {
                server.startServer();
                Platform.runLater(() -> {
                    chartUpdaterThread = new Thread(() -> {
                        while (isRunning) {
                            Platform.runLater(this::updateChart);
                            try {
                                Thread.sleep(5000); 
                            } catch (InterruptedException ex) {
                               
                                break;
                            }
                        }
                    });
                    chartUpdaterThread.setDaemon(true);
                    chartUpdaterThread.start();
                });
            });
            thread.start();

            isRunning = true;
            System.out.println("Server Started");
            startButton.setDisable(true);
            endButton.setDisable(false);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateChart() {
        if (onlinePlayers == null || offlinePlayers == null) {
            onlinePlayers = new Series<>();
            offlinePlayers = new Series<>();
        }

        System.out.println("Updating chart...");

        barChart.getData().clear();

        onlinePlayers.getData().clear();
        offlinePlayers.getData().clear();

        updateChartData(onlinePlayers, offlinePlayers);

        barChart.getData().addAll(onlinePlayers, offlinePlayers);
    }

    private void updateChartData(Series<String, Integer> onlinePlayers, Series<String, Integer> offlinePlayers) {
        try {
            onlinePlayers.getData().clear();
            offlinePlayers.getData().clear();

            Vector<String> onlineUsers = DAO.getAllInlineUsers();
            Vector<String> allUsers = DAO.getAllUsers(); 

            int onlineCount = onlineUsers.size();
            int offlineCount = allUsers.size() - onlineCount;

            onlinePlayers.getData().add(new XYChart.Data<>("Users", onlineCount));
            offlinePlayers.getData().add(new XYChart.Data<>("Users", offlineCount));
        } catch (SQLException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handleEndButton(ActionEvent event) {
        stop();
    }

    public void stop() {
        try {
            isRunning = false;

            if (chartUpdaterThread != null) {
                chartUpdaterThread.interrupt();
                
            }

            if (server != null) {
                server.stopServer();
            }

            if (thread != null) {
                thread.interrupt();
            }
            barChart.getData().clear();

                onlinePlayers = null;
                offlinePlayers = null;
                
            System.out.println("Server Stopped");
            startButton.setDisable(false);
            endButton.setDisable(true);

        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
}