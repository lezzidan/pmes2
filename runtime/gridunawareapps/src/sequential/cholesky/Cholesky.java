
package sequential.cholesky;

import worker.cholesky.CholeskyAppException;
import worker.cholesky.CholeskyImpl;


public class Cholesky {

	private static String [][]_A;
	private static String [][]_L;
			
	
	public static void main(String[] args) {
		
		if ( args.length != 1 )
		{
			System.out.println("Usage: java Cholesky <matrix_dimension>\n");
			return;
		}
		
		int MSIZE = Integer.parseInt(args[0]);
		String TMP = "tmp.txt";
		
		_A = new String[MSIZE][MSIZE];
		_L = new String[MSIZE][MSIZE];
		
		for ( int i = 0; i < MSIZE; i ++ )
		{
			for ( int j = 0; j < MSIZE; j ++ )
			{
				_A[i][j] = "A." + i + "." + j;
				_L[i][j] = "L." + i + "." + j;
				try {
					CholeskyImpl.initialize(_L[i][j]);
				}
				catch (CholeskyAppException e) {
					e.printStackTrace();
					return;
				}
			}
		}
		
		try {
			for (int j = 0; j < MSIZE; j++)
			{
				CholeskyImpl.initialize(TMP);
				
				for (int i = 0; i < j; i++)
					CholeskyImpl.multiplyAccumulative(TMP, _L[j][i], _L[j][i]);
				
				CholeskyImpl.substract(_L[j][j], _A[j][j], TMP);
						
				for (int i = j+1; i < MSIZE; i++)
				{
					CholeskyImpl.initialize(TMP);
					
					for (int k = 0; k < j; k++)
						CholeskyImpl.multiplyAccumulative(TMP, _L[i][k], _L[j][k]);
					
					CholeskyImpl.substract(_L[i][j], _A[i][j], TMP);
				}
				
				CholeskyImpl.cholesky(_L[j][j]);
			
				for (int i = j+1; i < MSIZE; i++)
					CholeskyImpl.choleskyDivision(_L[i][j], _L[j][j]);
			}
		}
		catch (CholeskyAppException e) {
			e.printStackTrace();
			return;
		}
		
	}
	
}
