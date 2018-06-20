package de.buffalodan.fractal.app;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.opengl.GL;

import de.buffalodan.fractal.core.Context;
import de.buffalodan.fractal.core.FractalRenderer;
import de.buffalodan.fractal.mandelbrot.MandelbrotFractal;

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
		Context desktopContext = new Context(800, 800, new MandelbrotFractal());
		new DesktopApp(desktopContext).start();
	}

	private void screenshot() {
		glReadBuffer(GL_FRONT);
		int width = context.getWidth();
		int height= context.getHeight();
		int bpp = 4; // Assuming a 32-bit display with a byte each for red, green, blue, and alpha.
		ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
		glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer );
		
		File file = new File("test.png"); // The file to save to.
		String format = "PNG"; // Example: "PNG" or "JPG"
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		   
		for(int x = 0; x < width; x++) 
		{
		    for(int y = 0; y < height; y++)
		    {
		        int i = (x + (width * y)) * bpp;
		        int r = buffer.get(i) & 0xFF;
		        int g = buffer.get(i + 1) & 0xFF;
		        int b = buffer.get(i + 2) & 0xFF;
		        image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
		    }
		}
		   
		try {
		    ImageIO.write(image, format, file);
		} catch (IOException e) { e.printStackTrace(); }
	}

	@Override
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

		bbuffer = ByteBuffer.allocateDirect(getBufferSize()).order(ByteOrder.nativeOrder());

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0.0f, context.getWidth(), context.getHeight(), 0.0f, 0.0f, 1.0f);
	}

	@Override
	protected void dispose() {
		keyCallback.free();
		glfwDestroyWindow(window);
		glfwTerminate();
		errorCallback.free();
	}

	@Override
	protected void render(byte[] data) {
		if (takeScreenshot) {
			screenshot();
			takeScreenshot = false;
		}

		synchronized (data) {
			bbuffer.put(data).flip();
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
