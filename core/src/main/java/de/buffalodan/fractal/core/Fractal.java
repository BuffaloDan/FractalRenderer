package de.buffalodan.fractal.core;

public interface Fractal {
	public void zoom(double startX, double startY, double endX, double endY, int width, int height);
	
	public void reset();

	public void createFractal(byte[] data, int width, int height);
	public void createFractal(byte[] data, int startX, int startY, int endX, int endY, int width, int height);
	
	public void setMaxIterations(int maxIterations);
}
