package haven.res.ui.tt.defn;

public interface DynName {
    String name();

    default String oname() {
        return (name());
    }
}