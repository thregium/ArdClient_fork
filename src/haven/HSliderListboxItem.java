package haven;

import java.util.List;
import java.util.Objects;

public class HSliderListboxItem {
    public String name;
    public int val;

    public HSliderListboxItem(String name, int val) {
        this.name = name;
        this.val = val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HSliderListboxItem)) return false;
        HSliderListboxItem that = (HSliderListboxItem) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static HSliderListboxItem contains(List<HSliderListboxItem> list, String name) {
        for (HSliderListboxItem item : list)
            if (item.name.equals(name))
                return (item);
        return (null);
    }

    public boolean contains(String text) {
        if (name.contains(text))
            return (true);
        return (false);
    }
}
