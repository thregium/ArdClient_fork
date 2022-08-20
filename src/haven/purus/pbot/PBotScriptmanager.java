package haven.purus.pbot;

import haven.UI;
import modification.configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PBotScriptmanager {
    public static final Map<String, PBotScript> scripts = new HashMap<>();

    public static PBotScript startScript(UI ui, File scriptFile) {
        String id = UUID.randomUUID().toString();
        String ext = scriptFile.getName().substring(scriptFile.getName().lastIndexOf("."));
        PBotScript script = (ext.equalsIgnoreCase(".py")) ? new PBotScriptPy(ui, scriptFile, id) : new PBotScriptJS(ui, scriptFile, id);
        scripts.put(id, script);
        script.start();
        configuration.classMaker(() -> ui.gui.PBotScriptlist.threadsUpdate());
        return (script);
    }

    public static PBotScript getScript(String id) {
        return scripts.get(id);
    }

    public static void closeScript(PBotScript script) {
        if (script != null) {
            for (Map.Entry<String, PBotScript> item : scripts.entrySet())
                if (item.getValue().equals(script)) {
                    scripts.remove(item.getKey());
                    break;
                }
            configuration.classMaker(() -> script.ui.gui.PBotScriptlist.removeFromList(script));
        }
    }
}
