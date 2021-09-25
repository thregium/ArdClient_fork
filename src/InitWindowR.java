/* Preprocessed source code */

import haven.Button;
import haven.Coord;
import haven.Img;
import haven.Resource;
import haven.RichText;
import haven.Tex;
import haven.Text;
import haven.TextEntry;
import haven.UI;
import haven.Widget;
import haven.Window;

import java.awt.Color;

/* >wdg: InitWindow */
public class InitWindowR extends Window {
    public static final Tex bg = Resource.remote().loadwait("ui/rinit").layer(Resource.imgc).tex();
    public static final RichText.Foundry fnd = new RichText.Foundry(Text.latin.deriveFont(UI.scale(16.5f)), Color.BLACK).aa(true);
    public static final RichText.Foundry nfnd = new RichText.Foundry(Text.latin.deriveFont(UI.scale(25f)), Color.BLACK).aa(true);
    public static final int width = UI.scale(400);
    public static final String body = Resource.remote().loadwait("ui/rinit").layer(Resource.pagina).text;
    public final TextEntry name;

    public static final String ihead =
            "$col[160,0,0]{Upon the illustrious founding} of this, the great Realm of...";
    public static final String jhead =
            "$col[160,0,0]{Upon joining} this, the great Realm of...";

    public InitWindowR(String chr, String rname) {
        super(bg.sz(), "Found realm", true);
        //decohide(true);
        makeHidable();
        add(new Img(bg) {
            public boolean mousedown(Coord c, int button) {
                if (checkhit(c)) {
                    if (button == 1)
                        InitWindowR.this.drag(parentpos(InitWindowR.this, c));
                    return (true);
                }
                return (false);
            }
        }, 0, 0);
        Widget cont = add(new Widget(), (bg.sz().x - width) / 2, UI.scale(52));
        Widget prev;
        if (rname == null) {
            prev = cont.add(new Img(fnd.render(ihead, width).tex()), 0, 0);
            prev = name = cont.adda(new TextEntry(UI.scale(200), ""), prev.pos("bl").adds(0, 15).x(width / 2), 0.5, 0.0);
        } else {
            prev = cont.add(new Img(fnd.render(jhead, width).tex()), 0, 0);
            prev = cont.adda(new Img(nfnd.render("$col[160,0,0]{" + RichText.Parser.quote(rname) + "}", width).tex()), prev.pos("bl").adds(0, 15).x(width / 2), 0.5, 0.0);
            name = null;
        }
        String body = this.body;
        if (chr != null)
            body = body.replace("NN", RichText.Parser.quote(chr));
        prev = cont.add(new Img(fnd.render(body, width).tex()), prev.pos("bl").adds(0, 15).x(0));
        cont.adda(new Button(UI.scale(200), "Sign in Blood", true).action(this::form), prev.pos("bl").adds(0, 15).x(width / 2), 0.5, 0.0);
        cont.pack();
        pack();
    }

    private void form() {
        wdgmsg("found", (name != null) ? name.text() : null);
    }

    public static Widget mkwidget(UI ui, Object[] args) {
        String chr = (String) args[0];
        String rname = (args.length > 1) ? (String) args[1] : null;
        return (new InitWindowR(chr, rname));
    }
}
