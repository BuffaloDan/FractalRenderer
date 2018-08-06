package de.buffalodan.fractal.mandelbrot;

import de.buffalodan.fractal.core.Context;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MandelbrotCLFractal extends MandelbrotFractal {

    private final static String PROGRAM_PATH = "cl/mandelbrot.cl";

    private long clContext;
    private long clQueue;
    private long clProgram;
    private long clKernel;

    private IntBuffer dataBuffer;
    private long clBuffer;

    private long clColorMap;
    private int colorMapsize = 32*2*4;

    @Override
    public void init(Context context) {
        super.init(context);
        //dataBuffer = MemoryUtil.memAllocInt(context.getBufferSize());
        dataBuffer = BufferUtils.createIntBuffer(context.getBufferSize());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer err = stack.mallocInt(1);
            clBuffer = CL10.clCreateBuffer(clContext, CL10.CL_MEM_WRITE_ONLY, context.getBufferSize() << 2, err);
            checkCLError(err);

            IntBuffer colorMapBuffer = BufferUtils.createIntBuffer(32 * 2);
            initColorMap(colorMapBuffer, 32, Color.BLUE, Color.GREEN, Color.RED);

            clColorMap = CL10.clCreateBuffer(clContext, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, colorMapBuffer, err);
            checkCLError(err);
        }

        maxIterations = 1000;
    }

    private long createCLProgram(String path, long device, MemoryStack stack) throws IOException {
        URL resource = MandelbrotCLFractal.class.getResource(path);
        try (ReadableByteChannel channel = Channels.newChannel(resource.openStream())) {
            int length = resource.openConnection().getContentLength();
            ByteBuffer source = stack.malloc(length + 1);
            int read = channel.read(source);
            source.put((byte) MemoryUtil.NULL);
            source.flip();
            assert length != read;
            PointerBuffer sources = stack.mallocPointer(1);
            sources.put(0, source);
            IntBuffer err = stack.mallocInt(1);
            long ret = CL10.clCreateProgramWithSource(clContext, sources, null, err);
            checkCLError(err);
            try {
                checkCLError(CL10.clBuildProgram(ret, device, "", null, MemoryUtil.NULL));
            } catch (RuntimeException e) {
                PointerBuffer pb = stack.mallocPointer(1);
                CL10.clGetProgramBuildInfo(ret, device, CL10.CL_PROGRAM_BUILD_LOG, (ByteBuffer) null, pb);
                int s = (int) pb.get();
                System.out.println(s);
                ByteBuffer bb = stack.malloc(s);
                CL10.clGetProgramBuildInfo(ret, device, CL10.CL_PROGRAM_BUILD_LOG, bb, null);
                System.out.println(MemoryUtil.memUTF8(bb, s - 1));
                throw e;
            }
            clKernel = CL10.clCreateKernel(ret, "mandelbrot", err);
            checkCLError(err);
            return ret;
        }
    }

    public MandelbrotCLFractal() {
        Scanner in = new Scanner(System.in);
        List<Long> platforms, devices;
        System.out.println("Platforms:");

        //Das ist ein richtiger Hurensohn
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pi = stack.mallocInt(1);
            checkCLError(CL10.clGetPlatformIDs(null, pi));
            if (pi.get(0) == 0) {
                throw new RuntimeException("No OpenCL platforms found.");
            }
            PointerBuffer platformIDs = stack.mallocPointer(pi.get(0));
            checkCLError(CL10.clGetPlatformIDs(platformIDs, (IntBuffer) null));

            platforms = new ArrayList<>(platformIDs.capacity());
            PointerBuffer pb = stack.mallocPointer(1);

            for (int i = 0; i < platformIDs.capacity(); i++) {
                long platform = platformIDs.get(i);
                CL10.clGetPlatformInfo(platform, CL10.CL_PLATFORM_NAME, (ByteBuffer) null, pb);
                try (MemoryStack ms = MemoryStack.stackPush()) {
                    int bytes = (int) pb.get(0);
                    ByteBuffer buffer = ms.malloc(bytes);
                    CL10.clGetPlatformInfo(platform, CL10.CL_PLATFORM_NAME, buffer, null);
                    System.out.println(i + " - " + MemoryUtil.memUTF8(buffer, bytes - 1));
                    platforms.add(platform);
                }
            }
        }
        System.out.print("Select Platform (0-" + (platforms.size() - 1) + "):");
        long selectedPlatform = platforms.get(in.nextInt());
        System.out.println();
        System.out.println("Devices:");

        //Und das auch
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pi = stack.mallocInt(1);

            CL10.clGetDeviceIDs(selectedPlatform, CL10.CL_DEVICE_TYPE_ALL, null, pi);
            PointerBuffer deviceIDs = stack.mallocPointer(pi.get(0));
            CL10.clGetDeviceIDs(selectedPlatform, CL10.CL_DEVICE_TYPE_ALL, deviceIDs, (IntBuffer) null);

            devices = new ArrayList<>(deviceIDs.capacity());
            PointerBuffer pb = stack.mallocPointer(1);

            for (int i = 0; i < deviceIDs.capacity(); i++) {
                long device = deviceIDs.get(i);
                CL10.clGetDeviceInfo(device, CL10.CL_DEVICE_NAME, (ByteBuffer) null, pb);
                try (MemoryStack ms = MemoryStack.stackPush()) {
                    int bytes = (int) pb.get(0);
                    ByteBuffer buffer = ms.malloc(bytes);
                    CL10.clGetDeviceInfo(device, CL10.CL_DEVICE_NAME, buffer, null);
                    System.out.println(i + " - " + MemoryUtil.memUTF8(buffer, bytes - 1));
                    devices.add(device);
                }
            }
        }
        long selectedDevice = devices.get(0);
        if (devices.size() > 1) {
            System.out.print("Select Device (0-" + (devices.size() - 1) + "):");
            selectedDevice = devices.get(in.nextInt());
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer props = stack.mallocPointer(3);
            props.put(CL10.CL_CONTEXT_PLATFORM).put(selectedPlatform);
            props.put(MemoryUtil.NULL);
            props.flip();
            clContext = CL10.clCreateContext(props, selectedDevice, null, MemoryUtil.NULL, null);
            clQueue = CL10.clCreateCommandQueue(clContext, selectedDevice, MemoryUtil.NULL, (IntBuffer) null);

            clProgram = createCLProgram("/cl/mandelbrot.cl", selectedDevice, stack);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkCLError(IntBuffer errcode) {
        checkCLError(errcode.get(errcode.position()));
    }

    public static void checkCLError(int errcode) {
        if (errcode != CL10.CL_SUCCESS) {
            throw new RuntimeException(String.format("OpenCL error [%d]", errcode));
        }
    }

    @Override
    public void createFractal(int[] data, int startX, int startY, int endX, int endY, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int err;
            dataBuffer.clear();
            //dataBuffer.put(data);
            //dataBuffer.flip();
            //int err = CL10.clEnqueueWriteBuffer(clQueue, clBuffer, true, 0, dataBuffer, null, null);
            //checkCLError(err);
            err = CL10.clSetKernelArg1p(clKernel, 0, clBuffer);
            checkCLError(err);
            err = CL10.clSetKernelArg1d(clKernel, 1, sx);
            checkCLError(err);
            err = CL10.clSetKernelArg1d(clKernel, 2, sy);
            checkCLError(err);
            err = CL10.clSetKernelArg1d(clKernel, 3, w);
            checkCLError(err);
            err = CL10.clSetKernelArg1d(clKernel, 4, h);
            checkCLError(err);
            err = CL10.clSetKernelArg1i(clKernel, 5, width);
            checkCLError(err);
            err = CL10.clSetKernelArg1i(clKernel, 6, height);
            checkCLError(err);
            err = CL10.clSetKernelArg1i(clKernel, 7, maxIterations);
            checkCLError(err);
            err = CL10.clSetKernelArg1p(clKernel, 8, clColorMap);
            checkCLError(err);
            err = CL10.clSetKernelArg1i(clKernel, 9, colorMapsize);
            checkCLError(err);



            PointerBuffer workSize = stack.mallocPointer(2);
            workSize.put(0, width);
            workSize.put(1, height);
            err = CL10.clEnqueueNDRangeKernel(clQueue, clKernel, 2, null, workSize, null, null, null);
            checkCLError(err);
            err = CL10.clFinish(clQueue);
            checkCLError(err);

            err = CL10.clEnqueueReadBuffer(clQueue, clBuffer, true, 0, dataBuffer, null, null);
            checkCLError(err);
        }
        dataBuffer.get(data);
    }

    @Override
    public void dispose() {
        if (clBuffer != MemoryUtil.NULL) CL10.clReleaseMemObject(clBuffer);
        if (clColorMap != MemoryUtil.NULL) CL10.clReleaseMemObject(clColorMap);
        //if (dataBuffer != null) MemoryUtil.memFree(dataBuffer);
        if (clKernel != MemoryUtil.NULL) CL10.clReleaseKernel(clKernel);
        if (clProgram != MemoryUtil.NULL) CL10.clReleaseProgram(clProgram);
        if (clQueue != MemoryUtil.NULL) CL10.clReleaseCommandQueue(clQueue);
        if (clContext != MemoryUtil.NULL) CL10.clReleaseContext(clContext);
    }

    private void initColorMap(IntBuffer colorMap, int stepSize, Color... colors) {
        for (int n = 0; n < colors.length - 1; n++) {
            Color color = colors[n];

            int r0 = color.getRed();
            int g0 = color.getGreen();
            int b0 = color.getBlue();

            color = colors[n + 1];

            int r1 = color.getRed();
            int g1 = color.getGreen();
            int b1 = color.getBlue();

            int deltaR = r1 - r0;
            int deltaG = g1 - g0;
            int deltaB = b1 - b0;

            for (int step = 0; step < stepSize; step++) {
                float alpha = (float)step / (stepSize - 1);

                int r = (int)(r0 + alpha * deltaR);
                int g = (int)(g0 + alpha * deltaG);
                int b = (int)(b0 + alpha * deltaB);

                colorMap.put((r << 0) | (g << 8) | (b << 16));
            }
        }
        colorMap.flip();
    }
}
