package Model;

import java.util.ArrayList;

public class UserAddressList extends Model{
    private ArrayList<UserAddress> userAddresses;

    public ArrayList<UserAddress> getUserAddresses() {
        return userAddresses;
    }

    public void setUserAddresses(ArrayList<UserAddress> userAddresses) {
        this.userAddresses = userAddresses;
    }
}
