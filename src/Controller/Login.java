package Controller;

import Connection.Action;
import Connection.ServerHandler;
import Connection.Signal;
import Model.User;
import Model.ServerIP;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Login implements Initializable {
    @FXML
    private JFXTextField username;
    @FXML
    private JFXPasswordField password;
    @FXML
    private JFXTextField nickname;
    @FXML
    private JFXButton loginButton;
    @FXML
    private Label error;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        error.setVisible(false);
        username.clear();
        password.clear();
        try {
            ServerHandler.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            System.out.println("Login Successful");
        }
//      TODO: If failed, reset scene
        else if (response.getAction().equals(Action.LOGIN)) {
            resetScene();
            System.out.println(response.getError());
        }
    }

    @FXML
    public void registerClick(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
        User registerUser = new User(0, username.getText(), password.getText(), nickname.getText());
        Signal registerRequest = new Signal(Action.REGISTER, true, registerUser, "");

        ServerHandler.getObjectOutputStream().writeObject(registerRequest);
        ServerHandler.getObjectOutputStream().flush();

        Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();
    }
}
