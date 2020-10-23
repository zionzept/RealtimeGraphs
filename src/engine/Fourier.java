package engine;

import java.util.LinkedList;

public class Fourier {

	private static final double tau = Math.PI * 2;
	
	public static LinkedList<Point> transform(LinkedList<Point> t_points, double ox, double oy) {
		double[] tx = new double[t_points.size()];
		double[] ty = new double[t_points.size()];
		int i = 0;
		for (Point p : t_points) {
			tx[i] = p.x - ox;
			ty[i] = p.y - oy;
			i++;
		}
		
		LinkedList<Point> f_points = new LinkedList<Point>();
		int N = t_points.size();
		double iN = 1d/N;
		for (int k = 0; k < N; k++) {
			double fx = 0;
			double fy = 0;
			for (int n = 0; n < N; n++) {
				fx += tx[n] * Math.cos(tau*k*n*iN) + ty[n] * Math.sin(tau*k*n*iN);
				fy += ty[n] * Math.cos(tau*k*n*iN) - tx[n] * Math.sin(tau*k*n*iN);
			}
			fx *= iN;
			fy *= iN;
			f_points.add(new Point(fx, fy));
		}
		return f_points;
	}
}
