package Connection;

import Model.ServerIP;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public final class ServerHandler {
    private static Socket socket;
    private static ObjectOutputStream objectOutputStream;
    private static ObjectInputStream objectInputStream;

    public static void init() throws IOException {
        socket = new Socket(ServerIP.hostname, ServerIP.port);
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    public static Socket getSocket() {
        return socket;
    }

    public static void setSocket(Socket socket) {
        ServerHandler.socket = socket;
    }

    public static ObjectOutputStream getObjectOutputStream() {
        return objectOutputStream;
    }

    public static void setObjectOutputStream(ObjectOutputStream objectOutputStream) {
        ServerHandler.objectOutputStream = objectOutputStream;
    }

    public static ObjectInputStream getObjectInputStream() {
        return objectInputStream;
    }

    public static void setObjectInputStream(ObjectInputStream objectInputStream) {
        ServerHandler.objectInputStream = objectInputStream;
    }

    public static void close() throws IOException {
        if (objectInputStream != null)
            objectInputStream.close();

        if (objectOutputStream != null)
            objectOutputStream.close();

        socket.close();
    }
}
