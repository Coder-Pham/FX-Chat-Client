package Controller;

import Model.Friend;

import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

    private ArrayList<Friend> lst;

    @FXML
    private Label userNickName;

    @FXML
    private Label friendNickName;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userNickName.setText("Thien Huynh");
        friendNickName.setText("Trong Nhan");
    }

    @FXML
    public void refreshClick(ActionEvent actionEvent) throws IOException {
//        TODO: Refresh users list manually
        // Test
        lst = new ArrayList<Friend>();
        this.lst.add(new Friend("A", "Thien Huynh"));
        this.lst.add(new Friend("C", "Trong Nhan"));
        this.lst.add(new Friend("E", "Quan Tran"));
        this.lst.add(new Friend("G", "Duong Huynh"));
        InputStream inputIcon = getClass().getResourceAsStream("../Resources/Images/Online.png");
        Image image = new Image(inputIcon);
        this.dynamicUserOnlineList.getChildren().clear();
        for (int i = 0; i < this.lst.size(); i++) {
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
