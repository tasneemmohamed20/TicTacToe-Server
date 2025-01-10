/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;


/**
 *
 * @author ALANDALUS
 */
public class ServerUI extends Application {
    
    FXMLDocumentController server = new FXMLDocumentController();
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));
        
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest((event) -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Do You Want Close Server", ButtonType.YES,ButtonType.NO);
            alert.showAndWait().ifPresent(click->{
                if(click == ButtonType.NO)
                {
                    server.stop();
                }
                
            });
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
