package Controller;

import Connection.Action;
import Connection.ServerHandler;
import Connection.Signal;

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
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

public class Message implements Initializable {
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
    @FXML
    private ImageView emoji;

    private Desktop desktop = Desktop.getDesktop();

    private Thread serverListener;
    private AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private User currentFriend = new User(-1, "", "", "");
    private HashMap<String, Boolean> UserNotification = new HashMap<String, Boolean>();

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

                                        System.out.println(userOnlineList.toString());

                                        refreshUserList(filterUser(userOnlineList.getUsers()));
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

        textMessage.setOnKeyPressed((event) -> {
            if(event.getCode() == KeyCode.ENTER) {
                event.consume(); // otherwise a new line will be added to the textArea after the sendFunction() call
                if (event.isShiftDown()) {
                    textMessage.appendText(System.getProperty("line.separator"));
                } else {
                    String text = textMessage.getText();

                    if (text.trim().isEmpty())
                        return;
                    else if(text.equals("::Smiling"))
                        emoji.setImage(new Image("Resources/Images/happy.png"));

                    System.out.println("Message sent: " + text);

//                    TODO: Send message to Server - Write down CSV
//                    NOTE: When click to friend, already check to CSV
                    MessageModel messageModel = new MessageModel(Login.currentUser, this.currentFriend, text);
                    Signal request = new Signal(Action.MESSAGE, true, messageModel, "");
                    try {
                        appendHistoryMessage(messageModel);
                        refreshMessage(messageModel);
                        ServerHandler.getObjectOutputStream().writeObject(request);
                        ServerHandler.getObjectOutputStream().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                this.UserNotification.put(sender.getUsername(), true);
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
                this.UserNotification.put(currentFriend.getUsername(), false);
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
                    fileInfo = new FileInfo(Login.currentUser, currentFriend, "", 0, new byte[]{});

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
                fileInfo.setSender(Login.currentUser);
                fileInfo.setReceiver(currentFriend);
                Signal request = new Signal(Action.FILE, true, fileInfo, "");
                try {
                    ServerHandler.getObjectOutputStream().writeObject(request);
                    ServerHandler.getObjectOutputStream().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                TODO: Alert file sent
                MessageModel messageModel = new MessageModel(Login.currentUser, this.currentFriend, "INCOMING FILE: " + fileSend.getName());
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
            if (UOLList.get(i).getId() == Login.currentUser.getId()) {
                UOLList.remove(i);
            }
            else
                i++;
        return UOLList;
    }

    private void refreshUserList(ArrayList<User> lst) {
//        TODO: Refresh online users list
        InputStream inputIcon = getClass().getResourceAsStream("/Resources/Images/Online.png");
        Image image = new Image(inputIcon);
        this.dynamicUserOnlineList.getChildren().clear();

        for (int i = 0; i < lst.size(); i++) {
            ImageView showIcon = new ImageView(image);
            showIcon.setFitHeight(10);
            showIcon.setFitWidth(10);
            JFXButton user = new JFXButton(lst.get(i).getUsername(), showIcon);

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

            if (this.UserNotification.containsKey(lst.get(i).getUsername()) && this.UserNotification.get(lst.get(i).getUsername()))
                user.setStyle("-fx-background-color: #8186d5; ");
            this.dynamicUserOnlineList.getChildren().add(i, user);
        }
    }

    private String getCurrentDir() {
        URL jarLocationUrl = Message.class.getProtectionDomain().getCodeSource().getLocation();
        String jarLocation = new File(jarLocationUrl.toString()).getParent();
        return jarLocation.substring(6);
    }

    private boolean checkHistory(User user) throws UnsupportedEncodingException {
        String CSV_FILE_PATH = String.format("%d-%d-message.csv", Login.currentUser.getId(), user.getId());
        File history = new File(getCurrentDir() + "/Resources/History/" + CSV_FILE_PATH);
        history.getParentFile().mkdirs();
        return history.exists();
    }

    private void connectFriend(User user) throws IOException {
        chatArea.setDisable(false);
        friendNickname.setText(user.getNickname());
        this.currentFriend = user;
        messageContainer.getChildren().clear();
        clearNotification();

        if (checkHistory(this.currentFriend))
            loadHistoryMessage();
        else
            createHistoryMessage(this.currentFriend);

        if (checkHistoryFile(this.currentFriend))
            loadHistoryFile();
        else
            createHistoryFile();
    }

    private void createHistoryMessage(User user) throws IOException {
        String CSV_FILE_PATH = String.format("%d-%d-message.csv", Login.currentUser.getId(), user.getId());
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



    private void refreshMessage(MessageModel msg) {
//        TODO: Push message to scene
        String text = msg.getContent();

        TextFlow container = new TextFlow();

        String[] splitText = text.split(" ");

        for(int i = 0; i < splitText.length; i++) {
            Text txt = new Text(splitText[i]);
            ImageView icon = new ImageView();
            icon.setFitHeight(15);
            icon.setFitWidth(15);
            if (splitText[i].equals("::SMILE")) {
                emoji.setImage(new Image("Resources/Images/smile.png"));
                icon.setImage(new Image("Resources/Images/smile.png"));
            } else if (splitText[i].equals("::HAPPY")) {
                emoji.setImage(new Image("Resources/Images/happy.png"));
                icon.setImage(new Image("Resources/Images/happy.png"));
            } else if (splitText[i].equals("::UNHAPPY")) {
                emoji.setImage(new Image("Resources/Images/unhappy.png"));
                icon.setImage(new Image("Resources/Images/unhappy.png"));
            } else if (splitText[i].equals("::ANGRY")) {
                emoji.setImage(new Image("Resources/Images/angry.png"));
                icon.setImage(new Image("Resources/Images/angry.png"));
            } else if (splitText[i].equals("::LOVE")) {
                emoji.setImage(new Image("Resources/Images/love.png"));
                icon.setImage(new Image("Resources/Images/love.png"));
            } else {
                container.getChildren().add(txt);
                container.getChildren().add(new Text(" "));
                continue;
            }
            container.getChildren().add(icon);
            container.getChildren().add(new Text(" "));
        }

        container.setPadding(new Insets(5, 5, 5, 5));
        HBox containMessageButton = new HBox();
        containMessageButton.setMinWidth(this.messageContainer.getPrefWidth());
        if (Login.currentUser.getUsername().equals(msg.getSender().getUsername())) {
            container.setStyle("-fx-background-color: #4298FB; -fx-text-fill: white; -fx-max-width : 240px; -fx-background-radius: 8;");
            containMessageButton.getChildren().add(container);
            containMessageButton.setAlignment(Pos.BASELINE_LEFT);
            HBox.setMargin(container, new Insets(0, 0, 5, 3));
        } else {
            container.setStyle("-fx-background-color: #F1EFF0; -fx-text-fill: black; -fx-max-width : 240px; -fx-background-radius: 8;");
            containMessageButton.getChildren().add(container);
            containMessageButton.setAlignment(Pos.BASELINE_RIGHT);
            HBox.setMargin(container, new Insets(0, 3, 5, 0));
        }
        this.messageContainer.getChildren().add(containMessageButton);
        this.messageContainer.heightProperty().addListener(observable -> messageScrollArea.setVvalue(1D));
    }

    private void appendHistoryMessage(MessageModel msg) throws IOException {
        String CSV_FILE_PATH;
        if (msg.getSender().getId() == Login.currentUser.getId())
            CSV_FILE_PATH = String.format("%d-%d-message.csv", Login.currentUser.getId(), msg.getReceiver().getId());
        else
            CSV_FILE_PATH = String.format("%d-%d-message.csv", Login.currentUser.getId(), msg.getSender().getId());
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
        String CSV_FILE_PATH = String.format("%d-%d-message.csv", Login.currentUser.getId(), this.currentFriend.getId());
        ICsvListReader listReader = null;
        try {
            listReader = new CsvListReader(new FileReader(getCurrentDir() + "/Resources/History/" + CSV_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);

            listReader.getHeader(true);
            final CellProcessor[] processors = getProcessors();

            List<Object> messageList;
            while ((messageList = listReader.read(processors)) != null){
                if (messageList.get(0).equals(Login.currentUser.getUsername())) {
                    MessageModel messageModel = new MessageModel(Login.currentUser, this.currentFriend, messageList.get(1).toString());
                    refreshMessage(messageModel);
                } else if (messageList.get(0).equals(this.currentFriend.getUsername())) {
                    MessageModel messageModel = new MessageModel(this.currentFriend, Login.currentUser, messageList.get(1).toString());
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
        String RECEIVE_FILE_PATH = String.format("%d-%d-file.txt", Login.currentUser.getId(), user.getId());
        File history = new File(getCurrentDir() + "/Resources/History/" + RECEIVE_FILE_PATH);
        history.getParentFile().mkdirs();
        return history.exists();
    }

    private void createHistoryFile() throws IOException {
//        TODO: Create history file for this.currentFriend
        String RECEIVE_FILE_PATH = String.format("%d-%d-file.txt", Login.currentUser.getId(), currentFriend.getId());
        File history = new File(getCurrentDir() + "/Resources/History/" + RECEIVE_FILE_PATH);
        history.createNewFile();
    }

    private void loadHistoryFile() throws IOException {
//        TODO: Clear history file
        dynamicFileList.getChildren().clear();

//        TODO: Load downloaded file history for this.currentFriend
        String RECEIVE_FILE_PATH = String.format("%d-%d-file.txt", Login.currentUser.getId(), currentFriend.getId());
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
        String RECEIVE_FILE_PATH = String.format("%d-%d-file.txt", Login.currentUser.getId(), fileInfo.getSender().getId());
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(getCurrentDir() + "/Resources/History/" + RECEIVE_FILE_PATH, true));
        bufferedWriter.write(fileInfo.getFilename().concat("\n"));
        bufferedWriter.close();
    }
}
