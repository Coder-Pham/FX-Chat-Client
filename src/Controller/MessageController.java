package Controller;

import Connection.*;

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
    private UserAddress currentFriendAddress = new UserAddress(new User(-1, "", "", ""),"127.0.0.1");

    //    P2P Section
    public static Thread fxServerThread;
    public static Thread fxClientThread;
    public static ServerSocket fxServer;
    public static Client currentClient;

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
        userNickName.setText(LoginController.currentUser.getNickname());
//        TODO: Request UOL
        Signal UOLRequest = new Signal(Action.UOL, true, new User(-1, "", "", ""), "");
        try {
            ServerHandler.getObjectOutputStream().writeObject(UOLRequest);
            ServerHandler.getObjectOutputStream().flush();

            Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();
            if (response.getAction().equals(Action.UOL) && response.isStatus()) {
                UserAddressList userAddressList = (UserAddressList) response.getData();
//                System.out.println("hello" + userAddressList.toString());

//                this.refreshUserList(filterUser(userAddressList.getUserAddresses());
                this.refreshUserList(this.filterUserAddress(userAddressList.getUserAddresses()));
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

//        TODO: Start subThread for Listener
        this.createServerListener();

//        this.createFXServer();
        ClientListener clientListener = new ClientListener(Integer.parseInt(Objects.requireNonNull(ReadPropertyHelper.getProperty("clientlistener_port"))),this);
        Thread thread = new Thread(clientListener);
        thread.start();

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
//                    Signal request = new Signal(Action.MESSAGE, true, messageModel, "");
                    try {
                        appendHistoryMessage(messageModel);
                        refreshMessage(messageModel);

                        ClientTalker.sendRequestTo(this.currentFriendAddress.getAddress(),
                                Integer.parseInt(Objects.requireNonNull(ReadPropertyHelper.getProperty("clientlistener_port"))),
                                Action.MESSAGE,
                                messageModel);
//                        this.talkTo(this.currentFriendAddress.getAddress(),messageModel);
//                        ServerHandler.getObjectOutputStream().writeObject(request);
//                        ServerHandler.getObjectOutputStream().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    textMessage.setText("");
                }
            }
        });
    }

    public void updateText(String string)
    {
        this.textMessage.setText(string);
    }

    public void createServerListener()
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
                                    case MESSAGE:
                                        MessageModel message = (MessageModel) response.getData();

