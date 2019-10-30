package Model;

public class FileInfo extends Model {
    private User sender;
    private User receiver;
    private String filename;
    private long fileSize;
    private byte[] dataBytes;

    public FileInfo(User sender, User receiver, String filename, long fileSize, byte[] dataBytes) {
        this.sender = sender;
        this.receiver = receiver;
        this.filename = filename;
        this.fileSize = fileSize;
        this.dataBytes = dataBytes;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getFilename() {
        return this.filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getDataBytes() {
        return this.dataBytes;
    }

    public void setDataBytes(byte[] dataBytes) {
        this.dataBytes = dataBytes;
    }
}