import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
public class Parallelbrot 
{
	public static int escapes(double cr, double ci) 
	{
	    int steps = 0;
	    double zr = 0;
	    double zi = 0;
	    double zrtmp;
	  
	    for(int i = 0; i < 256; i++) 
	    {
	      zrtmp = zr*zr - zi*zi + cr;
	      zi = 2*zr*zi + ci;
	      zr = zrtmp;
	      
	      Double d = zr;
	      
	      if (d.isInfinite() || d.isNaN()) 
	      {
	        steps = i;
	        break;
	      }
	    }
	    return steps;
	  }
	
	public static void setPixelColor(double px, double py, int z, BufferedImage bi, int resolution) 
	{		
		// translate to Image y-ordinate, length of [-2, 2]:
		int py_scr = (int) ( py * (resolution  / 0.7) ); 
		// translate to Image x-abscissa, length of [-2, 2]:
		int px_scr = (int) ( px  * (resolution  / 0.7) ); 
		
		// color
	    if (z <= 14) {
			bi.setRGB(px_scr, py_scr, 0x000000);
		} else if (z == 15) {
			bi.setRGB(px_scr, py_scr, 0x0E1B00);
		} else if (z == 16) {
			bi.setRGB(px_scr, py_scr, 0x122200);
		} else if (z == 17) {
			bi.setRGB(px_scr, py_scr, 0x162A00);
		} else if (z == 18) {
			bi.setRGB(px_scr, py_scr, 0x1B3500);
		} else if (z == 19) {
			bi.setRGB(px_scr, py_scr, 0x224200);
		} else if (z == 20) {//==
			bi.setRGB(px_scr, py_scr, 0x2A5300);
		} else if (20 < z && z <= 30) {
			bi.setRGB(px_scr, py_scr, 0x264C00);
		} else if (30 < z && z <= 40) {
			bi.setRGB(px_scr, py_scr, 0x336600);
		} else if (40 < z && z <= 50) {
			bi.setRGB(px_scr, py_scr, 0x4C9900);
		} else if (50 < z && z <= 100) {
			bi.setRGB(px_scr, py_scr, 0xFFB424);
		} else if (100 < z && z <= 150) {
			bi.setRGB(px_scr, py_scr, 0xFFC350);
		} else if (150 < z && z <= 200) {
			bi.setRGB(px_scr, py_scr, 0xFFCF73);
		} else if (200 < z && z <= 256) {
			bi.setRGB(px_scr, py_scr, 0xFFD98F);
		}
	}
	
	public static class Mandelbrot implements Runnable
	{
		double startY;
		double endY;
		BufferedImage bi;
		int resolution;
		
		//constructor:
		public Mandelbrot(double _startY, double _endY, BufferedImage _bi, int _resolution)
		{
			this.startY = _startY;
			this.endY = _endY;
			this.bi = _bi;
			this.resolution = _resolution;
		}
		
		public void run()
		{
			double ty = startY  / resolution;
			
			for (double i = startY; i < endY; i++) //traversing by Y
			{
				double py = 0.699999*ty; //0.699999 instead of 0.7 because artifacts appear with 0.7
				double tx = 1.2 / resolution ;//*1.2 instead of 1.0 to remove artifacts
	
				for (int j = 0; j < resolution ; j++) //traversing by X
				{
					double px = -0 + 0.7 * tx;
					int z = escapes(px, py);
					setPixelColor(px, py, z, bi, resolution);
					tx += 1.0/(resolution);
				}
				ty = ty + 1.0/(resolution);
			}
		}
	};
	
	public static void main(String[] args) throws InterruptedException
	{
		int RESOLUTION = 4096;
		int THREADS = Integer.parseInt(args[0]);
		int GRANULARITY = Integer.parseInt(args[1]);
		
		Instant start = Instant.now();
		PrintWriter out = new PrintWriter(System.out);
		
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(THREADS, THREADS, 10, TimeUnit.SECONDS, queue);
		
		BufferedImage bi = new BufferedImage(RESOLUTION + 1, RESOLUTION + 1, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = bi.createGraphics();
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, RESOLUTION + 1, RESOLUTION + 1);
		
		for (int thread = 1; thread < THREADS * GRANULARITY; thread++)
		{
			double startY = ((RESOLUTION + 1) / (THREADS * GRANULARITY)) * thread;
			double endY = ((RESOLUTION + 1) / (THREADS * GRANULARITY) ) * (thread + 1);
			executor.execute(new Mandelbrot(startY, endY, bi, RESOLUTION));
		}
		//starting Mandelbrot calculation on current thread too:
		double startY = 0;
		double endY = (RESOLUTION + 1) / (THREADS * GRANULARITY);
		new Mandelbrot(startY, endY, bi, RESOLUTION).run();
		
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		
		try 
		{
			ImageIO.write(bi, "PNG", new File("mandelbrotIMG.png"));
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		out.println("Took " + start.until(Instant.now(), ChronoUnit.MILLIS) + "ms");
		out.flush();
		out.close();
	}
}