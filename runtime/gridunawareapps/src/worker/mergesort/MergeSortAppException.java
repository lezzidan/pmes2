
package worker.mergesort;


public class MergeSortAppException extends Exception {
	
	public MergeSortAppException() {
		super("unknown");
	}
	
	public MergeSortAppException( String _s ) {
		super(_s);
	}
	
}