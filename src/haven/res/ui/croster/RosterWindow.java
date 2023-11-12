package haven.res.ui.croster;

import haven.Coord;
import haven.Glob;
import haven.UI;
import haven.Widget;
import haven.Window;
import haven.sloth.gui.ResizableWnd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RosterWindow extends ResizableWnd {
    public static final Map<Glob, RosterWindow> rosters = new HashMap<>();
    public static int rmseq = 0;
    public int btny = 0;
    public List<CattleRoster> crosters = new ArrayList<>();
    public List<TypeButton> buttons = new ArrayList<>();

    RosterWindow() {
        super(Coord.z, "Cattle Roster", true);
    }

    public void show(CattleRoster rost) {
        for (CattleRoster ch : children(CattleRoster.class))
            ch.show(ch == rost);
    }

    public void addroster(CattleRoster rost) {
        if (btny == 0)
            btny = rost.sz.y + UI.scale(10);
        crosters.add(add(rost, Coord.z));
        TypeButton btn = this.add(rost.button());
        btn.action(() -> show(rost));
        buttons.add(btn);
        buttons.sort(Comparator.comparingInt(a -> a.order));
        int x = 0;
        for (Widget wdg : buttons) {
            wdg.move(new Coord(x, btny));
            x += wdg.sz.x + UI.scale(10);
        }
        buttons.get(0).click();
        pack();
        rmseq++;
    }

    @Override
    public void resize(final Coord sz) {
        if (this.sz.equals(sz)) return;
        super.resize(sz);
        int szy = 0;
        for (Widget wdg : buttons) {
            szy = Math.max(wdg.sz.y, szy);
        }
        for (Widget wdg : crosters) {
            wdg.resize(asz.sub(0, szy + UI.scale(10)));
            btny = wdg.sz.y + UI.scale(10);
        }
        int x = 0;
        for (Widget wdg : buttons) {
            wdg.move(new Coord(x, btny));
            x += wdg.sz.x + UI.scale(10);
        }
//        pack();
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && msg.equals("close")) {
            this.hide();
            return;
        }
        super.wdgmsg(sender, msg, args);
    }
}
