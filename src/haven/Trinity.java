package haven;

public class Trinity<A, B, C> {
    public final A a;
    public final B b;
    public final C c;

    public Trinity(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public String toString() {
        return (String.format("(%s . %s . %s)", a, b, c));
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + (c != null ? c.hashCode() : 0);
        return result;
    }

    public boolean equals(Object O) {
        if (!(O instanceof Trinity))
            return (false);
        Trinity o = (Trinity<?, ?, ?>) O;
        return (Utils.eq(a, o.a) && Utils.eq(b, o.b) && Utils.eq(c, o.c));
    }
}