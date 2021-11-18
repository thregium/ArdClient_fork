package haven.purus.pbot;

import haven.UI;

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
        //for heirs
    }

    public void kill() {
        interrupt();
    }

    public void execute(String... text) {
        //for heirs
    }

    public String name() {
        return name;
    }
}
