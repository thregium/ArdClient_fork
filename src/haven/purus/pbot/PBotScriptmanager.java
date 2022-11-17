package haven.purus.pbot;

import haven.UI;
import haven.Widget;
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
        configuration.classMaker(() -> {
            PBotScriptlist list = ui.root.findchild(PBotScriptlist.class);
            if (list == null) list = ui.root.add(new PBotScriptlist());
            list.threadsUpdate();
        });
        return (script);
    }

    public static PBotScript prepareScript(UI ui, File scriptFile) {
        String id = UUID.randomUUID().toString();
        String ext = scriptFile.getName().substring(scriptFile.getName().lastIndexOf("."));
        PBotScript script = (ext.equalsIgnoreCase(".py")) ? new PBotScriptPy(ui, scriptFile, id) : new PBotScriptJS(ui, scriptFile, id);
        return (script);
    }

    public static void runScript(PBotScript script) {
        scripts.put(script.getScriptId(), script);
        script.start();
        for (UI ui : PBotAPI.uis()) {
            if (ui.gui == null) continue;
            ui.gui.PBotScriptlist.threadsUpdate();
        }
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
            for (UI ui : PBotAPI.uis()) {
                PBotScriptlist list = ui.root.findchild(PBotScriptlist.class);
                if (list != null) list.removeFromList(script);
            }
        }
    }
}
