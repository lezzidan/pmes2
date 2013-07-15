
package integratedtoolkit.util;


public abstract class RequestDispatcher<T> implements Runnable {
	
	protected ThreadPool pool;
	protected RequestQueue<T> queue;
	
	public RequestDispatcher(RequestQueue<T> queue) {
		this.queue = queue;
		this.pool = null;
	}
	
	public void setPool(ThreadPool pool) {
		this.pool = pool;
	}
	
	public void run() {
		processRequests();
		if (pool != null) pool.threadEnd();
	}
	
	public RequestQueue<T> getQueue() {
		return queue;
	}
	
	protected abstract void processRequests();
	
}
