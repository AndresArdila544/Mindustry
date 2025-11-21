package mindustry.server.setupItems;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.net.Administration;
import mindustry.server.ServerControl;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static arc.util.ColorCodes.*;
import static arc.util.ColorCodes.reset;
import static arc.util.Log.*;
import static arc.util.Log.removeColors;
import static mindustry.Vars.maxDeltaServer;

public class LoggingSystemSetup extends SetupItem
{

    protected DateTimeFormatter dateTime;
    protected String[] tags;
    private PrintWriter socketOutput;
    /** The file to which the logs are currently being written. */
    public Fi currentLogFile;
    public final Fi logFolder = Core.settings.getDataDirectory().child("logs/");

    public LoggingSystemSetup() {
        dateTime = ServerControl.getDateTime();
        tags = ServerControl.getTags();
    }

    @Override
    public void initiateEvents() {

    }

    @Override
    public void setupSteps() {

        //update log level
        Administration.Config.debug.set(Administration.Config.debug.bool());

        logger = (level1, text) -> {
            //err has red text instead of reset.
            if(level1 == Log.LogLevel.err) text = text.replace(reset, lightRed + bold);

            String result = bold + lightBlack + "[" + dateTime.format(LocalDateTime.now()) + "] " + reset + format(tags[level1.ordinal()] + " " + text + "&fr");
            System.out.println(result);

            if(Administration.Config.logging.bool()){
                logToFile("[" + dateTime.format(LocalDateTime.now()) + "] " + formatColors(tags[level1.ordinal()] + " " + text + "&fr", false));
            }

            if(socketOutput != null){
                try{
                    socketOutput.println(formatColors(text + "&fr", false));
                }catch(Throwable e1){
                    err("Error occurred logging to socket: @", e1.getClass().getSimpleName());
                }
            }
        };

        formatter = (text, useColors, arg) -> {
            text = Strings.format(text.replace("@", "&fb&lb@&fr"), arg);
            return useColors ? addColors(text) : removeColors(text);
        };

        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60f, maxDeltaServer));
    }


    public void logToFile(String text){
        if(currentLogFile != null && currentLogFile.length() > Administration.Config.maxLogLength.num()){
            currentLogFile.writeString("[End of log file. Date: " + dateTime.format(LocalDateTime.now()) + "]\n", true);
            currentLogFile = null;
        }

        for(String value : values){
            text = text.replace(value, "");
        }

        if(currentLogFile == null){
            int i = 0;
            while(logFolder.child("log-" + i + ".txt").length() >= Administration.Config.maxLogLength.num()){
                i++;
            }

            currentLogFile = logFolder.child("log-" + i + ".txt");
        }

        currentLogFile.writeString(text + "\n", true);
    }


    public void setSocketOutput(PrintWriter socketOutput) {
        this.socketOutput = socketOutput;
    }
}
