package haven.res.ui.mailbox;

import haven.Indir;
import haven.Resource;

public class Mail {
    public final Indir<Resource> res;
    public int num;

    public Mail(Indir<Resource> res, int num) {
        this.res = res;
        this.num = num;
    }
}
