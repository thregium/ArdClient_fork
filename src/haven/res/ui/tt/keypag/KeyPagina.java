package haven.res.ui.tt.keypag;

import haven.ItemInfo;
import haven.Resource;
import haven.RichText;
import haven.UI;

import java.awt.image.BufferedImage;

public class KeyPagina extends ItemInfo.Pagina {
    private static final String DEF_SHORT = "STACK";
    public KeyPagina(final Owner owner, final String text) {
        super(owner, text);
    }

    public static KeyPagina mkinfo(final Owner owner, final Object... args) {
        if (args[1] instanceof String)
            return (new KeyPagina(owner, (String) args[1]));
        Resource res = owner.context(Resource.Resolver.class).getres((Integer) args[1]).get();
        return (new KeyPagina(owner, res.layer(Resource.pagina).text));
    }

    @Override
    public BufferedImage tipimg(final int w) {
        UI ui = owner.glob().ui.get();
        boolean extended = ui != null && ui.modflags() == UI.MOD_SHIFT;
        return (RichText.render(extended ? str : DEF_SHORT, w).img);
    }

    public int order() {return (20000);}
}
