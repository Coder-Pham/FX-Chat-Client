package Controller;

import Connection.Action;
import Connection.Listener;
import Connection.ServerHandler;
import Connection.Signal;

import Model.StageView;
import Model.User;
import Model.UserOnlineList;
import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Message implements Initializable {
    @FXML
    private VBox dynamicUserOnlineList;
    @FXML
    private Label userNickName;
    @FXML
    private Label friendNickName;

    private Listener listener;
    private Thread serverListener;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
//        TODO: Setup user information
        userNickName.setText(Login.currentUser.getNickname());
//        TODO: Request UOL
        Signal UOLRequest = new Signal(Action.UOL, true, new User(-1, "", "", ""), "");
        try {
            ServerHandler.getObjectOutputStream().writeObject(UOLRequest);
            ServerHandler.getObjectOutputStream().flush();

            Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();
            if (response.getAction().equals(Action.UOL) && response.isStatus()) {
                UserOnlineList userOnlineList = (UserOnlineList) response.getData();
                this.refreshUserList(userOnlineList.getUsers());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

//        TODO: Start subThread for Listener
        this.listener = new Listener();
        this.serverListener = new Thread(listener);
        this.serverListener.start();
    }

    @FXML
    public void logoutClick(ActionEvent actionEvent) throws IOException {
//        TODO: Send Logout signal
        Signal logoutRequest = new Signal(Action.LOGOUT, true, Login.currentUser, "");
        ServerHandler.getObjectOutputStream().writeObject(logoutRequest);
        ServerHandler.getObjectOutputStream().flush();

//        TODO: Force stop Listener thread
        this.listener.stop();

//        TODO: Switch back to login scene
        StageView.setSize(444, 600);
        StageView.getStage().getScene().setRoot(FXMLLoader.load(getClass().getResource("../View/Login.fxml")));
    }

    public void refreshUserList(ArrayList<User> lst) {
//        TODO: Refresh online users list
        InputStream inputIcon = getClass().getResourceAsStream("../Resources/Images/Online.png");
        Image image = new Image(inputIcon);
        this.dynamicUserOnlineList.getChildren().clear();

        for (int i = 0; i < lst.size(); i++) {
            ImageView showIcon = new ImageView(image);
            showIcon.setFitHeight(10);
            showIcon.setFitWidth(10);
            JFXButton user = new JFXButton(lst.get(i).getNickname(), showIcon);
            user.setContentDisplay(ContentDisplay.RIGHT);
            user.setMinWidth(this.dynamicUserOnlineList.getPrefWidth());
            user.setAlignment(Pos.BASELINE_RIGHT);
            this.dynamicUserOnlineList.getChildren().add(i, user);
        }
    }
}
