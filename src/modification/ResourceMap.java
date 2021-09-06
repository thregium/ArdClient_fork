package modification;

import haven.Indir;
import haven.Message;
import haven.Resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResourceMap implements Resource.Resolver {
    public final Resource.Resolver bk;
    public final Map<Integer, Integer> map;

    public ResourceMap(Resource.Resolver var1, Map<Integer, Integer> var2) {
        this.bk = var1;
        this.map = var2;
    }

    public ResourceMap(Resource.Resolver var1, Message var2) {
        this(var1, decode(var2));
    }

    public static Map<Integer, Integer> decode(Message var0) {
        if (var0.eom()) {
            return Collections.emptyMap();
        } else {
            int var1 = var0.uint8();
            HashMap var2 = new HashMap();

            for (int var3 = 0; var3 < var1; ++var3) {
                var2.put(var0.uint16(), var0.uint16());
            }

            return var2;
        }
    }

    public Indir<Resource> getres(int var1) {
        return this.bk.getres((Integer) this.map.get(var1));
    }
}