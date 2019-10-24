package Controller;

import Connection.Signal;
import Model.User;
import Model.ServerIP;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
    private JFXButton loginButton;
    @FXML
    private Label error;

    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private Signal response;

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
        User loginUser = new User(username.getText(), password.getText());
        Signal loginRequest = new Signal("login", true, loginUser, "");

        Socket socket = new Socket(ServerIP.hostname, ServerIP.port);
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());

        objectOutputStream.writeObject(loginRequest);
        objectOutputStream.flush();
        socket.close();

//      TODO: Retrieve data from server

        response = (Signal) objectInputStream.readObject();
        socket.close();

//      TODO: If successful login, then create new Stage - Scene for main
        if (response.isStatus()) {
            System.out.println("Login Successful");
        }
//      TODO: If failed, reset scene
        else {
            resetScene();
        }
    }

    @FXML
    public void registerClick(ActionEvent actionEvent) {
    }
}
