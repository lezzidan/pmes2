
package api.matmul;

import java.io.*;

import worker.matmul.MatmulImpl;

import integratedtoolkit.api.IntegratedToolkit;
import integratedtoolkit.api.impl.IntegratedToolkitImpl;


public class Matmul {

	
	private final int MSIZE = 6;
	private final int BSIZE = 800;

	private String [][]_A;
	private String [][]_B;
	private String [][]_C;

	public void Run ()
	{
		// initialize arrays holding the acctual array names
		initialize_variables();
		
		/*try
		{
			fill_matrices();
		}
		catch ( IOException ioe )
		{
			ioe.printStackTrace();
			return;
		}*/
		
		IntegratedToolkit it = new IntegratedToolkitImpl();
		it.startIT();

		for (int i = 0; i < MSIZE; i++)
		{
			for (int j = 0; j < MSIZE; j++)
			{
				for (int k = 0; k < MSIZE; k++)
				{
					MatmulImpl.multiplyAccumulative( _C[i][j], _A[i][k], _B[k][j] );
				}
            }
        }
			
		// Stop IT definitively
		it.stopIT(true);
		
	}
	
	
	private void initialize_variables ()
	{
		_A = new String[MSIZE][MSIZE];
		_B = new String[MSIZE][MSIZE];
		_C = new String[MSIZE][MSIZE];
		for ( int i = 0; i < MSIZE; i ++ )
		{
			for ( int j = 0; j < MSIZE; j ++ )
			{
				_A[i][j] = "A." + i + "." + j;
				_B[i][j] = "B." + i + "." + j;
				_C[i][j] = "C." + i + "." + j;
			}
		}
	}

	private void fill_matrices ()
		throws FileNotFoundException, IOException
	{
		for ( char c = 'A'; c < 'D'; c++ )
		{
			for ( int i = 0; i < MSIZE; i++ )
			{
				for ( int j = 0; j < MSIZE; j++ )
				{
					String tmp = c + "." + i + "." + j;
					FileOutputStream fos = new FileOutputStream(tmp);
					for ( int ii = 0; ii < BSIZE; ii++ )
					{	
						for(int jj = 0; jj < BSIZE; jj ++)
						{
							if(c == 'C')
							{
								fos.write("0.0 ".getBytes());
							}
							else
							{
								fos.write("2.0 ".getBytes());
							}
						}
						fos.write("\n".getBytes());
					}
					fos.close();
				}
			}
		}
	}

	public static void main(String args[])
	{
		(new Matmul()).Run();
	}

}
