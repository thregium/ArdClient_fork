/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Color;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Textlog extends Widget {
    static Tex texpap = Resource.loadtex("gfx/hud/texpap");
    static Tex schain = Resource.loadtex("gfx/hud/schain");
    static Tex sflarp = Resource.loadtex("gfx/hud/sflarp");
    static final RichText.Foundry fnd = new RichText.Foundry(new ChatUI.ChatParser(TextAttribute.FAMILY, Text.cfg.font.get("sans"), TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, TextAttribute.SIZE, 12, TextAttribute.FOREGROUND, Color.BLACK));
    List<ChatUI.Channel.Message> lines;
    int maxy, cury;
    int margin = 3;
    public int maxLines = 150;
    boolean quote = true;
    UI.Grab sdrag = null;

    @RName("log")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            return (new Textlog((Coord) args[0]));
        }
    }

    public void draw(GOut g) {
        Coord dc = new Coord();
        for (dc.y = 0; dc.y < sz.y; dc.y += texpap.sz().y) {
            for (dc.x = 0; dc.x < sz.x; dc.x += texpap.sz().x) {
                g.image(texpap, dc);
            }
        }
        g.chcolor();
        int y = -cury;
        boolean sel = false;
        synchronized (lines) {
            for (ChatUI.Channel.Message line : lines) {
                if ((selstart != null) && (line == selstart.msg))
                    sel = true;
                int dy1 = sz.y + y;
                int dy2 = dy1 + line.sz().y;
                if ((dy2 > 0) && (dy1 < sz.y)) {
                    if (sel)
                        drawsel(g, line, dy1);
                    g.image(line.tex(), new Coord(margin, dy1));
                }
                if ((selend != null) && (line == selend.msg))
                    sel = false;
                y += line.sz().y;
            }
        }
        if (maxy > sz.y) {
            int fx = sz.x - sflarp.sz().x;
            int cx = fx + (sflarp.sz().x / 2) - (schain.sz().x / 2);
            for (y = 0; y < sz.y; y += schain.sz().y - 1)
                g.image(schain, new Coord(cx, y));
            double a = (double) (cury - sz.y) / (double) (maxy - sz.y);
            int fy = (int) ((sz.y - sflarp.sz().y) * a);
            g.image(sflarp, new Coord(fx, fy));
        }
    }

    private void drawsel(GOut g, ChatUI.Channel.Message msg, int y) {
        RichText rt = (RichText) msg.text();
        boolean sel = msg != selstart.msg;
        for (RichText.Part part = rt.parts; part != null; part = part.next) {
            if (!(part instanceof RichText.TextPart))
                continue;
            RichText.TextPart tp = (RichText.TextPart) part;
            if (tp.start == tp.end)
                continue;
            TextHitInfo a, b;
            if (sel) {
                a = TextHitInfo.leading(0);
            } else if (tp == selstart.part) {
                a = selstart.ch;
                sel = true;
            } else {
                continue;
            }
            if (tp == selend.part) {
                sel = false;
                b = selend.ch;
            } else {
                b = TextHitInfo.trailing(tp.end - tp.start - 1);
            }
            Coord ul = new Coord(margin + tp.x + (int) tp.advance(0, a.getInsertionIndex()), tp.y + y);
            Coord sz = new Coord((int) tp.advance(a.getInsertionIndex(), b.getInsertionIndex()), tp.height());
            g.chcolor(0, 0, 255, 175);
            g.frect(ul, sz);
            g.chcolor();
            if (!sel)
                break;
        }
    }

    public Textlog(Coord sz) {
        super(sz);
        lines = new LinkedList<>();
        maxy = cury = 0;
    }

    public static class SimpleMessage extends ChatUI.Channel.Message {
        private final Text t;
        public final String text;

        public SimpleMessage(String text, Color col, int w) {
            this.text = text;
            if (col == null)
                this.t = fnd.render(RichText.Parser.quote(text), w);
            else
                this.t = fnd.render(RichText.Parser.quote(text), w, TextAttribute.FOREGROUND, col);
        }

        public Text text() {
            return (t);
        }

        public Tex tex() {
            return (t.tex());
        }

        public Coord sz() {
            return (t.sz());
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public int iw() {
        return (sz.x - (margin * 2) - sflarp.sz().x);
    }

    public void append(String line, Color col) {
        Text rl;
        if (quote) {
            line = RichText.Parser.quote(line);
        }
        if (col == null)
            rl = fnd.render(line, sz.x - (margin * 2) - sflarp.sz().x);
        else
            rl = fnd.render(line, sz.x - (margin * 2) - sflarp.sz().x, TextAttribute.FOREGROUND, col);
        synchronized (lines) {
            lines.add(new SimpleMessage(line, col, iw())); //rl
            if ((maxLines > 0) && (lines.size() > maxLines)) {
                ChatUI.Channel.Message tl = lines.remove(0);
                int dy = tl.sz().y;
                maxy -= dy;
                cury -= dy;
            }
        }
        if (cury == maxy)
            cury += rl.sz().y;
        maxy += rl.sz().y;
    }

    public void append(String line) {
        append(line, null);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "apnd") {
            append((String) args[0]);
        }
    }

    public boolean mousewheel(Coord c, int amount) {
        cury += amount * 20;
        if (cury < sz.y)
            cury = sz.y;
        if (cury > maxy)
            cury = maxy;
        return (true);
    }

    private ChatUI.Channel.CharPos selorig, lasthit, selstart, selend;
    private UI.Grab grab;
    private boolean dragging;

    public ChatUI.Channel.Message messageat(Coord c, Coord hc) {
        int y = 0;
        synchronized (lines) {
            for (ChatUI.Channel.Message msg : lines) {
                Coord msz = msg.sz();
                if ((c.y + (cury - sz.y) >= y) && (c.y + (cury - sz.y) < y + msz.y)) {
                    if (hc != null) {
                        hc.x = c.x;
                        hc.y = c.y + (cury - sz.y) - y;
                    }
                    return (msg);
                }
                y += msz.y;
            }
        }
        return (null);
    }

    public ChatUI.Channel.CharPos charat(Coord c) {
        if (c.y < 0) {
            if (lines.size() < 1)
                return (null);
            ChatUI.Channel.Message msg = lines.get(0);
            if (!(msg.text() instanceof RichText))
                return (null);
            RichText.TextPart fp = null;
            for (RichText.Part part = ((RichText) msg.text()).parts; part != null; part = part.next) {
                if (part instanceof RichText.TextPart) {
                    fp = (RichText.TextPart) part;
                    break;
                }
            }
            if (fp == null)
                return (null);
            return (new ChatUI.Channel.CharPos(msg, fp, TextHitInfo.leading(0)));
        }

        Coord hc = new Coord();
        ChatUI.Channel.Message msg = messageat(c, hc);
        if ((msg == null) || !(msg.text() instanceof RichText))
            return (null);
        RichText rt = (RichText) msg.text();
        RichText.Part p = rt.partat(hc);
        if (p == null) {
            RichText.TextPart lp = null;
            for (RichText.Part part = ((RichText) msg.text()).parts; part != null; part = part.next) {
                if (part instanceof RichText.TextPart)
                    lp = (RichText.TextPart) part;
            }
            if (lp == null) return (null);
            return (new ChatUI.Channel.CharPos(msg, lp, TextHitInfo.trailing(lp.end - lp.start - 1)));
        }
        if (!(p instanceof RichText.TextPart))
            return (null);
        RichText.TextPart tp = (RichText.TextPart) p;
        return (new ChatUI.Channel.CharPos(msg, tp, tp.charat(hc)));
    }

    public boolean mousedown(Coord c, int button) {
        if (button != 1)
            return (false);
        int fx = sz.x - sflarp.sz().x;
        int cx = fx + (sflarp.sz().x / 2) - (schain.sz().x / 2);
        if ((maxy > sz.y) && (c.x >= fx)) {
            sdrag = ui.grabmouse(this);
            mousemove(c);
            return (true);
        } else {
            selstart = selend = null;
            ChatUI.Channel.CharPos ch = charat(c);
            if (ch != null) {
                selorig = lasthit = ch;
                dragging = false;
                grab = ui.grabmouse(this);
            }
            return (true);
        }
//        return (false);
    }

    public final Comparator<ChatUI.Channel.CharPos> poscmp = new Comparator<ChatUI.Channel.CharPos>() {
        public int compare(ChatUI.Channel.CharPos a, ChatUI.Channel.CharPos b) {
            if (a.msg != b.msg) {
                synchronized (lines) {
                    for (ChatUI.Channel.Message msg : lines) {
                        if (msg == a.msg)
                            return (-1);
                        else if (msg == b.msg)
                            return (1);
                    }
                }
                throw (new IllegalStateException("CharPos message is no longer contained in the log"));
            } else if (a.part != b.part) {
                for (RichText.Part part = ((RichText) a.msg.text()).parts; part != null; part = part.next) {
                    if (part == a.part)
                        return (-1);
                    else
                        return (1);
                }
                throw (new IllegalStateException("CharPos is no longer contained in the log"));
            } else {
                return (a.ch.getInsertionIndex() - b.ch.getInsertionIndex());
            }
        }
    };

    public void mousemove(Coord c) {
        if (sdrag != null) {
            double a = (double) (c.y - (sflarp.sz().y / 2)) / (double) (sz.y - sflarp.sz().y);
            if (a < 0)
                a = 0;
            if (a > 1)
                a = 1;
            cury = (int) (a * (maxy - sz.y)) + sz.y;
        } else if (selorig != null) {
            ChatUI.Channel.CharPos ch = charat(c);
            if ((ch != null) && !ch.equals(lasthit)) {
                lasthit = ch;
                if (!dragging && !ch.equals(selorig))
                    dragging = true;
                int o = poscmp.compare(selorig, ch);
                if (o < 0) {
                    selstart = selorig;
                    selend = ch;
                } else if (o > 0) {
                    selstart = ch;
                    selend = selorig;
                } else {
                    selstart = selend = null;
                }
            }
        } else {
            super.mousemove(c);
        }
    }

    public boolean mouseup(Coord c, int button) {
        if (button == 1) {
            if (sdrag != null) {
                sdrag.remove();
                sdrag = null;
                return (true);
            } else if (selorig != null) {
                if (selstart != null)
                    selected(selstart, selend);
                else
                    clicked(selorig);
                grab.remove();
                selorig = null;
                dragging = false;
                return (true);
            }
        }
        return (false);
    }

    protected void selected(ChatUI.Channel.CharPos start, ChatUI.Channel.CharPos end) {
        StringBuilder buf = new StringBuilder();
        synchronized (lines) {
            boolean sel = false;
            for (ChatUI.Channel.Message msg : lines) {
                if (!(msg.text() instanceof RichText))
                    continue;
                RichText rt = (RichText) msg.text();
                RichText.Part part = null;
                if (sel) {
                    part = rt.parts;
                } else if (msg == start.msg) {
                    sel = true;
                    for (part = rt.parts; part != null; part = part.next) {
                        if (part == start.part)
                            break;
                    }
                }
                if (sel) {
                    for (; part != null; part = part.next) {
                        if (!(part instanceof RichText.TextPart))
                            continue;
                        RichText.TextPart tp = (RichText.TextPart) part;
                        CharacterIterator iter = tp.ti();
                        int sch;
                        if (tp == start.part)
                            sch = tp.start + start.ch.getInsertionIndex();
                        else
                            sch = tp.start;
                        int ech;
                        if (tp == end.part)
                            ech = tp.start + end.ch.getInsertionIndex();
                        else
                            ech = tp.end;
                        for (int i = sch; i < ech; i++)
                            buf.append(iter.setIndex(i));
                        if (part == end.part) {
                            sel = false;
                            break;
                        }
                        buf.append(' ');
                    }
                    if (sel)
                        buf.append('\n');
                }
                if (msg == end.msg)
                    break;
            }
        }
        Clipboard cl;
        if ((cl = java.awt.Toolkit.getDefaultToolkit().getSystemSelection()) == null)
            cl = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            final ChatUI.Channel.CharPos ownsel = selstart;
            cl.setContents(new StringSelection(buf.toString()),
                    new ClipboardOwner() {
                        public void lostOwnership(Clipboard cl, Transferable tr) {
                            if (selstart == ownsel)
                                selstart = selend = null;
                        }
                    });
        } catch (IllegalStateException e) {
        }
    }

    protected void clicked(ChatUI.Channel.CharPos pos) {
        AttributedCharacterIterator inf = pos.part.ti();
        inf.setIndex(pos.ch.getCharIndex() + pos.part.start);
        ChatUI.FuckMeGentlyWithAChainsaw url = (ChatUI.FuckMeGentlyWithAChainsaw) inf.getAttribute(ChatUI.ChatAttribute.HYPERLINK);
        if ((url != null) && (WebBrowser.self != null)) {
            try {
                WebBrowser.self.show(url.url);
            } catch (WebBrowser.BrowserException e) {
                getparent(GameUI.class).error("Could not launch web browser.");
            }
        }
    }

    public void setprog(double a) {
        if (a < 0)
            a = 0;
        if (a > 1)
            a = 1;
        cury = (int) (a * (maxy - sz.y)) + sz.y;
    }
}
