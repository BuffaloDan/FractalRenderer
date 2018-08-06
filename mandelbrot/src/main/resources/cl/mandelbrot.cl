kernel void mandelbrot(global write_only int *data, double sx, double sy, double w, double h, int width, int height, 
						int maxIterations, global uint *colorMap, int colorMapSize) {
    unsigned int x = get_global_id(0);
    unsigned int y = get_global_id(1);

    double cr, ci, zr, zi, zr2, zi2;
	cr = sx + (w / width) * x;
    ci = sy + (h / height) * y;

	zr = 0;
	zi = 0;
	double magnitudeSquared = 0;
	int iteration = 0;
	while (magnitudeSquared < (1<<16) && iteration < maxIterations) {
        zr2 = zr * zr;
		zi2 = zi * zi;
        zi = 2 * zr * zi + ci;
        zr = zr2 - zi2 + cr;
        magnitudeSquared = zr2 + zi2;
        iteration++;
    }
	if (iteration == maxIterations)  {
		data[y * width + x] = 0;
	} else {
		double alpha = ((double)iteration) / maxIterations;
        int colorIndex = (int)(alpha * colorMapSize);
		data[y * width + x] = colorMap[colorIndex];
	}
}