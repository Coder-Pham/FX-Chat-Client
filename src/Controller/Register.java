package Controller;

import Connection.Action;
import Connection.ServerHandler;
import Connection.Signal;
import Model.StageView;
import Model.User;

import com.jfoenix.controls.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class Register implements Initializable {
    @FXML
    private JFXTextField username;
    @FXML
    private JFXPasswordField password;
    @FXML
    private JFXPasswordField passwordConfirm;
    @FXML
    private JFXTextField nickname;
    @FXML
    private JFXButton registerButton;
    @FXML
    private JFXButton loginButton;
    @FXML
    private Label error;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        Already init socket in Login scene
        error.setVisible(false);
    }

    private boolean throwError(String errorMessage) {
        error.setVisible(true);
        error.setText(errorMessage);
        return false;
    }

    private boolean validateRegister(String username, String password, String nickname) {
        String USER_PATTERN = "^[_A-Za-z0-9-]+";
        Pattern usernamePattern = Pattern.compile(USER_PATTERN);

        if (username == null || username.trim().equals(""))
            return throwError("Username should not be empty");
        else if (password == null || password.trim().equals(""))
            return throwError("Password should not be empty");
        else if (nickname == null || nickname.trim().equals(""))
            return throwError("Nick name should not be empty");
        else if (!usernamePattern.matcher(username).matches())
            return throwError("Invalid username");
        return true;
    }

    @FXML
    public void registerClick(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
//        TODO: Validate pattern of inputs
        if (password.getText().equals(passwordConfirm.getText())) {
            boolean valid = validateRegister(username.getText(), password.getText(), nickname.getText());
            if (!valid)
                return;
        } else {
            throwError("Wrong password confirm");
            return;
        }

//        TODO: Sent register data
        User registerUser = new User(0, username.getText(), password.getText(), nickname.getText());
        Signal registerRequest = new Signal(Action.REGISTER, true, registerUser, "");

        ServerHandler.getObjectOutputStream().writeObject(registerRequest);
        ServerHandler.getObjectOutputStream().flush();

        Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();

//      TODO: If successful register, then create new Stage - Scene for main
        if (response.isStatus()) {
            Login.currentUser = (User) response.getData();
            System.out.println("Register Successful");

            this.registerSuccess();
        }
//      TODO: If failed, THROW ERROR MESSAGE FROM SERVER
        else if (response.getAction().equals(Action.REGISTER)) {
            error.setVisible(true);
            error.setText(response.getError());
        }
    }

    @FXML
    public void loginClick(ActionEvent actionEvent) throws IOException {
//        TODO: switch scene to Login
        StageView.getStage().getScene().setRoot(FXMLLoader.load(getClass().getResource("../View/Login.fxml")));
    }

    private void registerSuccess() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Register Success");
        alert.setHeaderText("Now you can login with:");
        alert.setContentText("Username: \"" + Login.currentUser.getUsername() + "\" --- Nickname: \"" + Login.currentUser.getNickname() + "\"");
        alert.showAndWait();
        try {
            StageView.getStage().getScene().setRoot(FXMLLoader.load(getClass().getResource("../View/Login.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
