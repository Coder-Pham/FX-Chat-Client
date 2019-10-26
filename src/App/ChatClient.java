package App;

import Model.StageView;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatClient extends Application {
    public static Stage firstStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("../View/Login.fxml"));
        Scene scene = new Scene(root, 600, 444);

        scene.getStylesheets().add(getClass().getResource("../Resources/Styles/login-register.css").toExternalForm());

        primaryStage.setTitle("FX Chat");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        new StageView(primaryStage);

        primaryStage.show();
    }
}
