package haven.res.gfx.hud.rosters.pig;

import haven.Resource;
import haven.UI;
import haven.res.ui.croster.CattleRoster;
import haven.res.ui.croster.Column;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.TypeButton;
import java.util.Comparator;
import java.util.List;

public class PigRoster extends CattleRoster<Pig> {
    public static List<Column> cols = initcols(
            new Column<>("Name", Comparator.comparing((Entry e) -> e.name), 200),

            new Column<>(Resource.local().load("gfx/hud/rosters/sex"), Comparator.comparing((Pig e) -> e.hog).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/growth"), Comparator.comparing((Pig e) -> e.piglet).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/deadp"), Comparator.comparing((Pig e) -> e.dead).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/pregnant"), Comparator.comparing((Pig e) -> e.pregnant).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/lactate"), Comparator.comparing((Pig e) -> e.lactate).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/owned"), Comparator.comparing((Pig e) -> ((e.owned ? 1 : 0) | (e.mine ? 2 : 0))).reversed(), 20),

            new Column<>(Resource.local().load("gfx/hud/rosters/quality"), Comparator.comparing((Pig e) -> e.q).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/trufflesnout"), Comparator.comparing((Pig e) -> e.prc).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/meatquantity"), Comparator.comparing((Pig e) -> e.meat).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/milkquantity"), Comparator.comparing((Pig e) -> e.milk).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/meatquality"), Comparator.comparing((Pig e) -> e.meatq).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/meatquality"), Comparator.comparing((Pig e) -> e.tmeatq).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/milkquality"), Comparator.comparing((Pig e) -> e.milkq).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/milkquality"), Comparator.comparing((Pig e) -> e.tmilkq).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/hidequality"), Comparator.comparing((Pig e) -> e.hideq).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/hidequality"), Comparator.comparing((Pig e) -> e.thideq).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/breedingquality"), Comparator.comparing((Pig e) -> e.seedq).reversed())
    );

    protected List<Column> cols() {
        return (cols);
    }

    public static CattleRoster mkwidget(UI ui, Object... args) {
        return (new PigRoster());
    }

    public Pig parse(Object... args) {
        int n = 0;
        long id = (Long) args[n++];
        String name = (String) args[n++];
        Pig ret = new Pig(id, name);
        ret.grp = (Integer) args[n++];
        int fl = (Integer) args[n++];
        ret.hog = (fl & 1) != 0;
        ret.piglet = (fl & 2) != 0;
        ret.dead = (fl & 4) != 0;
        ret.pregnant = (fl & 8) != 0;
        ret.lactate = (fl & 16) != 0;
        ret.owned = (fl & 32) != 0;
        ret.mine = (fl & 64) != 0;
        ret.q = ((Number) args[n++]).doubleValue();
        ret.meat = (Integer) args[n++];
        ret.milk = (Integer) args[n++];
        ret.meatq = (Integer) args[n++];
        ret.milkq = (Integer) args[n++];
        ret.hideq = (Integer) args[n++];
        ret.tmeatq = ret.meatq * ret.q / 100;
        ret.tmilkq = ret.milkq * ret.q / 100;
        ret.thideq = ret.hideq * ret.q / 100;
        ret.seedq = (Integer) args[n++];
        ret.prc = (Integer) args[n++];
        return (ret);
    }

    public TypeButton button() {
        return (typebtn(Resource.local().load("gfx/hud/rosters/btn-pig"),
                Resource.local().load("gfx/hud/rosters/btn-pig-d")));
    }
}
