package de.buffalodan.fractal.core;

public class Context {
	private int width;
	private int height;
	private Fractal fractal;

	public Context(int width, int height, Fractal fractal) {
		this.width = width;
		this.height = height;
		this.fractal = fractal;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public Fractal getFractal() {
		return fractal;
	}
}
