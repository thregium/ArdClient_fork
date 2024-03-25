package haven.res.ui.obj.buddy;

import haven.CompImage;
import haven.FromResource;
import haven.PView;

@FromResource(name = "ui/obj/buddy", version = 3, override = true)
public interface InfoPart {
    void draw(CompImage cmp, PView.RenderContext ctx);

    default int order() {return (0);}

    default boolean auto() {return (false);}
}
