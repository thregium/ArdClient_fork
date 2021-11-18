package haven.purus.pbot;

import haven.UI;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PBotScriptmanager {
    public static Map<String, PBotScript> scripts = new HashMap<>();
    public UI ui;

    public static void startScript(UI ui, File scriptFile) {
        String id = UUID.randomUUID().toString();
        String ext = scriptFile.getName().substring(scriptFile.getName().lastIndexOf("."));
        PBotScript script = (ext.equalsIgnoreCase(".py")) ? new PBotScriptPy(ui, scriptFile, id) : new PBotScriptJS(ui, scriptFile, id);
        scripts.put(id, script);
        script.start();
    }

    public static PBotScript getScript(String id) {
        return scripts.get(id);
    }
}
