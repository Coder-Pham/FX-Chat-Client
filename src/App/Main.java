package App;

import Connection.Action;
import Connection.ServerHandler;
import Connection.Signal;
import Controller.Login;
import Model.StageView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    public static Stage firstStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            ServerHandler.init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Parent root = FXMLLoader.load(getClass().getResource("/View/Login.fxml"));
        Scene scene = new Scene(root, 600, 500);

        primaryStage.setTitle("FX Chat");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

        new StageView(primaryStage);

        primaryStage.setOnCloseRequest( event ->
        {
            Signal request = new Signal(Action.LOGOUT, true, Login.currentUser, "");
            try {
                ServerHandler.getObjectOutputStream().writeObject(request);
                ServerHandler.getObjectOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }
}
