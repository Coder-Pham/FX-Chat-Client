package Helper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReadPropertyHelper {
    public static String getProperty(String propertyName)
    {
        String propertyPath = "resources/config.properties";
        try
        {
            InputStream input = new FileInputStream(propertyPath);

            Properties properties = new Properties();

            // load a properties file
            properties.load(input);

            // get the property value and print it out
            return properties.getProperty(propertyName);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
