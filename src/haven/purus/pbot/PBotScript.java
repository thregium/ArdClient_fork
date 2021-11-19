package haven.purus.pbot;

import haven.UI;

import java.awt.Color;
import java.io.File;

public class PBotScript extends Thread {
    protected File scriptFile;
    protected String name;
    protected String id;
    public final UI ui;

    public PBotScript(UI ui, File scriptFile, String id) {
        this.scriptFile = scriptFile;
        this.name = scriptFile.getName();
        this.id = id;
        this.ui = ui;
    }

    @Override
    public void run() {
        PBotUtils.debugMsg(ui, "Starting script: " + name, Color.ORANGE);
    }

    public void kill() {
        interrupt();
        PBotScriptmanager.closeScript(this);
    }

    public void execute(String... text) {
        //for heirs
    }

    public String name() {
        return name;
    }
}
