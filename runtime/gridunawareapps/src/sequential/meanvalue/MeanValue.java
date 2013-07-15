
package sequential.meanvalue;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import worker.meanvalue.MeanValueImpl;


public class MeanValue {

	public MeanValue( int loops )
	{
		File results = new File("results.txt");
		if ( results.exists() )
		{
			results.delete();
		}
		try {
			results.createNewFile();

		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			return;
		}

		for ( int i = 0; i < loops; i++ )
		{
			MeanValueImpl.genRandom("random.txt");
			MeanValueImpl.mean("random.txt", "results.txt");
		} 
		
		try
		{
			FileInputStream FINstream = new FileInputStream("results.txt");
			
			try
			{
				for ( int i = 0; i < loops; i++ )
				{	
					System.out.println("mean " + i + " : " + FINstream.read() );
				}
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			
			FINstream.close();
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
	
	private static void usage()
	{
			System.out.println("Usage: java MeanValue <number_of_iterations>");
	}
		
	public static void main(String args[])
	{
		if ( args.length != 1 )
		{
			MeanValue.usage();
		}
		else
		{
			try
			{
				new MeanValue( Integer.parseInt(args[0]) );
			}
			catch (NumberFormatException nfe)
			{
				System.out.println("Error commandline parameters " + nfe.getMessage() );
				MeanValue.usage();
			}
		}
	}
	
}
