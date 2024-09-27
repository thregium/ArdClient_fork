package haven.management;

import haven.*;

import com.google.gson.*;

public class GridCheck {

    static void saveGridAdj(UI ui, long id) {
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
                    //if(i == 0 && j == 0) continue;
                    try {
                        final Glob glob;
                        Coord rc = new Coord(ui.gui.map.player().rc);
                        rc.x += 1100 * i;
                        rc.y += 1100 * j;
                        long gridid = ui.gui.map.glob.map.getgrid(rc).id;
                        adjArr.set((i + 1) * 3 + (j + 1), new JsonPrimitive(gridid));
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
