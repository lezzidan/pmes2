
package api.meanvalue;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import worker.meanvalue.MeanValueImpl;

import integratedtoolkit.api.IntegratedToolkit;
import integratedtoolkit.api.impl.IntegratedToolkitImpl;

import integratedtoolkit.api.IntegratedToolkit.*;


public class MeanValue
{

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
		
		// Create and deploy the IT
		IntegratedToolkit it = new IntegratedToolkitImpl();

		// Start the IT
		it.startIT();
		
		for ( int i = 0; i < loops; i++ )
		{
			MeanValueImpl.genRandom("random.txt");
			MeanValueImpl.mean("random.txt", "results.txt");
		}
		
		// Open a file
		String fileName = it.openFile("results.txt", OpenMode.READ);
		
		try
		{
			// Use the file name returned by IT_Open to work with the file
			FileInputStream FINstream = new FileInputStream(fileName);
			
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
		
		// Stop the IT (and terminate it)
		it.stopIT(true);
		
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
