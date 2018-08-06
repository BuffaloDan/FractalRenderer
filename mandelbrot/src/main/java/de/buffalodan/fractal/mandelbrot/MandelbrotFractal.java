package de.buffalodan.fractal.mandelbrot;

import java.awt.Color;

import de.buffalodan.fractal.core.Context;
import de.buffalodan.fractal.core.Fractal;

public class MandelbrotFractal implements Fractal {

    protected int maxIterations = 50;
    protected double threshold = 4;

    protected double sx = -2, sy = -2, w = 4, h = 4;

    @Override
    public void init(Context context) {
    }

    @Override
    public void zoom(double startX, double startY, double endX, double endY, int width, int height) {
        double rx = startX / width;
        double ry = (height - startY - 1) / height;
        sx += rx * w;
        sy += ry * h;
        double zoomX = endX / width - rx;
        double zoomY = (height - endY - 1) / height - ry;
        w *= zoomX;
        h *= zoomY;

        System.out.println("New Configuration:");
        System.out.println("X: " + sx + "   Y: " + sy);
        System.out.println("W: " + w + "   H: " + h);
        System.out.println("MaxIterations: " + maxIterations);
        System.out.println();
    }

    @Override
    public void reset() {
        sx = -2;
        sy = -2;
        w = 4;
        h = 4;
    }

    @Override
    public void createFractal(byte[] data, int width, int height) {
        createFractal(data, 0, 0, width, height, width, height);
    }

    @Override
    public int getMaxIterations() {
        return maxIterations;
    }

    @Override
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public void createFractal(int[] data, int startX, int startY, int endX, int endY, int width, int height) {
        int i;
        double cr, ci, zr, zi;
        double[][][] tmp = new double[endX - startX][endY - startY][3];
        for (i = 0; i < maxIterations; i++) {
            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    if (tmp[x - startX][y - startY][2] != 0)
                        continue;

                    cr = sx + (w / width) * x;
                    ci = sy + (h / height) * y;
                    if (i == 0) {
                        zr = cr;
                        zi = ci;
                    } else {
                        zr = tmp[x - startX][y - startY][0];
                        zi = tmp[x - startX][y - startY][1];
                    }
                    double zr2 = zr * zr, zi2 = zi * zi;
                    if (zr2 + zi2 > threshold) {
                        tmp[x - startX][y - startY][2] = -1;
                    } else {
                        zi = 2 * zr * zi + ci;
                        zr = zr * zr - zi2 + cr;
                        tmp[x - startX][y - startY][0] = zr;
                        tmp[x - startX][y - startY][1] = zi;
                    }

                    synchronized (data) {
                        double sc = smooth(i, zr2, zi2) / maxIterations;
                        int col = Color.HSBtoRGB((float) (0.95f + 10f * sc), 0.8f, 1);
                        data[y * height + x] = col;
                    }
                }
            }
        }
    }

    public void createFractal(byte[] data, int startX, int startY, int endX, int endY, int width, int height) {
        int i;
        double cr, ci, zr, zi;
        double[][][] tmp = new double[endX - startX][endY - startY][3];
        for (i = 0; i < maxIterations; i++) {
            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    if (tmp[x - startX][y - startY][2] != 0)
                        continue;

                    cr = sx + (w / width) * x;
                    ci = sy + (h / height) * y;
                    if (i == 0) {
                        zr = cr;
                        zi = ci;
                    } else {
                        zr = tmp[x - startX][y - startY][0];
                        zi = tmp[x - startX][y - startY][1];
                    }
                    double zr2 = zr * zr, zi2 = zi * zi;
                    if (zr2 + zi2 > threshold) {
                        tmp[x - startX][y - startY][2] = -1;
                    } else {
                        zi = 2 * zr * zi + ci;
                        zr = zr * zr - zi2 + cr;
                        tmp[x - startX][y - startY][0] = zr;
                        tmp[x - startX][y - startY][1] = zi;
                    }

                    synchronized (data) {
                        double sc = smooth(i, zr2, zi2) / maxIterations;
                        int col = Color.HSBtoRGB((float) (0.95f + 10f * sc), 0.6f, 1);
                        data[y * height * 4 + x * 4] = (byte) ((col) & 0xFF);
                        data[y * height * 4 + x * 4 + 1] = (byte) ((col >> 8) & 0xFF);
                        data[y * height * 4 + x * 4 + 2] = (byte) ((col >> 16) & 0xFF);
                        data[y * height * 4 + x * 4 + 3] = (byte) 255;
                    }
                }
            }
        }
    }

    private double smooth(int i, double zr2, double zi2) {
        return i + 1d - Math.log(Math.log(Math.sqrt(zr2 + zi2))) / Math.log(2);
    }

}
