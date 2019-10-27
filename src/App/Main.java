package App;

import Connection.ServerHandler;
import Model.StageView;

import javafx.application.Application;
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

        Parent root = FXMLLoader.load(getClass().getResource("../View/Message.fxml"));
        // Scene scene = new Scene(root, 600, 444);
        Scene scene = new Scene(root, 1000, 844);

        primaryStage.setTitle("FX Chat");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        new StageView(primaryStage);

        primaryStage.show();
    }
}
