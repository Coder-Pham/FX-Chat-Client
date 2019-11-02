package Model;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    public Client()
    {

    }

    public Client(Socket socket, ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
        this.socket = socket;
        this.objectOutputStream = objectOutputStream;
        this.objectInputStream = objectInputStream;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ObjectOutputStream getObjectOutputStream() {
        return objectOutputStream;
    }

    public void setObjectOutputStream(ObjectOutputStream objectOutputStream) {
        this.objectOutputStream = objectOutputStream;
    }

    public ObjectInputStream getObjectInputStream() {
        return objectInputStream;
    }

    public void setObjectInputStream(ObjectInputStream objectInputStream) {
        this.objectInputStream = objectInputStream;
    }
}