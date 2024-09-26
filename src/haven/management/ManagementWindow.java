package haven.management;

import haven.Button;
import haven.Coord;
import haven.Window;

public class ManagementWindow extends Window {

    public ManagementWindow() {
        super(new Coord(1000, 500), "Management", "Management");

        add(new Button(200, "Register") {
            @Override
            public void click() {
                GridCheck.saveGridAdj(-1);
            }
        }, 50, 50);
    }
}
