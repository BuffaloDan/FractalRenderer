package de.buffalodan.fractal.core;

public class Context {
    private int width;
    private int height;
    private Fractal fractal;

    private final int processesPerRow;
    private final int processes;

    public Context(int width, int height, Fractal fractal, int processesPerRow) {
        this.width = width;
        this.height = height;
        this.fractal = fractal;
        this.processesPerRow = processesPerRow;
        this.processes = processesPerRow * processesPerRow;

        fractal.init(this);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getBufferSize() {
        return width * height;
    }

    public Fractal getFractal() {
        return fractal;
    }

    public int getProcessesPerRow() {
        return processesPerRow;
    }

    public int getProcesses() {
        return processes;
    }

    public void dispose() {
        fractal.dispose();
    }
}
