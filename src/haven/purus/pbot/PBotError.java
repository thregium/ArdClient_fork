package haven.purus.pbot;

import haven.UI;
import modification.configuration;

import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class PBotError {

    // Writes error stacktrace to disk and notifies the user
    public static void handleException(UI ui, Exception e) {
        try {
            File folder = new File(configuration.pbotErrorPath);
            if (!folder.exists()) folder.mkdir();

            File output = new File(folder + "/" + "PBotError_" + System.currentTimeMillis() + ".txt");
            output.createNewFile();
            PBotUtils.debugMsg(ui, "PBot error occurred!! Writing it into a file called: " + output.getPath(), Color.RED);
            PrintWriter pw = new PrintWriter(Files.newBufferedWriter(output.toPath(), StandardOpenOption.WRITE));
            e.printStackTrace(pw);
            e.printStackTrace(ui.cons.out);
            pw.flush();
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
