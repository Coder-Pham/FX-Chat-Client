package Controller;

import Connection.Action;
import Connection.ServerHandler;
import Connection.Signal;

import Model.MessageModel;
import Model.StageView;
import Model.User;
import Model.UserOnlineList;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

public class Message implements Initializable {
    @FXML
    private VBox dynamicUserOnlineList;
    @FXML
    private Label userNickName;
    @FXML
    private Label friendNickName;
    @FXML
    private JFXTextArea textMessage;
    @FXML
    private VBox messageContainter;
    @FXML
    private HBox chatArea;

    private Thread serverListener;
    private AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private static CellProcessor[] getProcessors() {

        final CellProcessor[] processors = new CellProcessor[] {
                new NotNull(), // Nickname
                new NotNull(), // Message
        };

        return processors;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
//        TODO: Setup user information
        chatArea.setDisable(true);
        userNickName.setText(Login.currentUser.getNickname());
//        TODO: Request UOL
        Signal UOLRequest = new Signal(Action.UOL, true, new User(-1, "", "", ""), "");
        try {
            ServerHandler.getObjectOutputStream().writeObject(UOLRequest);
            ServerHandler.getObjectOutputStream().flush();

            Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();
            if (response.getAction().equals(Action.UOL) && response.isStatus()) {
                UserOnlineList userOnlineList = (UserOnlineList) response.getData();
                this.refreshUserList(filterUser(userOnlineList.getUsers()));
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

                                        refreshUserList(filterUser(userOnlineList.getUsers()));
                                        break;
                                    case MESSAGE:
                                        MessageModel message = (MessageModel) response.getData();

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

//        try {
//            this.loadHistoryMessage(Login.currentUser);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

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

//        TODO: Switch back to login scene
        try {
            ServerHandler.init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FXMLLoader messageLoader = new FXMLLoader(getClass().getResource("../View/Login.fxml"));
        StageView.getStage().setScene(new Scene(messageLoader.load(), 600, 444));
    }

    private ArrayList<User> filterUser(ArrayList<User> UOLList) {
        System.out.println(UOLList);
        int i = 0;
        while (i < UOLList.size())
            if (UOLList.get(i).getId() == Login.currentUser.getId()) {
                UOLList.remove(i);
            }
            else
                i++;
        return UOLList;
    }

    private void refreshUserList(ArrayList<User> lst) {
//        TODO: Refresh online users list
        InputStream inputIcon = getClass().getResourceAsStream("../Resources/Images/Online.png");
        Image image = new Image(inputIcon);
        this.dynamicUserOnlineList.getChildren().clear();

        for (int i = 0; i < lst.size(); i++) {
            ImageView showIcon = new ImageView(image);
            showIcon.setFitHeight(10);
            showIcon.setFitWidth(10);
            JFXButton user = new JFXButton(lst.get(i).getNickname(), showIcon);
            int finalI = i;
            user.setOnAction(e -> connectFriend(lst.get(finalI)));
            user.setContentDisplay(ContentDisplay.RIGHT);
            user.setMinWidth(this.dynamicUserOnlineList.getPrefWidth());
            user.setAlignment(Pos.BASELINE_RIGHT);
            this.dynamicUserOnlineList.getChildren().add(i, user);
        }
    }

    private void connectFriend(User user){
        friendNickName.setText(user.getNickname());
        System.out.println(user);
    }

    private void refreshMessage(MessageModel msg) {
//        TODO: Write message
        JFXButton container = new JFXButton(msg.getContent());
        container.setContentDisplay(ContentDisplay.CENTER);
        if (Login.currentUser.getNickname() == msg.getSender().getNickname()){
            container.setStyle("-fx-background-color: #4298FB;");
            container.setAlignment(Pos.BASELINE_LEFT);
        }
        else{
            container.setStyle("-fx-background-color: #F1EFF0;");
            container.setAlignment(Pos.BASELINE_RIGHT);
        }
        this.messageContainter.getChildren().add(container);
    }

    private void appendSenderToCSV(MessageModel msg) throws IOException {

    }

    @SuppressWarnings("resource")
    @Deprecated
    private void loadHistoryMessage(User friend) throws IOException {
        String SAMPLE_CSV_FILE_PATH = "1-2-message.csv";
        ICsvListReader listReader = null;
        try {
            listReader = new CsvListReader(new FileReader("./out/production/FXChat-Client/Resources/History/" + SAMPLE_CSV_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);

            listReader.getHeader(true);
            final CellProcessor[] processors = getProcessors();

            List<Object> messageList;
            while ((messageList = listReader.read(processors)) != null){
                JFXButton container = new JFXButton(messageList.get(1).toString());
                container.setContentDisplay(ContentDisplay.CENTER);
                container.setAlignment(Pos.BASELINE_CENTER);
                HBox containMessageButton = new HBox();
                containMessageButton.setMinWidth(this.messageContainter.getPrefWidth());
                if (Login.currentUser.getNickname().equals(messageList.get(0).toString())){
                    container.setStyle("-fx-background-color: #4298FB; -fx-text-fill: white; -fx-max-width : 240px");
                    container.setWrapText(true);
                    containMessageButton.getChildren().add(container);
                    containMessageButton.setAlignment(Pos.BASELINE_LEFT);
                }
                else{
                    container.setStyle("-fx-background-color: #F1EFF0; -fx-text-fill: black; -fx-max-width : 240px");
                    container.setWrapText(true);
                    containMessageButton.getChildren().add(container);
                    containMessageButton.setAlignment(Pos.BASELINE_RIGHT);
                }
                this.messageContainter.getChildren().add(containMessageButton);
            }
        } finally {
            if (listReader != null){
                listReader.close();
            }
        }
    }

}
