
package integratedtoolkit.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


public class Graph<K,T> {

	private Map<K,Node<T>> graph;
	
	
	public Graph() {
		graph = new TreeMap<K,Node<T>>();
	}
	
	
	public T get(K key) {
		Node<T> n = graph.get(key);
		if (n == null) return null;
		return n.getElement();
	}
	
	public Set<T> getPredecessors(K key) throws ElementNotFoundException {
		Node<T> n = graph.get(key);
		if (n == null) throw new ElementNotFoundException();
		return n.getPredecessors();
	}
	
	public Set<T> getSuccessors(K key) throws ElementNotFoundException {
		Node<T> n = graph.get(key);
		if (n == null) throw new ElementNotFoundException();
		return n.getSuccessors();
	}
	
	public Iterator<T> getIteratorOverPredecessors(K key) throws ElementNotFoundException {
		Node<T> n = graph.get(key);
		if (n == null) throw new ElementNotFoundException();
		return n.getIteratorOverPredecessors();
	}
	
	public Iterator<T> getIteratorOverSuccessors(K key) throws ElementNotFoundException {
		Node<T> n = graph.get(key);
		if (n == null) throw new ElementNotFoundException();
		return n.getIteratorOverSuccessors();
	}
	
	public int getSize() {
		return graph.size();
	}
	
	public boolean hasPredecessors(K key) throws ElementNotFoundException {
		return !this.getPredecessors(key).isEmpty();
	}
	
	public boolean hasSuccessors(K key) throws ElementNotFoundException {
		return !this.getSuccessors(key).isEmpty();
	}
	
	
	public void addNode(K key, T element) {
		graph.put(key, new Node<T>(element));
	}
	
	public void addEdge(K sourceKey, K destKey) throws ElementNotFoundException {
		Node<T> pred = graph.get(sourceKey);
		Node<T> succ = graph.get(destKey);
		
		if (pred == null || succ == null)
			throw new ElementNotFoundException("Cannot add the edge: predecessor and/or successor don't exist");
		
		pred.addSuccessor(succ.getElement());
		succ.addPredecessor(pred.getElement());
	}
	
	
	public T removeNode(K key) {
		Node<T> n = graph.remove(key);
		if (n != null) return n.getElement();
		return null;
	}
	
	public void removeEdge(K sourceKey, K destKey) throws ElementNotFoundException {
		Node<T> pred = graph.get(sourceKey);
		Node<T> succ = graph.get(destKey);
		
		if (pred == null || succ == null)
			throw new ElementNotFoundException("Cannot remove the edge: predecessor and/or successor don't exist");
		
		pred.removeSuccessor(succ.getElement());
		succ.removePredecessor(pred.getElement());
	}
	
	
	public void clear() {
		graph.clear();
	}
	
	
	private class Node<E> {
		
		// Node fields
		private E element;
		private TreeSet<E> predecessors;
		private TreeSet<E> successors;
		
		public Node(E element) {
			this.element = element;
			this.predecessors = new TreeSet<E>();
			this.successors = new TreeSet<E>();
		}
		
		public E getElement() {
			return element;
		}
		
		public Set<E> getPredecessors() {
			return predecessors;
		}
		
		public Set<E> getSuccessors() {
			return successors;
		}
		
		@SuppressWarnings("unchecked")
		public Iterator<E> getIteratorOverPredecessors() {
			return ((Set<E>)predecessors.clone()).iterator();
		}
		
		@SuppressWarnings("unchecked")
		public Iterator<E> getIteratorOverSuccessors() {
			return ((Set<E>)successors.clone()).iterator();
		}
		
		public void addPredecessor(E pred) {
			predecessors.add(pred);
		}
		
		public void addSuccessor(E succ) {
			successors.add(succ);
		}
		
		public void removePredecessor(E pred) {
			predecessors.remove(pred);
		}
		
		public void removeSuccessor(E succ) {
			successors.remove(succ);
		}
		
	}
}
