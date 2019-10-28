package Connection;

import Controller.Message;
import Controller.RefreshController;
import Model.User;

import java.io.IOException;
import java.util.ArrayList;

public class Listener implements Runnable {
    @Override
    public void run() {
        while (true) {
            Signal response = null;
            try {
                response = (Signal) ServerHandler.getObjectInputStream().readObject();

                switch (response.getAction()) {
                    case UOL:
                        ArrayList<User> UserOnlineList = (ArrayList<User>) response.getData();

                        RefreshController.refreshUOL(UserOnlineList);
                        break;
                    case MESSAGE:
                        break;
                    default:
                        break;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
