
package integratedtoolkit.util;

import java.lang.Thread;
import java.util.concurrent.Semaphore;


public class ThreadPool {

	public static final int MAX_THREADS = 10;
	
	private int size;
	private String name;
	private Thread workerThreads[];
	private RequestDispatcher runObject;
	private RequestQueue queue;
	private Semaphore sem;
	
	
	public ThreadPool(int size, String name, RequestDispatcher runObject) {
		if (size > MAX_THREADS) size = MAX_THREADS;
		this.workerThreads = new Thread[size];
		this.size = size;
		this.name = name;
		
		this.runObject = runObject;
		this.runObject.setPool(this);
		
		this.queue = runObject.getQueue();
		
		this.sem = new Semaphore(size);
	}
	
	
	public void startThreads() throws Exception {
		int i = 0;
		for (Thread t : workerThreads) {
			t = new Thread(runObject);
			t.setName(name + " pool thread # " + i++);
			t.start();
		}
		
		sem.acquire(size);
	}
	
	
	@SuppressWarnings("unchecked")
	public void stopThreads() throws Exception {
		/* Empty queue to discard any pending requests
		 * and make threads finish
		 */
		synchronized (queue) {
			for (int i = 0; i < size; i++)
				queue.addToFront(null);
			queue.wakeUpAll();
		}
		
		// Wait until all threads have completed their last request
		sem.acquire(size);
	}
	
	
	public void threadEnd() {
		//System.out.println("Thread terminated " + Thread.currentThread().getName());
		sem.release();
	}
	
	
	public int getNumThreads() {
		return workerThreads.length;
	}
	
}
