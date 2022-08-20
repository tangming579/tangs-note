package com.tm.gm.common.gm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;

/**
 * @author: tangming
 * @date: 2022-08-20
 */
public class PropertiesTool {
    private static Properties prop = null;
    private static final String path = "src/test.properties";

    private static void init() {
        prop = new Properties();
        InputStream is = null;
        try {
            is = PropertiesTool.class.getClassLoader().getResourceAsStream("test.properties");
            prop.load(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String getProperty(String key) {

        if (prop == null) {
            init();
        }
        return prop.getProperty(key);
    }

    public static boolean setProperties(String key, String value) {

        if (prop == null) {
            init();
        }

        Writer writer = null;
        try {
            writer = new FileWriter(path);
            prop.setProperty(key, value);
            prop.store(writer, key);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
