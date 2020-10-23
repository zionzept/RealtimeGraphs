package engine;

public class IntPoint {

	public final int x;
	public final int y;
	
	public IntPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public int hashCode() {
		return ((((x * 23987)+y)*8736451)+x)*1922+x+y*82731987;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IntPoint)) return false;
		IntPoint p = (IntPoint) o;
		return x == p.x && y == p.y;
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

}
