
package sequential.matmul;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import worker.matmul.MatmulImpl;


public class Matmul
{
	private final int MSIZE = 8;
	private final int BSIZE = 800;

	private String [][]_A;
	private String [][]_B;
	private String [][]_C;

	public void Run ()
	{
		// initialize arrays holding the actual array names
		initializeVariables();
		/*try
		{
			fillMatrices();
		}
		catch ( IOException ioe )
		{
			ioe.printStackTrace();
			return;
		}*/

		System.out.println("Begin calculation");

		long first, last;
		
		first = System.currentTimeMillis();

		for (int i = 0; i < MSIZE; i++)
		{
			for (int j = 0; j < MSIZE; j++)
			{
				for (int k = 0; k < MSIZE; k++)
				{
					long ini, fi;
					ini = System.currentTimeMillis(); 
					MatmulImpl.multiplyAccumulative( _C[i][j], _A[i][k], _B[k][j] );
					fi = System.currentTimeMillis();
					System.out.println("TASK: " + ((fi - ini) / 1000) + " seconds\n");
				}
            }
		}
		
		last = System.currentTimeMillis();

		System.out.println("\n\n######### EXECUTION STATS #########");
                System.out.println("\t - TOTAL: " + ((last - first) / 1000) + " seconds\n");
	
	}

	private void initializeVariables ()
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

	private void fillMatrices ()
		throws FileNotFoundException, IOException
	{
		int initVal = 1325;
		for ( char c = 'A'; c < 'D'; c++ )
		{
			//initVal = (initVal * (double)c) % 65536;
			for ( int i = 0; i < MSIZE; i++ )
			{
				//initVal = (initVal * (i * 111)) % 65536;
				for ( int j = 0; j < MSIZE; j++ )
				{
					//initVal = (initVal * (j * 3127)) % 65536;
					String tmp = c + "." + i + "." + j;
					FileOutputStream fos = new FileOutputStream(tmp);
					for ( int ii = 0; ii < BSIZE; ii++ )
					{
						//initVal = (initVal * (ii * 127)) % 65536;
						for(int jj = 0; jj < BSIZE; jj ++)
						{
							if(c == 'C')
							{
								fos.write("0.0 ".getBytes());
							}
							else
							{
								//initVal = (initVal * (j * 31227)) % 65536;
								initVal = (3125 * initVal) % 65536;
                                                                double cellValue = (double)((initVal - 32768.0) / 16384.0);
								fos.write((cellValue + " ").getBytes());
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

