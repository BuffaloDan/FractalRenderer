package de.buffalodan.fractal.core;

public interface Fractal {
	public void zoom(double startX, double startY, double endX, double endY, int width, int height);
	
	public void reset();

	public void createFractal(byte[] data, int width, int height);
	public void createFractal(int[] data, int startX, int startY, int endX, int endY, int width, int height);

    public int getMaxIterations();
	public void setMaxIterations(int maxIterations);

	default public void dispose() {}

	public void init(Context context);
}
