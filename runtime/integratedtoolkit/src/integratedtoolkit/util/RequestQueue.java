
package integratedtoolkit.util;

import java.util.LinkedList;


public class RequestQueue<T> {

	LinkedList<T> queue;
	int waiting;
	
	
	public RequestQueue() {
		queue = new LinkedList<T>();
		waiting = 0;
	}
	
	public synchronized void enqueue(T request) {
		queue.add(request);
		notify();
	}
	
	public synchronized T dequeue() {
		while (queue.size() == 0) {
			waiting++;
			try {
				wait();
			}
			catch (InterruptedException e) {
				return null;
			}
			waiting--;
		}
		return queue.poll();
	}

	public synchronized void addToFront(T request) {
        queue.addFirst(request);
	}
	
	public synchronized int getNumRequests() {
		return queue.size();
	}
	
	public synchronized boolean isEmpty() {
		return queue.size() == 0;
	}
	
	public synchronized int getWaiting() {
		return waiting;
	}
	
	public synchronized void clear() {
		queue.clear();
	}
	
	public void wakeUpAll() {
		notifyAll();
	}

	public synchronized T getValueof(Integer i){
                return queue.get(i);
        }
	
}
