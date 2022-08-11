package haven;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class SSearchBox<I, W extends Widget> extends SListBox<I, W> {
    private static final int C = 1;
    private static final int M = 2;
    public String searching = null;
    private List<I> filtered = null;
    private Text info;

    protected abstract List<? extends I> allitems();

    protected abstract boolean searchmatch(I item, String text);

    @Override
    protected List<? extends I> items() {
        if (searching != null)
            return (filtered);
        return (allitems());
    }

    public SSearchBox(Coord sz, int itemh) {
        super(sz, itemh);
        setcanfocus(true);
    }

    @Override
    public boolean keydown(KeyEvent ev) {
        int mod = 0;
        if ((ev.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) mod |= C;
        if ((ev.getModifiersEx() & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0) mod |= M;
        char c = ev.getKeyChar();
        if (c == ev.CHAR_UNDEFINED)
            c = '\0';
        int code = ev.getKeyCode();
        if (mod == 0) {
            if (c == 8) {
                if (searching != null) {
                    search(searching.substring(0, searching.length() - 1));
                    return (true);
                }
            } else if (key_act.match(ev)) {
                if (searching != null) {
                    stopsearch();
                    return (true);
                }
            } else if (key_esc.match(ev)) {
                if (searching != null) {
                    stopsearch();
                    return (true);
                }
            } else if (code == KeyEvent.VK_UP) {
                List<? extends I> items = items();
                if (items.size() > 0) {
                    int p = items.indexOf(sel);
                    if (p < 0) p = 0;
                    if (p > 0) p -= 1;
                    change(items.get(p));
                    display(p);
                }
                return (true);
            } else if (code == KeyEvent.VK_DOWN) {
                List<? extends I> items = items();
                if (items.size() > 0) {
                    int p = items.indexOf(sel);
                    if (p < 0) p = items.size() - 1;
                    if (p < items.size() - 1) p += 1;
                    change(items.get(p));
                    display(p);
                }
                return (true);
            } else if (c >= 32) {
                search(((searching == null) ? "" : searching) + c);
                return (true);
            }
        }
        return (super.keydown(ev));
    }

    private void updinfo() {
        this.info = Text.renderf(Color.WHITE, "%s (%d/%d)", searching, filtered.size(), allitems().size());
    }

    public void search(String text) {
        if (text.length() < 1) {
            stopsearch();
            return;
        }
        List<I> found = new ArrayList<>();
        List<? extends I> items = allitems();
        boolean sf = false, bs = true;
        int ncc = -1;
        for (I item : items) {
            if (item == sel)
                bs = false;
            if (searchmatch(item, text)) {
                if (item == sel)
                    sf = true;
                if (bs || (ncc < 0))
                    ncc = found.size();
                found.add(item);
            }
        }
        filtered = found;
        searching = text;
        if (!sf) {
            if (ncc >= 0)
                change(found.get(ncc));
            else
                change(null);
        }
        if (sel != null)
            display(sel);
        updinfo();
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        if (searching != null) {
            g.aimage(info.tex(), g.sz(), 1, 1);
        }
    }

    public void stopsearch() {
        searching = null;
        filtered = null;
        if (sel != null)
            display(sel);
    }

    @Override
    public void lostfocus() {
        super.lostfocus();
        stopsearch();
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        parent.setfocus(this);
        return (super.mousedown(c, button));
    }
}
