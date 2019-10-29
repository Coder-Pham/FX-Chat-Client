package Connection;

import Controller.RefreshController;
import Model.UserOnlineList;

import java.io.IOException;
import java.io.ObjectInputStream;

public class Listener implements Runnable {
    private boolean isRunning = true;

    public synchronized void stop() {
        this.isRunning = false;
    }

    private synchronized boolean Running() {
        return this.isRunning;
    }

    @Override
    public void run() {
        while (Running()) {
            try {
                Signal response = (Signal) ServerHandler.getObjectInputStream().readObject();

                switch (response.getAction()) {
                    case UOL:
                        UserOnlineList userOnlineList = (UserOnlineList) response.getData();
//                        for (int i = 0; i < userOnlineList.getUsers().size(); i++)
//                            System.out.println(userOnlineList.getUsers().get(i).getNickname());

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
