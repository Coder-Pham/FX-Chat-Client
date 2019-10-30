package Controller;

import Connection.Action;
import Connection.ServerHandler;
import Connection.Signal;
import Model.StageView;
import Model.User;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Login implements Initializable {
    @FXML
    private JFXTextField username;
    @FXML
    private JFXPasswordField password;
    @FXML
    private JFXButton loginButton;
    @FXML
    private JFXButton registerButton;
    @FXML
    private Label error;
    public static User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        error.setVisible(false);
        username.clear();
        password.clear();
    }

    private void resetScene() {
        username.clear();
        password.clear();
        error.setVisible(true);
        error.setText("Username / Password is incorrect");
    }

    @FXML
    private void loginClick(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
//      TODO: Establish connection
        User loginUser = new User(0, username.getText(), password.getText(), "");
        Signal loginRequest = new Signal(Action.LOGIN, true, loginUser, "");

        ServerHandler.getObjectOutputStream().writeObject(loginRequest);
        ServerHandler.getObjectOutputStream().flush();

//      TODO: Retrieve data from server
        Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();

//      TODO: If successful login, then create new Stage - Scene for main
        if (response.isStatus()) {
            currentUser = (User) response.getData();
            System.out.println("Login Successful");

            FXMLLoader messageLoader = new FXMLLoader(getClass().getResource("../View/Message.fxml"));
            StageView.getStage().setScene(new Scene(messageLoader.load(), 1000, 844));
        }
//      TODO: If failed, reset scene
        else if (response.getAction().equals(Action.LOGIN)) {
            resetScene();
            System.out.println(response.getError());
        }
    }

    @FXML
    public void registerClick(ActionEvent actionEvent) throws IOException {
//        TODO: Switch Register scene
        StageView.getStage().getScene().setRoot(FXMLLoader.load(getClass().getResource("../View/Register.fxml")));
    }
}
