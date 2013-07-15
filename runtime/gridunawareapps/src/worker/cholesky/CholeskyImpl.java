
package worker.cholesky;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class CholeskyImpl {

	public static void initialize(String filename) throws CholeskyAppException
	{
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(filename);
		}
		catch (FileNotFoundException fnfe)
		{
			throw new CholeskyAppException( fnfe.getMessage() );
		}
			
		try {
			for ( int i = 0; i < Block.BLOCK_SIZE; i++ )
			{	
				for(int j = 0; j < Block.BLOCK_SIZE; j++)
				{
					fos.write("0.0 ".getBytes());
				}
				fos.write("\n".getBytes());
			}
			fos.close();
		}
		catch (IOException ioe)
		{
			throw new CholeskyAppException( ioe.getMessage() );
		}		
	}
	
	
	public static void multiplyAccumulative(String f3, String f1, String f2) throws CholeskyAppException
	{
		Block a = new Block( f1 );
		Block b = new Block( f2 );
		Block c = new Block( f3 );
		
		c.multiplyAccum( a, b );
	
		c.blockToDisk( f3 );	
	}

	
	public static void substract(String f3, String f1, String f2) throws CholeskyAppException
	{	
		Block a = new Block( f1 );
		Block b = new Block( f2 );
		
		a.sub(b);

		a.blockToDisk( f3 );	
	}

	
	public static void cholesky(String f1) throws CholeskyAppException
	{
		Block a = new Block( f1 );
		a.cholesky();
		a.blockToDisk( f1 );
	}

	
	public static void choleskyDivision(String f2, String f1) throws CholeskyAppException
	{
		Block a = new Block( f2 );
		Block b = new Block( f1 );
		Block c = new Block();
		
		b.inverse();
		
		a.reverseColumns();
	
		c.multiplyAccum( a, b );
		
		c.blockToDisk( f2 );	
	}

}
