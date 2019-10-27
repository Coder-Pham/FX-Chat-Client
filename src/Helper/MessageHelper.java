package Helper;

import Model.StageView;
import Model.Friend;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.util.ArrayList;

public class MessageHelper {
    @FXML
    private VBox dynamicUserOnlineList;

    public MessageHelper() {
    }

    public VBox refreshList(ArrayList<Friend> lst){
        InputStream inputIcon = getClass().getResourceAsStream("../Resources/Images/Online.png");
        Image image = new Image(inputIcon);
        ImageView showIcon = new ImageView(image);
        this.dynamicUserOnlineList = new VBox();
        // for(int i = 0; i < lst.size();i++)
        this.dynamicUserOnlineList.getChildren().add(new JFXButton("lst.get(i).getNickname()"));
        return this.dynamicUserOnlineList;
    }

}
