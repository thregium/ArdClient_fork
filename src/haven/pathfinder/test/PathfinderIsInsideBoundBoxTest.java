package haven.pathfinder.test;

import haven.Coord;
import haven.pathfinder.Pathfinder;
import haven.sloth.script.pathfinding.Hitbox;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.Arrays;

@RunWith(Parameterized.class)
public class PathfinderIsInsideBoundBoxTest {
    @Parameterized.Parameters()
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new Coord(869, 841), 5.565857541244563, new Hitbox[]{new Hitbox(new Coord(-10, -2), new Coord(10, 2))}, new Coord(874, 836), true},
                {new Coord(869, 841), 5.565857541244563 - Math.PI, new Hitbox[]{new Hitbox(new Coord(-10, -2), new Coord(10, 2))}, new Coord(874, 836), true},
                {new Coord(869, 841), 5.565857541244563 - 2 * Math.PI, new Hitbox[]{new Hitbox(new Coord(-10, -2), new Coord(10, 2))}, new Coord(874, 836), true},
                {new Coord(869, 841), -5.565857541244563, new Hitbox[]{new Hitbox(new Coord(-10, -2), new Coord(10, 2))}, new Coord(874, 836), false},
                {new Coord(869, 841), -5.565857541244563 + Math.PI, new Hitbox[]{new Hitbox(new Coord(-10, -2), new Coord(10, 2))}, new Coord(874, 836), false},
                {new Coord(869, 841), -5.565857541244563 + 2 * Math.PI, new Hitbox[]{new Hitbox(new Coord(-10, -2), new Coord(10, 2))}, new Coord(874, 836), false},
        });
    }

    @Parameterized.Parameter
    public Coord gobRc;

    @Parameterized.Parameter(1)
    public double gobA;

    @Parameterized.Parameter(2)
    public Hitbox gobHitbox[];

    @Parameterized.Parameter(3)
    public Coord point;

    @Parameterized.Parameter(4)
    public boolean expected;

    @Test
    public void isInsideBoundBox() {
        assertEquals(expected, Pathfinder.isInsideBoundBox(gobRc, gobA, gobHitbox, point));
    }
}
