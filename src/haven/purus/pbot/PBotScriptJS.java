package haven.purus.pbot;

import haven.UI;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.File;

public class PBotScriptJS extends PBotScript {
    private Context context;

    public PBotScriptJS(UI ui, File scriptFile, String id) {
        super(ui, scriptFile, id);
        context = Context.newBuilder("js").allowAllAccess(true).build();
        context.eval("js", "const ScriptID = '" + id + "';");
    }

    @Override
    public void run() {
        super.run();
        try {
            context.eval(Source.newBuilder("js", scriptFile).build());
        } catch (Exception e) {
            PBotError.handleException(ui, e);
        }
    }

    public Context getContext() {
        return (context);
    }

    @Override
    public void kill() {
        try {
            super.kill();
            context.close(true);
        } catch (Exception e) {
            PBotError.handleException(ui, e);
        }
    }

    @Override
    public void execute(String... text) {
        try {
            context.eval("js", String.join("", text));
        } catch (Exception e) {
            PBotError.handleException(ui, e);
        }
    }
}
