package haven.res.lib.layspr;

import haven.Coord;
import haven.GOut;
import haven.Resource;

class Image extends Layer {
    final Resource.Image img;

    Image(Resource.Image img) {
        super(img.z, img.sz);
        this.img = img;
    }

    void draw(GOut g) {
        g.image(img, Coord.z);
    }
}