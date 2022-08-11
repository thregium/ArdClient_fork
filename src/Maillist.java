import haven.*;
import haven.res.ui.mailbox.*;
import java.util.*;

public class Maillist extends SSearchBox<Mail, Mailbox> {
    private List<Mail> list = Collections.emptyList();
    private boolean loading = false;

    public Maillist(int h) {
        super(Coord.of(Mailbox.bg.sz().x + UI.scale(15), UI.scale(h)), Mailbox.bg.sz().y + UI.scale(5));
    }

    public static Widget mkwidget(UI ui, Object... args) {
        return(new Maillist(UI.scale((Integer)args[0])));
    }

    protected boolean searchmatch(Mail mail, String text) {
        return((mail.name != null) &&
                (mail.name.toLowerCase().indexOf(text.toLowerCase()) >= 0));
    }
    protected List<Mail> allitems() {return(list);}
    protected Mailbox makeitem(Mail item, int idx, Coord sz) {return(new Mailbox(item));}

    protected void drawslot(GOut g, Mail item, int idx, Area area) {}

    public void tick(double dt) {
        if(loading) {
            loading = false;
            for(Mail item : list) {
                if(item.name == null) {
                    try {
                        item.name = item.res.get().layer(Resource.tooltip).t;
                    } catch(Loading l) {
                        loading = true;
                    }
                }
            }
            Collections.sort(list, (a, b) -> {
                if((a.name == null) && (b.name == null))
                    return(0);
                if(a.name == null)
                    return(1);
                if(b.name == null)
                    return(-1);
                return(a.name.compareTo(b.name));
            });
        }
        super.tick(dt);
    }

    public void uimsg(String name, Object... args) {
        if(name == "pop") {
            List<Mail> n = new ArrayList<>(list);
            for(int a = 0; a < args.length; a += 2) {
                int resid = (Integer)args[a];
                int num = (Integer)args[a + 1];
                n.add(new Mail(ui.sess.getres(resid), resid, num));
            }
            list = n;
            loading = true;
        } else if(name == "clear") {
            list = Collections.emptyList();
        } else if(name == "num") {
            for(int a = 0; a < args.length; a += 2) {
                int resid = (Integer)args[a];
                int num = (Integer)args[a + 1];
                for(Mail item : list) {
                    if(item.id == resid) {
                        item.num = num;
                        break;
                    }
                }
            }
        } else {
            super.uimsg(name, args);
        }
    }

    public void wdgmsg(Widget sender, String name, Object... args) {
        if((sender instanceof Mailbox) && (name == "recv")) {
            wdgmsg("recv", ((Mail)((Mailbox)sender).spec).id);
        } else {
            super.wdgmsg(sender, name, args);
        }
    }
}
