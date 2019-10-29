package Connection;

import Controller.RefreshController;
import Model.UserOnlineList;

import java.io.IOException;

public class Listener implements Runnable {
    @Override
    public void run() {
        while (true) {
            Signal response = null;
            try {
                response = (Signal) ServerHandler.getObjectInputStream().readObject();

                switch (response.getAction()) {
                    case UOL:
                        UserOnlineList userOnlineList = (UserOnlineList) response.getData();

                        RefreshController.refreshUOL(userOnlineList.getUsers());
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
