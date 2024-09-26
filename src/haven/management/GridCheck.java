package haven.management;

import haven.Config;
import haven.Utils;

import com.google.gson.*;

public class GridCheck {

    static void saveGridAdj(long id) {
        String s = "academy/grids/" + id;
        JsonObject adjGrid = new JsonObject();
        try {
            adjGrid = Utils.loadCustomElement(s).getAsJsonObject();
        }
        catch(Exception e) {

        }
        boolean needtoSave = false;
        if(adjGrid == null) needtoSave = true;
        else {
            if(!adjGrid.has("adj")) needtoSave = true;
        }
        if(needtoSave) {
            JsonArray adjArr = new JsonArray(9);
            while(adjArr.size() < 9) adjArr.add(0);
            for(int i = -1; i <= 1; i++) {
                for(int j = -1; j <= 1; j++) {
                    if(i == 0 && j == 0) continue;
                    try {
                        adjArr.set((i + 1) * 3 + (j + 1), new JsonPrimitive(-1));
                    }
                    catch(Exception e) {

                    }
                }
            }
            adjGrid.add("adj", adjArr);
            Utils.saveCustomElement(adjGrid, s);
        }
    }
}
