package haven.res.ui.tt.q.qbuff;

import haven.CompImage;
import haven.Config;
import haven.Coord;
import haven.CustomQualityList;
import haven.ItemInfo;
import haven.Resource;
import haven.Tex;
import haven.Text;
import static haven.Text.num10Fnd;
import static haven.Text.num12boldFnd;
import haven.Utils;
import modification.configuration;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class QBuff extends ItemInfo.Tip {
    public final BufferedImage icon;
    public final String name;
    public final String origName;
    public double q;
    public Tex qtex, qwtex;
    public Color color, outline = Color.BLACK;

    public QBuff(Owner owner, BufferedImage icon, String name, double q) {
        super(owner);
        this.icon = icon;
        this.origName = name;
        this.name = Resource.getLocString(Resource.BUNDLE_LABEL, name);
        this.q = q;
        if (Config.qualitycolor) {
            if (configuration.customquality) {
                boolean custom = false;
                for (int i = 0; i < CustomQualityList.qualityList.size(); i++) {
                    if (CustomQualityList.qualityList.get(i).a) {
                        if (q <= CustomQualityList.qualityList.get(i).number) {
                            color = CustomQualityList.qualityList.get(i).color;
                            custom = true;
                            break;
                        }
                    }
                }
                if (!custom && configuration.morethanquility)
                    for (int i = CustomQualityList.qualityList.size(); i > 0; i--) {
                        if (CustomQualityList.qualityList.get(i - 1).a) {
                            if (q > CustomQualityList.qualityList.get(i - 1).number) {
                                color = new Color(configuration.morethancolor, true);
                                outline = new Color(configuration.morethancoloroutline, true);
                                custom = true;
                                break;
                            }
                        }
                    }
                if (!custom)
                    color = Color.WHITE;
            } else {
                if (q < 11) {
                    color = Color.white;
                } else if (q < Config.uncommonq) {
                    color = Config.uncommon;
                } else if (q < Config.rareq) {
                    color = Config.rare;
                } else if (q < Config.epicq) {
                    color = Config.epic;
                } else if (q < Config.legendaryq) {
                    color = Config.legendary;
                } else {
                    if (Config.insaneitem) {
                        //PBotUtils.sysMsg(ui, "What a nice item!");
                    }
                    color = Color.orange;
                    outline = Color.RED;
                }
            }
        } else {
            color = Color.white;
        }
        if (q != 0) {
            if (!Config.largeqfont) {
                qtex = Text.renderstroked(Utils.fmt1DecPlace(q), color, outline, num10Fnd).tex();
                qwtex = Text.renderstroked(Math.round(q) + "", color, outline, num10Fnd).tex();
            } else {
                qtex = Text.renderstroked(Utils.fmt1DecPlace(q), color, outline, num12boldFnd).tex();
                qwtex = Text.renderstroked(Math.round(q) + "", color, outline, num12boldFnd).tex();
            }
        }
    }

    public interface Modifier {
        void prepare(QList ql);
    }

    public abstract static class QList extends Tip {
        public final List<QBuff> ql = new ArrayList<>();
        public final List<Modifier> mods = new ArrayList<>();

        QList() {super(null);}

        void sort() {
            Collections.sort(ql, Comparator.comparing(a -> a.name));
            for (Modifier mod : mods)
                mod.prepare(this);
        }
    }

    public static class Table extends QList {
        public int order() {return (10);}

        public void layout(Layout l) {
            sort();
            CompImage tab = new CompImage();
            CompImage.Image[] ic = new CompImage.Image[ql.size()];
            CompImage.Image[] nm = new CompImage.Image[ql.size()];
            CompImage.Image[] qv = new CompImage.Image[ql.size()];
            int i = 0;
            for (QBuff q : ql) {
                ic[i] = CompImage.mk(q.icon);
                nm[i] = CompImage.mk(Text.render(q.name + ":").img);
                qv[i] = CompImage.mk(Text.render((((int) q.q) == q.q) ? String.format("%d", (int) q.q) : String.format("%.1f", q.q)).img);
                i++;
            }
            tab.table(Coord.z, new CompImage.Image[][]{ic, nm, qv}, new int[]{5, 15}, 0, new int[]{0, 0, 1});
            l.cmp.add(tab, new Coord(0, l.cmp.sz.y));
        }
    }

    public static final Layout.ID<Table> lid = () -> (new Table());

    public static class Summary extends QList {
        public int order() {return (10);}

        public void layout(Layout l) {
            sort();
            CompImage buf = new CompImage();
            for (int i = 0; i < ql.size(); i++) {
                QBuff q = ql.get(i);
                Text t = Text.render(String.format((i < ql.size() - 1) ? "%,d, " : "%,d", Math.round(q.q)));
                buf.add(q.icon, new Coord(buf.sz.x, Math.max(0, (t.sz().y - q.icon.getHeight()) / 2)));
                buf.add(t.img, new Coord(buf.sz.x, 0));
            }
            l.cmp.add(buf, new Coord(l.cmp.sz.x + 10, 0));
        }
    }

    public static final Layout.ID<Summary> sid = () -> (new Summary());

    public void prepare(Layout l) {
        l.intern(lid).ql.add(this);
    }

    public Tip shortvar() {
        return (new Tip(owner) {
            public void prepare(Layout l) {
                l.intern(sid).ql.add(QBuff.this);
            }
        });
    }
}
