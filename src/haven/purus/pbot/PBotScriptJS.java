package haven.purus.pbot;

import haven.UI;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class PBotScriptJS extends PBotScript {
    private Context context;

    public PBotScriptJS(UI ui, File scriptFile, String id) {
        super(ui, scriptFile, id);
    }

    @Override
    public void run() {
        PBotUtils.sysMsg(ui, "Starting script: " + name, Color.ORANGE);
        context = Context.newBuilder("js").allowAllAccess(true).build();
        try {
            context.eval("js", "const ScriptID = '" + id + "';");
            context.eval(Source.newBuilder("js", scriptFile).build());
        } catch (Exception e) {
            PBotError.handleException(ui, e);
        }
    }

    @Override
    public void kill() {
        try {
            interrupt();
            context.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(String... text) {
        context.eval("js", String.join("", text));
    }
}
