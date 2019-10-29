package Controller;

import Connection.Action;
import Connection.ServerHandler;
import Connection.Signal;

import Model.StageView;
import Model.User;
import Model.UserOnlineList;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class Message implements Initializable {
    @FXML
    private VBox dynamicUserOnlineList;
    @FXML
    private Label userNickName;
    @FXML
    private Label friendNickName;
    @FXML
    private JFXTextArea textMessage;

    private Thread serverListener;
    private AtomicBoolean shuttingDown = new AtomicBoolean(false);

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
        serverListener = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!shuttingDown.get()) {
                    try {
                        Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                switch (response.getAction()) {
                                    case UOL:
                                        UserOnlineList userOnlineList = (UserOnlineList) response.getData();
                                        for (int i = 0; i < userOnlineList.getUsers().size(); i++)
                                            System.out.println(userOnlineList.getUsers().get(i).getNickname());

                                        refreshUserList(userOnlineList.getUsers());
                                        break;
                                    case MESSAGE:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                    } catch (IOException | ClassNotFoundException e) {
//                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        serverListener.start();

        textMessage.setOnKeyPressed((event) -> {
            if(event.getCode() == KeyCode.ENTER) {
                event.consume(); // otherwise a new line will be added to the textArea after the sendFunction() call
                if (event.isShiftDown()) {
                    textMessage.appendText(System.getProperty("line.separator"));
                } else {
                    String text = textMessage.getText();
                    System.out.println(text);
                    textMessage.setText("");
                }
            }
        });
    }

    @FXML
    public void logoutClick(ActionEvent actionEvent) throws IOException {
//        TODO: Send Logout signal
        Signal logoutRequest = new Signal(Action.LOGOUT, true, Login.currentUser, "");
        ServerHandler.getObjectOutputStream().writeObject(logoutRequest);
        ServerHandler.getObjectOutputStream().flush();

//        TODO: Force stop Listener thread
        serverListener.interrupt();
        shuttingDown.set(true);
//        Platform.exit();

//        TODO: Switch back to login scene
        try {
            ServerHandler.init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FXMLLoader messageLoader = new FXMLLoader(getClass().getResource("../View/Login.fxml"));
        StageView.getStage().setScene(new Scene(messageLoader.load(), 600, 444));
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
