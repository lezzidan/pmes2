
package worker.meanvalue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Random;


public class MeanValueImpl {

	private static int RANDOM_RANGE 			= 50;
	private static int MAX_RANDOM_NUMBERS		= 100;

	public static void genRandom( String rnumber_file )
	{
		try
		{
			Random generator = new Random();
			FileOutputStream FOUTstream = new FileOutputStream(rnumber_file);
			try
			{
				for ( int i = 0; i < MAX_RANDOM_NUMBERS; i++ )
				{
					FOUTstream.write(generator.nextInt(RANDOM_RANGE));
				}
			}
			catch (IOException ioe) 
			{
				ioe.printStackTrace();
			}
			
			FOUTstream.close();
		}
		catch(FileNotFoundException fnfe)
		{
			fnfe.printStackTrace();
		}
		catch(IOException fioe)
		{
			fioe.printStackTrace();
		}
	}
	
	public static void mean( String rnumber_file, String results_file )
	{
		int sum = 0;
		try
		{
			FileInputStream FINstream = new FileInputStream(rnumber_file);
			for ( int i = 0; i < MAX_RANDOM_NUMBERS; i++ )
			{
				sum += (int)FINstream.read();
			}
			FINstream.close();

			FileOutputStream FAstream = new FileOutputStream(results_file, true); 
			FAstream.write((int)(sum/MAX_RANDOM_NUMBERS));
			FAstream.close();
		}
		catch(FileNotFoundException fnfe)
		{
			fnfe.printStackTrace();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
}
