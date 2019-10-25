package Connection;

import Model.Model;

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

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
