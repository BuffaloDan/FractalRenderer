package de.buffalodan.fractal.app;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import javax.imageio.ImageIO;

import de.buffalodan.fractal.mandelbrot.MandelbrotCLFractal;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.opengl.GL;

import de.buffalodan.fractal.core.Context;
import de.buffalodan.fractal.core.FractalRenderer;

public class DesktopApp extends FractalRenderer {

	private long window;
	private GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);

	private GLFWMouseButtonCallback mouseCallback;
	private GLFWKeyCallback keyCallback;
	private ByteBuffer bbuffer;

	private DoubleBuffer mouseX = BufferUtils.createDoubleBuffer(1);
	private DoubleBuffer mouseY = BufferUtils.createDoubleBuffer(1);
	private double x1, y1, x2, y2;

	private boolean takeScreenshot = false;

	public DesktopApp(Context context) {
		super(context);
	}

	public static void main(String[] args) {
		Context desktopContext = new Context(800, 800, new MandelbrotCLFractal(), 1);
		new DesktopApp(desktopContext).start();
	}

	private void screenshot(int[] data) {
		String fileName = "test";
		int i = 0;
		File file; // The file to save to.
		String format = "PNG"; // Example: "PNG" or "JPG"

		while ((file = new File(fileName + (i++) + ".png")).exists()) {
		}

		int width = context.getWidth();
		int height = context.getHeight();

		ColorModel colorModel = new DirectColorModel(32,
                0x000000ff,       // Red
                0x0000ff00,       // Green
                0x00ff0000,       // Blue
                0xff000000        // Alpha
                );
		SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
		DataBuffer buffer = new DataBufferInt(data, width * height);
		WritableRaster raster = Raster.createWritableRaster(sampleModel, buffer, null);
		BufferedImage image = new BufferedImage(colorModel, raster, false, null);
		try {
			ImageIO.write(image, format, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    @Override
    protected void init() {
	    initGL();
        super.init();
    }

    @Override
    public void start() {
        glfwFocusWindow(window);
        super.start();
    }

    protected void initGL() {
		glfwSetErrorCallback(errorCallback);
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}
		window = glfwCreateWindow(context.getWidth(), context.getHeight(), "Simple example", NULL, NULL);
		if (window == NULL) {
			glfwTerminate();
			throw new RuntimeException("Failed to create the GLFW window");
		}
		keyCallback = new GLFWKeyCallback() {

			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
					glfwSetWindowShouldClose(window, true);
				} else if (key == GLFW_KEY_R && action == GLFW_PRESS && !isFractalUpdating()) {
					context.getFractal().reset();
					updateFractal();
				} else if (key == GLFW_KEY_SPACE && action == GLFW_PRESS && !isFractalUpdating()) {
					takeScreenshot = true;
				} else if (key == GLFW_KEY_PAGE_UP && action == GLFW_PRESS && !isFractalUpdating()) {
					context.getFractal().setMaxIterations(context.getFractal().getMaxIterations()+200);
					updateFractal();
				} else if (key == GLFW_KEY_PAGE_DOWN && action == GLFW_PRESS && !isFractalUpdating()) {
					context.getFractal().setMaxIterations(context.getFractal().getMaxIterations()-200);
					updateFractal();
				}
			}
		};
		glfwSetKeyCallback(window, keyCallback);
		mouseCallback = new GLFWMouseButtonCallback() {

			@Override
			public void invoke(long window, int button, int action, int mods) {
				if (button == GLFW_MOUSE_BUTTON_LEFT && action != GLFW_REPEAT && !isFractalUpdating()) {
					glfwGetCursorPos(window, mouseX, mouseY);
					if (action == GLFW_PRESS) {
						x1 = mouseX.get();
						y1 = mouseY.get();
					} else {
						x2 = mouseX.get();
						y2 = mouseY.get();

						double sx = Math.min(x1, x2);
						double sy = Math.max(y1, y2);
						double ex = Math.max(x1, x2);
						double ey = Math.min(y1, y2);
						if (ex > ey) {
							ey = sy - Math.abs(ex - sx);
						} else {
							ex = sx + Math.abs(ey - sy);
						}
						context.getFractal().zoom(sx, sy, ex, ey, context.getWidth(), context.getHeight());
						updateFractal();
					}
					mouseX.flip();
					mouseY.flip();
				}
			}
		};
		glfwSetMouseButtonCallback(window, mouseCallback);

		glfwMakeContextCurrent(window);
		GL.createCapabilities();

		bbuffer = ByteBuffer.allocateDirect(getBufferSize() * 4).order(ByteOrder.nativeOrder());

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0.0f, context.getWidth(), context.getHeight(), 0.0f, 0.0f, 1.0f);
	}

	@Override
	protected void dispose() {
		context.dispose();
		keyCallback.free();
		mouseCallback.free();
		glfwDestroyWindow(window);
		glfwTerminate();
		errorCallback.free();
	}

	@Override
	protected void render(int[] data) {
		if (takeScreenshot) {
			synchronized (data) {
				screenshot(data.clone());
			}
			takeScreenshot = false;
		}

		synchronized (data) {
			bbuffer.asIntBuffer().put(data).flip();
		}
		glViewport(0, 0, context.getWidth(), context.getHeight());
		glClear(GL_COLOR_BUFFER_BIT);

		glDrawPixels(context.getWidth(), context.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuffer);

		glfwSwapBuffers(window);
		glfwPollEvents();
	}

	@Override
	protected void update() {
		if (glfwWindowShouldClose(window))
			stop();
	}

}
