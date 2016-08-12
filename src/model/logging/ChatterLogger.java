package model.logging;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ChatterLogger {
    private static final Calendar time = Calendar.getInstance();
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("[dd-MM<>hh:mm:ss]");
    private static final Logger appLogger = Logger.getLogger("Chatter Status Logger");

    static{
        try{
            FileHandler fileHandler = new FileHandler("logs\\chatter-log.log", 1024 * 15, 1);
            fileHandler.setFormatter(new SimpleFormatter());

            appLogger.addHandler(fileHandler);
            appLogger.setUseParentHandlers(false);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public static synchronized void log(Level logLevel, String message){
        String errorTime = timeFormat.format(time.getTime());
        appLogger.log(logLevel, errorTime + " <-> " + message);
    }
}
