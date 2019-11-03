package Controller;

import Connection.*;

import Helper.FileDownloadHelper;
import Helper.FileHistoryHelper;
import Helper.MessageHistoryHelper;
import Helper.ReadPropertyHelper;
import Model.*;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.stage.FileChooser;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

public class MessageController implements Initializable {
    @FXML
    private VBox dynamicUserOnlineList;
    @FXML
    private Label userNickName;
    @FXML
    private Label friendNickname;
    @FXML
    private JFXTextArea textMessage;
    @FXML
    private VBox messageContainer;
    @FXML
    private HBox chatArea;
    @FXML
    private ScrollPane messageScrollArea;
    @FXML
    private JFXButton fileIcon;
    @FXML
    private VBox dynamicFileList;

    private Desktop desktop = Desktop.getDesktop();

    private Thread serverListener;
    private AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private User currentFriend = new User(-1, "", "", "");

    //    P2P Section
    public UserAddress currentFriendAddress = new UserAddress(new User(-1, "", "", ""),"127.0.0.1");

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Render some custom components such as userOnlineList
        this.initialRender();

        // Attach the event key listener for textMessage
        this.initialSetAction();

        // Create threads for listening from server and other client
        this.initialCreateThread();
    }

    private void initialRender()
    {
        // Setup user information
        chatArea.setDisable(true);
        userNickName.setText(LoginController.currentUser.getNickname());

        // Request UOL
        this.requestAndLoadUOL();
    }

    private void initialSetAction()
    {
        // Set event (Enter key) listener for textMessage box
        this.setKeyActionForTextMessage();
    }

    private void initialCreateThread()
    {
        //Create thread for listening to server
        this.createServerListener();

        // Create thread for listening to user who would like to connect
        ClientListener clientListener = new ClientListener(Integer.parseInt(Objects.requireNonNull(ReadPropertyHelper.getProperty("clientlistener_port"))),this);
        Thread thread = new Thread(clientListener);
        thread.start();
    }

    private void requestAndLoadUOL()
    {
        Signal UOLRequest = new Signal(Action.UOL, true, new User(-1, "", "", ""), "");
        try {
            ServerHandler.getObjectOutputStream().writeObject(UOLRequest);
            ServerHandler.getObjectOutputStream().flush();

            Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();
            if (response.getAction().equals(Action.UOL) && response.isStatus()) {
                UserAddressList userAddressList = (UserAddressList) response.getData();
                this.refreshUserList(this.filterUserAddress(userAddressList.getUserAddresses()));
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setKeyActionForTextMessage()
    {
        textMessage.setOnKeyPressed((event) -> {
            if(event.getCode() == KeyCode.ENTER) {
                event.consume(); // otherwise a new line will be added to the textArea after the sendFunction() call
                if (event.isShiftDown()) {
                    textMessage.appendText(System.getProperty("line.separator"));
                } else {
                    String text = textMessage.getText();
                    if (text == null)
                        text = "\n";
                    System.out.println("Message sent: " + text);

//                    TODO: Send message to Server - Write down CSV
//                    NOTE: When click to friend, already check to CSV
                    MessageModel messageModel = new MessageModel(LoginController.currentUser, this.currentFriend, text);
                    refreshMessage(messageModel);
                    MessageHistoryHelper.writeMessageHistory(messageModel.getSender(), messageModel.getReceiver(), messageModel);

                    ClientTalker.sendRequestTo(this.currentFriendAddress.getAddress(),
                            Integer.parseInt(Objects.requireNonNull(ReadPropertyHelper.getProperty("clientlistener_port"))),
                            Action.MESSAGE,
                            messageModel);

                    textMessage.setText("");
                }
            }
        });
    }

    @FXML
    public void logoutClick(ActionEvent actionEvent) throws IOException {
//        TODO: Send Logout signal
        Signal logoutRequest = new Signal(Action.LOGOUT, true, LoginController.currentUser, "");
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

        FXMLLoader messageLoader = new FXMLLoader(getClass().getResource("/View/Login.fxml"));
        StageView.getStage().setScene(new Scene(messageLoader.load(), 600, 500));
    }

    @FXML
    public void upFile(MouseEvent actionEvent) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = fileChooser.showOpenDialog(StageView.getStage());
        if (file != null) {
            List<File> files = Arrays.asList(file);

//            TODO: From files.get(i).getAbsolutePath() -> Convert to byte -> Send FileInfo
            for (File fileSend : files) {
                BufferedInputStream bufferedInputStream = null;
                FileInfo fileInfo = null;
                try {
                    bufferedInputStream = new BufferedInputStream(new FileInputStream(fileSend));
                    fileInfo = new FileInfo(LoginController.currentUser, currentFriend, "", 0, new byte[]{});

//                TODO: Get File info
                    byte[] fileBytes = new byte[(int) fileSend.length()];
                    bufferedInputStream.read(fileBytes, 0, fileBytes.length);
                    fileInfo.setFilename(fileSend.getName());
                    fileInfo.setDataBytes(fileBytes);
                    fileInfo.setFileSize(fileSend.length());
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    FileDownloadHelper.closeStream(bufferedInputStream);
                }

//                TODO: Add FileInfo 2-end users
                assert fileInfo != null;
                fileInfo.setSender(LoginController.currentUser);
                fileInfo.setReceiver(currentFriend);

                ClientTalker.sendRequestTo(this.currentFriendAddress.getAddress(),
                        Integer.parseInt(Objects.requireNonNull(ReadPropertyHelper.getProperty("clientlistener_port"))),
                        Action.FILE,
                        fileInfo);

//                TODO: Alert file sent
                MessageModel messageModel = new MessageModel(LoginController.currentUser, this.currentFriend, "INCOMING FILE: " + fileSend.getName());
                ClientTalker.sendRequestTo(this.currentFriendAddress.getAddress(),
                        Integer.parseInt(Objects.requireNonNull(ReadPropertyHelper.getProperty("clientlistener_port"))),
                        Action.MESSAGE,
                        messageModel);

                this.refreshMessage(messageModel);
                MessageHistoryHelper.writeMessageHistory(LoginController.currentUser, this.currentFriend, messageModel);
            }
        }
    }

    public void refreshMessage(MessageModel msg) {
//        TODO: Push message to scene
        JFXButton container = new JFXButton(msg.getContent());
        container.setContentDisplay(ContentDisplay.CENTER);
        container.setAlignment(Pos.BASELINE_CENTER);
        HBox containMessageButton = new HBox();
        containMessageButton.setMinWidth(this.messageContainer.getPrefWidth());
        if (LoginController.currentUser.getUsername().equals(msg.getSender().getUsername())) {
            container.setStyle("-fx-background-color: #4298FB; -fx-text-fill: white; -fx-max-width : 240px");
            container.setWrapText(true);
            containMessageButton.getChildren().add(container);
            containMessageButton.setAlignment(Pos.BASELINE_LEFT);
            HBox.setMargin(container, new Insets(0, 0, 5, 3));
        } else {
            container.setStyle("-fx-background-color: #F1EFF0; -fx-text-fill: black; -fx-max-width : 240px");
            container.setWrapText(true);
            containMessageButton.getChildren().add(container);
            containMessageButton.setAlignment(Pos.BASELINE_RIGHT);
            HBox.setMargin(container, new Insets(0, 3, 5, 0));
        }
        this.messageContainer.getChildren().add(containMessageButton);
    }

    private void createServerListener()
    {
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
                                        UserAddressList userAddressList = (UserAddressList) response.getData();

                                        refreshUserList(filterUserAddress(userAddressList.getUserAddresses()));
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

    }

    public void notification(User sender) {
        for (int i = 0; i < dynamicUserOnlineList.getChildren().size(); i++) {
            JFXButton friend = (JFXButton) dynamicUserOnlineList.getChildren().get(i);
            if (friend.getText().equals(sender.getUsername())) {
                friend.setStyle("-fx-background-color: #8186d5; ");
                dynamicUserOnlineList.getChildren().remove(i);
                dynamicUserOnlineList.getChildren().add(0, friend);
                break;
            }
        }
    }

    private void clearNotification() {
        for (int i = 0; i < dynamicUserOnlineList.getChildren().size(); i++) {
            JFXButton friend = (JFXButton) dynamicUserOnlineList.getChildren().get(i);
            if (friend.getText().equals(currentFriend.getUsername())) {
                friend.setStyle("-fx-background-color: #FFFFFF; ");
                dynamicUserOnlineList.getChildren().remove(i);
                dynamicUserOnlineList.getChildren().add(i, friend);
                break;
            }
        }
    }

    private ArrayList<User> filterUser(ArrayList<User> UOLList) {
        System.out.println(UOLList);
        int i = 0;
        while (i < UOLList.size())
            if (UOLList.get(i).getId() == LoginController.currentUser.getId()) {
                UOLList.remove(i);
            }
            else
                i++;
        return UOLList;
    }

    private ArrayList<UserAddress> filterUserAddress(ArrayList<UserAddress> userAddressArrayList) {
//        System.out.println(userAddressArrayList);
        int i = 0;
        while (i < userAddressArrayList.size())
            if (userAddressArrayList.get(i).getUser().getId() == LoginController.currentUser.getId()) {
                userAddressArrayList.remove(i);
            }
            else
                i++;
        return userAddressArrayList;
    }

    private void connectFriend(UserAddress userAddress) throws IOException {
        chatArea.setDisable(false);
        User user = userAddress.getUser();
        friendNickname.setText(user.getNickname());
        this.currentFriend = user;
        this.currentFriendAddress = userAddress;
        messageContainer.getChildren().clear();
        clearNotification();

        ArrayList<MessageModel> messageModels = MessageHistoryHelper.readMessageHistory(LoginController.currentUser,this.currentFriend);
        if(messageModels.size() > 0)
        {
            for (int i = 0; i < messageModels.size(); i++)
            {
                this.refreshMessage(messageModels.get(i));
            }
        }

        HashMap<String, String> fileList = FileHistoryHelper.readFileHistory(LoginController.currentUser,this.currentFriend);
        if(fileList.size() > 0)
        {
            ArrayList<String> fileNameList = new ArrayList<>(fileList.keySet());
            for (int i = 0; i < fileNameList.size(); i++ )
            {
                this.refreshFile(fileNameList.get(i),fileList.get(fileNameList.get(i)));
            }
        }
    }

    private void refreshUserList(ArrayList<UserAddress> lst) {
//        TODO: Refresh online users list
        InputStream inputIcon = getClass().getResourceAsStream("/Resources/Images/Online.png");
        Image image = new Image(inputIcon);
        this.dynamicUserOnlineList.getChildren().clear();

        for (int i = 0; i < lst.size(); i++) {
            ImageView showIcon = new ImageView(image);
            showIcon.setFitHeight(10);
            showIcon.setFitWidth(10);
            JFXButton user = new JFXButton(lst.get(i).getUser().getUsername(), showIcon);

            int userID = i;
            user.setOnAction(e -> {
                try {
                    connectFriend(lst.get(userID));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            user.setContentDisplay(ContentDisplay.RIGHT);
            user.setMinWidth(this.dynamicUserOnlineList.getPrefWidth());
            user.setAlignment(Pos.BASELINE_RIGHT);
            user.getStyleClass().add("choose-friend");
            this.dynamicUserOnlineList.getChildren().add(i, user);
        }
    }

    public void refreshFile(String filename, String path) {
//        TODO: Render open file button in dynamicFileList
        InputStream inputIcon = getClass().getResourceAsStream("/Resources/Images/download.png");
        Image image = new Image(inputIcon);

        ImageView showIcon = new ImageView(image);
        showIcon.setFitHeight(20);
        showIcon.setFitWidth(20);
        JFXButton file = new JFXButton(filename, showIcon);

//        TODO: setOnAction for button to open file
        file.setOnAction(e -> {
            try {
                File filePath = new File(path);
                if (filePath.exists())
                {
                    desktop.open(new File(path));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        file.setContentDisplay(ContentDisplay.LEFT);
        file.setMinWidth(this.dynamicFileList.getPrefWidth());
        file.setAlignment(Pos.BASELINE_LEFT);
        file.getStyleClass().add("open-file");
        this.dynamicFileList.getChildren().add(file);
    }
}
