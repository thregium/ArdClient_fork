/* Preprocessed source code */

import haven.UI;
import haven.Widget;

/* >wdg: InitWindow */
public class InitWindow {
    public static Widget mkwidget(UI ui, Object[] args) {
        if (args[0] instanceof Integer) {
            return (InitWindowV.mkwidget(ui, args));
        } else if (args[0] instanceof String) {
            return (InitWindowR.mkwidget(ui, args));
        }
        return (null);
    }
}
