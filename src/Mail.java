import haven.Indir;
import haven.Resource;

public class Mail extends haven.res.ui.mailbox.Mail {
    public final int id;
    public String name;

    public Mail(Indir<Resource> res, int id, int num) {
        super(res, num);
        this.id = id;
    }
}
