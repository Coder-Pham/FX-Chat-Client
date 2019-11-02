package Connection;

import Controller.MessageController;
import Model.Client;
import Model.FileInfo;
import Model.MessageModel;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientListener implements Runnable {

    private ServerSocket serverSocket;
    private static MessageController messageController;

    public ClientListener(int port, MessageController messageController)
    {
        System.out.println("Thread ClientListener start");
        try {
            this.serverSocket = new ServerSocket(port);
            ClientListener.messageController = messageController;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true)
        {
            try {
                Socket socket = this.serverSocket.accept();

                System.out.println("New client just connect to ClientListener");
                Client client = new Client(socket,new ObjectOutputStream(socket.getOutputStream()),new ObjectInputStream(socket.getInputStream()));

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Signal request = Signal.getRequest(client.getObjectInputStream());
                        if(request != null)
                        {
                            switch (request.getAction()) {
                                case MESSAGE:
                                    handleMessageRequest((MessageModel) request.getData());
                                    break;
                                case FILE:
                                    handleFileRequest((FileInfo) request.getData());
                                    break;
                                default:
                                    System.out.println("A client call to unknown function !!");
                                    break;
                            }
                        }
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessageRequest(MessageModel messageModel)
    {
        ClientListener.messageController.refreshMessage(messageModel);
    }

    private void handleFileRequest(FileInfo fileInfo)
    {

    }
}
