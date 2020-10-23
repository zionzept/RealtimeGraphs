package engine;

public class Triplet <A, B, C> {

	public final A a;
	public final B b;
	public final C c;
	
	public Triplet(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	@Override
	public int hashCode() {
		int x = a.hashCode();
		int y = b.hashCode();
		int z = c.hashCode();
		return ((((x * 23987)+y)*8736451)+z)*1922+x+987123*y+z*82731987;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Triplet)) return false;
		Triplet<?, ?, ?> p = (Triplet<?, ?, ?>) o;
		return a.equals(p.a) && b.equals(p.b) && c.equals(p.c);
	}
}
