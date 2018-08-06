package de.buffalodan.fractal.core;

public abstract class FractalRenderer {
	protected Context context;
	private int[] buffer;
	private boolean running;
	private boolean updateFractal;

	private int fractalUpdating;

	public FractalRenderer(Context context) {
		this.context = context;
		buffer = new int[context.getWidth() * context.getHeight()];
		init();
	}

	protected int getBufferSize() {
		return buffer.length;
	}

	protected void init() {
		running = true;
		updateFractal = true;
		fractalUpdating = context.getProcesses();
	}

	public boolean isFractalUpdating() {
		return fractalUpdating != 0;
	}

	private void createFractal() {
		fractalUpdating = context.getProcesses();
		for (int x = 0; x < context.getProcessesPerRow(); x++) {
			for (int y = 0; y < context.getProcessesPerRow(); y++) {
			final int xc = x;
			final int yc = y;
			Runnable updater = () -> {
				int startX = context.getWidth() / context.getProcessesPerRow() * xc;
				int endX = xc == context.getProcessesPerRow() - 1 ? context.getWidth() : context.getWidth() / context.getProcessesPerRow() * (xc + 1);
				int startY = context.getHeight() / context.getProcessesPerRow() * yc;
				int endY = yc == context.getProcessesPerRow() - 1 ? context.getHeight() : context.getHeight() / context.getProcessesPerRow() * (yc + 1);
				context.getFractal().createFractal(buffer, startX, startY, endX, endY, context.getWidth(), context.getHeight());
				fractalUpdating--;
			};
			new Thread(updater).start();
		}}
	}

	public void start() {
		long sleepTime = 1000L / 60L;
		while (running) {
			update();
			if (updateFractal) {
				fractalUpdating = 0;
				createFractal();
				updateFractal = false;
			}
			render(buffer);

			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
			}
		}
		dispose();
	}

	protected abstract void update();

	public void updateFractal() {
		updateFractal = true;
	}

	public void stop() {
		running = false;
	}

	protected abstract void dispose();

	protected abstract void render(int[] data);
}
