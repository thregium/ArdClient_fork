package haven.purus.pbot;

import haven.UI;

import java.awt.Color;
import java.io.File;
import java.util.regex.Matcher;

public class PBotScriptPy extends PBotScript {
    public PBotScriptPy(UI ui, File scriptFile, String id) {
        super(ui, scriptFile, id);
    }

    @Override
    public void run() {
        PBotUtils.sysMsg(ui, "Starting script: " + name, Color.ORANGE);
        try {
            String scriptname = scriptFile.getPath().substring("scripts/py/".length()).replaceAll(Matcher.quoteReplacement("\\"), ".").replace("/", ".").replace(".py", "");
            ((Py4j.PBotScriptLoader) Py4j.server.getPythonServerEntryPoint(new Class[]{Py4j.PBotScriptLoader.class})).start(scriptname, "run", id);
        } catch (Exception e) {
            e.printStackTrace();
            PBotError.handleException(ui, e);
        }
    }

    @Override
    public void kill() {
        try {
            interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(String... args) {
        try {
            String scriptname = scriptFile.getPath().substring("scripts/py/".length()).replaceAll(Matcher.quoteReplacement("\\"), ".").replace("/", ".").replace(".py", "");
            ((Py4j.PBotScriptLoader) Py4j.server.getPythonServerEntryPoint(new Class[]{Py4j.PBotScriptLoader.class})).start(scriptname, args[0], id);
        } catch (Exception e) {
            e.printStackTrace();
            PBotError.handleException(ui, e);
        }
    }
}
