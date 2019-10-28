package Model;

import java.io.Serializable;
import java.util.ArrayList;

public class UserOnlineList extends Model {
    private ArrayList<User> users;

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }
}