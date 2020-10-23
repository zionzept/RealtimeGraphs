package engine;

public class Point {

	public double x;
	public double y;
	
	private double blink_start;
	private double blink_end;
	public boolean blink;
	
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public boolean move(double x, double y) {
		if (this.x == x && this.y == y) {
			return false;
		}
		this.x = x;
		this.y = y;
		return true;
	}
	
	
	public void blink(double start_time, double end_time) {
		blink_start = start_time;
		blink_end = end_time;
		blink = true;
	}
	
	public double blink_value(double pt) {
		if (!blink) {
			return 0;
		}
		if (pt >= blink_end) {
			blink = false;
			return 0;
		}
		double x = (pt - blink_start) / (blink_end - blink_start);
		return 1 + x*x*(-3 + 2*x);
	}
}
