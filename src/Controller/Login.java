package Controller;

import Connection.Request;
import Model.User;
import Model.ServerIP;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
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
    private JFXButton loginButton;
    @FXML
    private Label error;

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
    private void loginClick() throws IOException, ClassNotFoundException {
//      TODO: Establish connection
        User loginUser = new User(username.getText(), password.getText());
        Request loginRequest = new Request("login", loginUser);

        Socket socket = new Socket(ServerIP.hostname, Integer.parseInt(ServerIP.port));
        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

        objectOutputStream.writeObject(loginRequest);

//      TODO: Retrieve data from server
        Request response = (Request) objectInputStream.readObject();

//      TODO: If successful login, then create new Stage - Scene for main
        if (response.getAction().equals("success")) {

        }
//      TODO: If failed, reset scene
        else {
            resetScene();
        }
    }
}
