package de.buffalodan.fractal.core;

public abstract class FractalRenderer {
	protected Context context;
	private byte[] buffer;
	private boolean running;
	private boolean updateFractal;
	
	private static final int PROCESSES = 8;
	private int fractalUpdating = PROCESSES;

	public FractalRenderer(Context context) {
		this.context = context;
		buffer = new byte[context.getWidth() * context.getHeight() * 4];
		init();
	}

	protected int getBufferSize() {
		return buffer.length;
	}

	private void init() {
		initGL();
		running = true;
		updateFractal = true;
	}

	public boolean isFractalUpdating() {
		return fractalUpdating != 0;
	}

	protected abstract void initGL();

	private void createFractal() {
		for (int i = 0; i < PROCESSES; i++) {
			final int c = i;
			Runnable updater = () -> {
				int startX = context.getWidth() / PROCESSES * c;
				int endX = c == PROCESSES - 1 ? context.getWidth() : context.getWidth() / PROCESSES * (c + 1);
				context.getFractal().createFractal(buffer, startX, 0, endX, context.getHeight(), context.getWidth(), context.getHeight());
				fractalUpdating++;
			};
			new Thread(updater).start();
		}
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

	protected abstract void render(byte[] data);
}
