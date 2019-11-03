package Helper;

import Model.FileInfo;
import Model.User;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FileHistoryHelper {
    private static String directory = "history/file/";
    private static String path = FileHistoryHelper.directory.concat("%d-%d-file.csv");

    private static CellProcessor[] getProcessors() {
        final CellProcessor[] processors = new CellProcessor[] {
                new NotNull(), // File name
                new NotNull(), // Path
        };
        return processors;
    }

    public static void writeFileHistory(User user, User friend, FileInfo fileInfo)
    {
        String CSV_FILE_PATH = String.format(FileHistoryHelper.path, user.getId(), friend.getId());

        if(Files.exists(Paths.get(CSV_FILE_PATH)))
        {
            FileHistoryHelper.appendFileHistory(user, friend, fileInfo);
        }
        else
        {
            System.out.println("History file not found. Creating new history file");
            FileHistoryHelper.newFileHistory(user, friend);
            FileHistoryHelper.appendFileHistory(user, friend, fileInfo);
        }

    }

    public static HashMap<String, String> readFileHistory(User user, User friend)
    {
        String CSV_FILE_PATH = String.format(FileHistoryHelper.path, user.getId(), friend.getId());
        ICsvListReader listReader = null;
        HashMap<String, String> resultFileList = new HashMap<>();

        if(Files.exists(Paths.get(CSV_FILE_PATH)))
        {
            try
            {
                try {
                    listReader = new CsvListReader(new FileReader(CSV_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);

                    listReader.getHeader(true);
                    final CellProcessor[] processors = getProcessors();

                    List<Object> fileList;
                    while ((fileList = listReader.read(processors)) != null){
                        resultFileList.put((String) fileList.get(0), (String) fileList.get(1));
                    }
                }
                finally {
                    if (listReader != null){
                        listReader.close();
                    }
                }
            }
            catch (IOException exception)
            {
                exception.printStackTrace();
            }
        }
        else
        {
            System.out.println("History file not found");
        }
        return resultFileList;
    }

    private static void newFileHistory(User user, User friend)
    {
        File directory = new File(FileHistoryHelper.directory);
        if (!directory.exists())
        {
            directory.mkdirs();
        }

        String CSV_FILE_PATH = String.format(FileHistoryHelper.path, user.getId(), friend.getId());
        ICsvListWriter listWriter = null;

        try
        {
            try {
                listWriter = new CsvListWriter(new FileWriter(CSV_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);

                final CellProcessor[] processors = getProcessors();
                final String[] header = new String[]{"Filename", "AbsolutePath"};

                // TODO: write the header
                listWriter.writeHeader(header);
            } finally {
                if (listWriter != null) {
                    listWriter.close();
                }
            }
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        }
    }

    private static void appendFileHistory(User user, User friend, FileInfo fileInfo)
    {
        String CSV_FILE_PATH = String.format(FileHistoryHelper.path, user.getId(), friend.getId());

        ICsvListWriter listWriter = null;
        try
        {
            try {
                listWriter = new CsvListWriter(new FileWriter(CSV_FILE_PATH, true), CsvPreference.STANDARD_PREFERENCE);

                final CellProcessor[] processors = getProcessors();
                final String[] header = new String[]{"User", "Message"};

//            TODO: write message
                listWriter.write(Arrays.asList(fileInfo.getFilename(), FileDownloadHelper.absolutePath.concat(fileInfo.getFilename())), processors);
            } finally {
                if (listWriter != null) {
                    listWriter.close();
                }
            }
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        }
    }

}
