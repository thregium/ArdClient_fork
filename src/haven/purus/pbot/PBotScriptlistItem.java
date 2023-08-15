package haven.purus.pbot;

import haven.Coord;
import haven.GOut;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;
import haven.Widget;
import modification.configuration;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class PBotScriptlistItem extends Widget implements Comparable<PBotScriptlistItem> {
    private String name;
    public Path scriptFile;
    private Tex iconTex;
    private Tex nameTex;
    public final UI ui;
    public final boolean isParent;
    public final boolean isFolder;

    public PBotScriptlistItem(UI ui, Path path, boolean parent) {
        this.scriptFile = path;
        this.ui = ui;
        this.isParent = parent;
        this.isFolder = Files.isDirectory(path);

        if (!this.isFolder()) {
            File icon = new File(scriptFile.toFile().getPath().substring(0, scriptFile.toFile().getPath().lastIndexOf(".")) + ".png");
            if (icon.exists()) {
                try {
                    this.iconTex = new TexI(ImageIO.read(icon));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                this.iconTex = configuration.imageToTex(configuration.modificationPath + "/paginae/purus/PBotMenu.png");//Resource.local().load("paginae/purus/PBotMenu").get().layer(Resource.Image.class).tex();
            }
        } else {
            this.iconTex = configuration.imageToTex(configuration.modificationPath + "/paginae/purus/PBotFolder.png");
        }

//        this.nameTex = Text.render(name.substring(0, name.length() - 5)).tex();

        if (Files.exists(scriptFile))
            if (parent)
                setName("../");
            else
                setName(scriptFile.getFileName().toString() + (isFolder() ? "/" : ""));
        else
            setName("...");

        resize(Coord.of(nameTex.sz().x + (iconTex != null ? iconTex.sz().x : 24), iconTex != null ? iconTex.sz().y : 24));
    }

    public boolean isFolder() {
        return (isFolder);
    }

    public void setName(String name) {
        this.name = name;
        Color clr = Color.WHITE;
        if (isParent)
            clr = Color.ORANGE;
        else if (isFolder)
            clr = Color.YELLOW;
        else {
            Path parent = scriptFile.getParent();
            if (parent != null && !Objects.equals(parent, PBotScriptlist.defPath))
                name = String.format("%s (%s)", name, parent.getFileName().toString());
        }
        this.nameTex = Text.render(name, clr).tex();
    }

    public PBotScriptlistItem(UI ui, Path path) {
        this(ui, path, false);
    }

    public void runScript() {
        try {
            if (!isFolder())
                PBotScriptmanager.startScript(ui, scriptFile.toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Tex getIconTex() {
        return this.iconTex;
    }

    public Tex getNameTex() {
        return this.nameTex;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        if (iconTex != null)
            g.image(getIconTex(), Coord.z, Coord.of(24, 24));
        g.aimage(getNameTex(), Coord.of(32, 12), 0, 0.5);
    }

    @Override
    public int compareTo(PBotScriptlistItem o) {
        int c;
        c = Boolean.compare(o.isParent, this.isParent);
        if (c == 0)
            c = Boolean.compare(o.isFolder(), this.isFolder());
        if (c == 0)
            c = this.name.toLowerCase().compareTo(o.name.toLowerCase());
        return (c);
    }
}
