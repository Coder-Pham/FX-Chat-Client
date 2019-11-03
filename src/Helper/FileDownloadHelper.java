package Helper;

import Model.FileInfo;

import java.io.*;

public class FileDownloadHelper {
    private static String directory = "download/";
    public static String absolutePath = System.getProperty("user.dir").concat(directory);

    public static void storeFile(FileInfo fileInfo)
    {
        BufferedOutputStream bufferedOutputStream = null;

//        TODO: Check Download folder exist
        File directory = new File(FileDownloadHelper.directory);
        if (!directory.exists())
        {
            directory.mkdirs();
        }

        //        TODO: Download file
        try {
            if (fileInfo != null) {
                File fileReceive = new File(FileDownloadHelper.directory.concat(fileInfo.getFilename()));
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileReceive));
                bufferedOutputStream.write(fileInfo.getDataBytes());
                bufferedOutputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileDownloadHelper.closeStream(bufferedOutputStream);
        }
    }

    public static void closeStream(InputStream inputStream) {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void closeStream(OutputStream outputStream) {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
