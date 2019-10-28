package Connection;

import Model.Model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Signal extends Model {
    private Action action;
    private boolean status;
    private Object data;
    private String error;

    public Signal(Action action, boolean status, Object data, String error) {
        this.action = action;
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static Signal getRequest(ObjectInputStream objectInputStream) {
        try {
            return (Signal) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            System.out.println("Signal-getRequest(): " + exception);
            return null;
        }
    }

    public static boolean sendResponse(Signal response, ObjectOutputStream objectOutputStream) {
        try {
            objectOutputStream.writeObject(response);
            return true;
        } catch (IOException exception) {
            System.out.println("Signal-sendResponse(): " + exception);
            return false;
        }
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}