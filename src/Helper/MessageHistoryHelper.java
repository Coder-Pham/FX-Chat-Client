package Helper;

import Controller.LoginController;
import Model.MessageModel;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageHistoryHelper {
    private static String directory = "history/message/";
    private static String path = MessageHistoryHelper.directory + "%d-%d-message.csv";

    private static CellProcessor[] getProcessors() {
        final CellProcessor[] processors = new CellProcessor[] {
                new NotNull(), // Nickname
                new NotNull(), // Message
        };
        return processors;
    }

    public static void writeMessageHistory(User user, User friend, MessageModel messageModel)
    {
        String CSV_FILE_PATH = String.format(MessageHistoryHelper.path, user.getId(), friend.getId());

        if(Files.exists(Paths.get(CSV_FILE_PATH)))
        {
            MessageHistoryHelper.appendMessageHistory(user, friend, messageModel);
        }
        else
        {
            System.out.println("History file not found. Creating new history file");
            MessageHistoryHelper.newMessageHistory(user, friend);
            MessageHistoryHelper.appendMessageHistory(user, friend, messageModel);
        }

    }

    public static ArrayList<MessageModel> readMessageHistory(User user, User friend)
    {
        String CSV_FILE_PATH = String.format(MessageHistoryHelper.path, user.getId(), friend.getId());
        ICsvListReader listReader = null;
        ArrayList<MessageModel> messageModels = new ArrayList<MessageModel>();

        if(Files.exists(Paths.get(CSV_FILE_PATH)))
        {
            try
            {
                try {
                    listReader = new CsvListReader(new FileReader(CSV_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);

                    listReader.getHeader(true);
                    final CellProcessor[] processors = getProcessors();

                    List<Object> messageList;
                    while ((messageList = listReader.read(processors)) != null){
                        if (messageList.get(0).equals(user.getUsername())) {
                            MessageModel messageModel = new MessageModel(user, friend, messageList.get(1).toString());
                            messageModels.add(messageModel);
                        } else if (messageList.get(0).equals(friend.getUsername())) {
                            MessageModel messageModel = new MessageModel(friend, user, messageList.get(1).toString());
                            messageModels.add(messageModel);
                        }
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
        return messageModels;
    }

    private static void newMessageHistory(User user, User friend)
    {
        File directory = new File(MessageHistoryHelper.directory);
        if (!directory.exists())
        {
            directory.mkdirs();
        }

        String CSV_FILE_PATH = String.format(MessageHistoryHelper.path, user.getId(), friend.getId());
        ICsvListWriter listWriter = null;

        try
        {
            try {
                listWriter = new CsvListWriter(new FileWriter(CSV_FILE_PATH), CsvPreference.STANDARD_PREFERENCE);

                final CellProcessor[] processors = getProcessors();
                final String[] header = new String[]{"User", "Message"};

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

    private static void appendMessageHistory(User user, User friend, MessageModel messageModel)
    {
        String CSV_FILE_PATH = String.format(MessageHistoryHelper.path, user.getId(), friend.getId());

        ICsvListWriter listWriter = null;
        try
        {
            try {
                listWriter = new CsvListWriter(new FileWriter(CSV_FILE_PATH, true), CsvPreference.STANDARD_PREFERENCE);

                final CellProcessor[] processors = getProcessors();
                final String[] header = new String[]{"User", "Message"};

//            TODO: write message
                listWriter.write(Arrays.asList(messageModel.getSender().getUsername(), messageModel.getContent()), processors);
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
