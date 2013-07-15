
package worker.cholesky;


public class CholeskyAppException extends Exception {
	
	public CholeskyAppException() {
		super("unknown");
	}
	
	public CholeskyAppException( String _s ) {
		super(_s);
	}
	
}
