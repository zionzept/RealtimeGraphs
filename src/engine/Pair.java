package engine;

public class Pair <A, B> {

	public final A a;
	public final B b;
	
	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}
	
	@Override
	public int hashCode() {
		int x = a.hashCode();
		int y = b.hashCode();
		return ((((x * 23987)+y)*8736451)+x)*1922+x+y*82731987;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) return false;
		Pair<?, ?> p = (Pair<?, ?>) o;
		return a.equals(p.a) && b.equals(p.b);
	}
}
