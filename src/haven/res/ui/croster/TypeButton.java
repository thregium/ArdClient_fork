package haven.res.ui.croster;

import haven.Audio;
import haven.Button;
import haven.IButton;

import java.awt.image.BufferedImage;

public class TypeButton extends IButton {
    public final int order;

    public TypeButton(BufferedImage up, BufferedImage down, int order) {
        super(up, down);
        this.order = order;
    }

    protected void depress() {
        Audio.play(Button.lbtdown.stream());
    }

    protected void unpress() {
        Audio.play(Button.lbtup.stream());
    }
}

