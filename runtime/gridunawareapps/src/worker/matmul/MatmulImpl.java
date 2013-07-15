
package worker.matmul;

//import java.lang.Compiler;


public class MatmulImpl
{
	public static void multiplyAccumulative( String f3, String f1, String f2 )
	{
		Block a = new Block( f1 );
		Block b = new Block( f2 );
		Block c = new Block( f3 );
		
		//Compiler.disable();	
		c.multiplyAccum( a, b );
		//Compiler.enable();

		try
		{
			c.blockToDisk( f3 );
		}
		catch ( MatmulAppException ce )
		{
			System.err.println( ce.getMessage() );
			return;
		}
	}
}