//                                        TODO: Check for history to read - write
                                        try {
                                            if (!checkHistory(message.getSender())) {
                                                try {
                                                    createHistoryMessage(message.getSender());
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }

                                        try {
                                            if (message.getSender().getUsername().equals(currentFriend.getUsername())) {
                                                appendHistoryMessage(message);
                                                refreshMessage(message);
                                            } else if (!message.getSender().getUsername().equals(currentFriend.getUsername())) {
                                                appendHistoryMessage(message);
                                                notification(message.getSender());
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    case FILE:
                                        try {
                                            downFile((FileInfo) response.getData());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
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

    public void createFXServer()
    {
        System.out.println("Thread fxServer start");
        MessageController.fxServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MessageController.fxServer = new ServerSocket(1111);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (!shuttingDown.get()) {
                    try {
                        //Waiting for client socket connect to serversocket
                        Socket client = MessageController.fxServer.accept();

                        MessageController.currentClient = new Client(client,new ObjectOutputStream(client.getOutputStream()),new ObjectInputStream(client.getInputStream()));

//                        System.out.println("A client just connect to our server");
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                    Client client = MessageController.currentClient;
                                    //Read request object from client
                                    Signal request = Signal.getRequest(client.getObjectInputStream());
                                    if (request != null) {
                                        switch (request.getAction()) {
                                            case MESSAGE:
                                                MessageModel message = (MessageModel) request.getData();

//                                        TODO: Check for history to read - write
                                                try {
                                                    if (!checkHistory(message.getSender())) {
                                                        try {
                                                            createHistoryMessage(message.getSender());
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                } catch (UnsupportedEncodingException e) {
                                                    e.printStackTrace();
                                                }

                                                try {
                                                    if (message.getSender().getUsername().equals(currentFriend.getUsername())) {
                                                        appendHistoryMessage(message);
                                                        refreshMessage(message);
                                                    } else if (!message.getSender().getUsername().equals(currentFriend.getUsername())) {
                                                        appendHistoryMessage(message);
                                                        notification(message.getSender());
                                                    }
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                break;
                                            case FILE:
                                                try {
                                                    downFile((FileInfo) request.getData());
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                break;
                                            default:
                                                System.out.println("A client call to unknown function !!");
                                        }
                                    }
                            }
                        });
                    } catch (IOException e) {
//                        e.printStackTrace();
                        break;
                    }
                }
            }
        });

        MessageController.fxServerThread.start();
    }

    public void talkTo(String address, MessageModel messageModel)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try{
                    Socket socket = new Socket(address, 1111);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    Signal request = new Signal(Action.MESSAGE,true,messageModel,"");
                    Signal.sendResponse(request,objectOutputStream);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendFileTo(String address, FileInfo fileInfo)
    {

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

    private void notification(User sender) {
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

    private void closeStream(InputStream inputStream) {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void closeStream(OutputStream outputStream) {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
                    closeStream(bufferedInputStream);
                }

//                TODO: Add FileInfo 2-end users
                assert fileInfo != null;
                fileInfo.setSender(LoginController.currentUser);
                fileInfo.setReceiver(currentFriend);
                Signal request = new Signal(Action.FILE, true, fileInfo, "");
                try {
                    ServerHandler.getObjectOutputStream().writeObject(request);
                    ServerHandler.getObjectOutputStream().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                TODO: Alert file sent
                MessageModel messageModel = new MessageModel(LoginController.currentUser, this.currentFriend, "INCOMING FILE: " + fileSend.getName());
                request = new Signal(Action.MESSAGE, true, messageModel, "");
                try {
                    appendHistoryMessage(messageModel);
                    refreshMessage(messageModel);
                    ServerHandler.getObjectOutputStream().writeObject(request);
                    ServerHandler.getObjectOutputStream().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean downFile(FileInfo fileInfo) throws IOException {
        BufferedOutputStream bufferedOutputStream = null;

//        TODO: Check Download folder exist
        File directory = new File(getCurrentDir() + "/Resources/Download");
        if (!directory.exists())
            directory.mkdir();

//        TODO: Download file
        try {
            if (fileInfo != null) {
                File fileReceive = new File(getCurrentDir() + "/Resources/Download/".concat(fileInfo.getFilename()));
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileReceive));
                bufferedOutputStream.write(fileInfo.getDataBytes());
                bufferedOutputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeStream(bufferedOutputStream);
        }

//        TODO: Write filename to HISTORY FILE
        assert fileInfo != null;
        appendHistoryFile(fileInfo);
        if (currentFriend.getId() == fileInfo.getSender().getId())
            refreshFile(fileInfo.getFilename());

        return true;
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
            this.dynamicUserOnlineList.getChildren().add(i, user);
        }
    }

    private String getCurrentDir() {
        URL jarLocationUrl = MessageController.class.getProtectionDomain().getCodeSource().getLocation();
        String jarLocation = new File(jarLocationUrl.toString()).getParent();
        return jarLocation.substring(6);
    }

    private boolean checkHistory(User user) throws UnsupportedEncodingException {
        String CSV_FILE_PATH = String.format("%d-%d-message.csv", LoginController.currentUser.getId(), user.getId());
        File history = new File(getCurrentDir() + "/Resources/History/" + CSV_FILE_PATH);
        history.getParentFile().mkdirs();
        return history.exists();
    }

    private void connectFriend(UserAddress userAddress) throws IOException {
        chatArea.setDisable(false);
        User user = userAddress.getUser();
        friendNickname.setText(user.getNickname());
        this.currentFriend = user;
        this.currentFriendAddress = userAddress;
        messageContainer.getChildren().clear();
        clearNotification();


//        if (checkHistory(this.currentFriend))
//            loadHistoryMessage();
//        else
//            createHistoryMessage(this.currentFriend);
//
//        if (checkHistoryFile(this.currentFriend))
//            loadHistoryFile();
//        else
//            createHistoryFile();
    }

    private void createHistoryMessage(User user) throws IOException {
        String CSV_FILE_PATH = String.format("%d-%d-message.csv", LoginController.currentUser.getId(), user.getId());
        ICsvListWriter listWriter = null;
        try {
            listWriter = new CsvListWriter(new FileWriter(getCurrentDir() + "/Resources/History/" + CSV_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);

            final CellProcessor[] processors = getProcessors();
            final String[] header = new String[]{"User", "Message"};

            // TODO: write the header
            listWriter.writeHeader(header);
        } finally {
            if (listWriter != null) {
                listWriter.close();
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

    private void appendHistoryMessage(MessageModel msg) throws IOException {
        String CSV_FILE_PATH;
        if (msg.getSender().getId() == LoginController.currentUser.getId())
            CSV_FILE_PATH = String.format("%d-%d-message.csv", LoginController.currentUser.getId(), msg.getReceiver().getId());
        else
            CSV_FILE_PATH = String.format("%d-%d-message.csv", LoginController.currentUser.getId(), msg.getSender().getId());
        ICsvListWriter listWriter = null;
        try {
            listWriter = new CsvListWriter(new FileWriter(getCurrentDir() + "/Resources/History/" + CSV_FILE_PATH, true), CsvPreference.STANDARD_PREFERENCE);

            final CellProcessor[] processors = getProcessors();
            final String[] header = new String[]{"User", "Message"};

//            TODO: write message
            listWriter.write(Arrays.asList(msg.getSender().getUsername(), msg.getContent()), processors);
        } finally {
            if (listWriter != null) {
                listWriter.close();
            }
        }
    }

    @SuppressWarnings("resource")
    private void loadHistoryMessage() throws IOException {
        String CSV_FILE_PATH = String.format("%d-%d-message.csv", LoginController.currentUser.getId(), this.currentFriend.getId());
        ICsvListReader listReader = null;
        try {
            listReader = new CsvListReader(new FileReader(getCurrentDir() + "/Resources/History/" + CSV_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);

            listReader.getHeader(true);
            final CellProcessor[] processors = getProcessors();

            List<Object> messageList;
            while ((messageList = listReader.read(processors)) != null){
                if (messageList.get(0).equals(LoginController.currentUser.getUsername())) {
                    MessageModel messageModel = new MessageModel(LoginController.currentUser, this.currentFriend, messageList.get(1).toString());
                    refreshMessage(messageModel);
                } else if (messageList.get(0).equals(this.currentFriend.getUsername())) {
                    MessageModel messageModel = new MessageModel(this.currentFriend, LoginController.currentUser, messageList.get(1).toString());
                    refreshMessage(messageModel);
                }
            }
        } finally {
            if (listReader != null){
                listReader.close();
            }
        }
    }

    private boolean checkHistoryFile(User user) {
        String RECEIVE_FILE_PATH = String.format("%d-%d-file.txt", LoginController.currentUser.getId(), user.getId());
        File history = new File(getCurrentDir() + "/Resources/History/" + RECEIVE_FILE_PATH);
        history.getParentFile().mkdirs();
        return history.exists();
    }

    private void createHistoryFile() throws IOException {
//        TODO: Create history file for this.currentFriend
        String RECEIVE_FILE_PATH = String.format("%d-%d-file.txt", LoginController.currentUser.getId(), currentFriend.getId());
        File history = new File(getCurrentDir() + "/Resources/History/" + RECEIVE_FILE_PATH);
        history.createNewFile();
    }

    private void loadHistoryFile() throws IOException {
//        TODO: Clear history file
        dynamicFileList.getChildren().clear();

//        TODO: Load downloaded file history for this.currentFriend
        String RECEIVE_FILE_PATH = String.format("%d-%d-file.txt", LoginController.currentUser.getId(), currentFriend.getId());
        BufferedReader bufferedReader = new BufferedReader(new FileReader(getCurrentDir() + "/Resources/History/" + RECEIVE_FILE_PATH));
        String filename;
        while ((filename = bufferedReader.readLine()) != null) {
//        TODO: call refreshFile(filename) to render open file button
            refreshFile(filename);
        }
    }

    private void refreshFile(String filename) {
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
                desktop.open(new File(getCurrentDir() + "/Resources/Download/".concat(filename)));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        file.setContentDisplay(ContentDisplay.LEFT);
        file.setMinWidth(this.dynamicFileList.getPrefWidth());
        file.setAlignment(Pos.BASELINE_LEFT);
        this.dynamicFileList.getChildren().add(file);
    }

    private void appendHistoryFile(FileInfo fileInfo) throws IOException {
        String RECEIVE_FILE_PATH = String.format("%d-%d-file.txt", LoginController.currentUser.getId(), fileInfo.getSender().getId());
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(getCurrentDir() + "/Resources/History/" + RECEIVE_FILE_PATH, true));
        bufferedWriter.write(fileInfo.getFilename().concat("\n"));
        bufferedWriter.close();
    }
}
