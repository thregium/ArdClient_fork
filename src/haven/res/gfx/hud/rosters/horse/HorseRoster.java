package haven.res.gfx.hud.rosters.horse;

import haven.Resource;
import haven.UI;
import haven.res.ui.croster.CattleRoster;
import haven.res.ui.croster.Column;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.TypeButton;
import java.util.Comparator;
import java.util.List;

public class HorseRoster extends CattleRoster<Horse> {
    public static List<Column> cols = initcols(
            new Column<>("Name", Comparator.comparing((Entry e) -> e.name), 200),

            new Column<>(Resource.local().load("gfx/hud/rosters/sex"), Comparator.comparing((Horse e) -> e.stallion).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/growth"), Comparator.comparing((Horse e) -> e.foal).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/deadp"), Comparator.comparing((Horse e) -> e.dead).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/pregnant"), Comparator.comparing((Horse e) -> e.pregnant).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/lactate"), Comparator.comparing((Horse e) -> e.lactate).reversed(), 20).runon(),
            new Column<>(Resource.local().load("gfx/hud/rosters/owned"), Comparator.comparing((Horse e) -> ((e.owned ? 1 : 0) | (e.mine ? 2 : 0))).reversed(), 20),

            new Column<>(Resource.local().load("gfx/hud/rosters/quality"), Comparator.comparing((Horse e) -> e.q).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/endurance"), Comparator.comparing((Horse e) -> e.end).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/stamina"), Comparator.comparing((Horse e) -> e.stam).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/metabolism"), Comparator.comparing((Horse e) -> e.mb).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/meatquantity"), Comparator.comparing((Horse e) -> e.meat).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/milkquantity"), Comparator.comparing((Horse e) -> e.milk).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/meatquality"), Comparator.comparing((Horse e) -> e.meatq).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/meatquality"), Comparator.comparing((Horse e) -> e.tmeatq).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/milkquality"), Comparator.comparing((Horse e) -> e.milkq).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/milkquality"), Comparator.comparing((Horse e) -> e.tmilkq).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/hidequality"), Comparator.comparing((Horse e) -> e.hideq).reversed()),
            new Column<>(Resource.local().load("gfx/hud/rosters/hidequality"), Comparator.comparing((Horse e) -> e.thideq).reversed()),

            new Column<>(Resource.local().load("gfx/hud/rosters/breedingquality"), Comparator.comparing((Horse e) -> e.seedq).reversed())
    );

    protected List<Column> cols() {
        return (cols);
    }

    public static CattleRoster mkwidget(UI ui, Object... args) {
        return (new HorseRoster());
    }

    public Horse parse(Object... args) {
        int n = 0;
        long id = (Long) args[n++];
        String name = (String) args[n++];
        Horse ret = new Horse(id, name);
        ret.grp = (Integer) args[n++];
        int fl = (Integer) args[n++];
        ret.stallion = (fl & 1) != 0;
        ret.foal = (fl & 2) != 0;
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
        ret.seedq = (Integer) args[n++];
        ret.tmeatq = ret.meatq * ret.q / 100;
        ret.tmilkq = ret.milkq * ret.q / 100;
        ret.thideq = ret.hideq * ret.q / 100;
        ret.end = (Integer) args[n++];
        ret.stam = (Integer) args[n++];
        ret.mb = (Integer) args[n++];
        return (ret);
    }

    public TypeButton button() {
        return (typebtn(Resource.local().load("gfx/hud/rosters/btn-horse"),
                Resource.local().load("gfx/hud/rosters/btn-horse-d")));
    }
}
