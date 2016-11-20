package com.att.aft.dme2.iterator.bean;

public interface EndpointIteratorMXBean {
	/** Restores the original collection of EndpointReferences that was set on Initialization. Also, resets the iterator position so that it will start 
	 * from the beginning of the collection.*/
	public void resetIterator();
	
	/** Returns the original total number of elements contained in EndpointIterator. */
	public int getTotalNumberOfElements();
	
	/** Returns the number of active elements in the EndpointIterator. These are elements that have not been marked stale,
	 * or removed from the Iterator. */
	public int getNumberOfActiveElements();
	
	/** Returns the number of elements that have been marked stale in the EndpointIterator. */
	public int getNumberOfStaleElements();
	
	/** Returns the number of elements that have been removed from the EndpointIterator by calling the remove() method. */
	public int getNumberOfRemovedElements();
	
	/** Returns the current index position of the EndpointIterator for active elements. */
	public int getCurrentIteratorActiveIndexPosition();
	
	/** Returns the current index position of the EndpointIterator for stale elements. */
	public int getCurrentIteratorStaleIndexPosition();
	
	/** Returns a String representation of the current Endpoint selected by DME2EndpointIterator. */
	public String getCurrentDME2Endpoint();
	
	/** Returns the distance band of the corresponding Endpoint currently selected by DME2EndpointIterator. */
	public double getCurrentDME2EndpointDistanceBand();
	
	/** Returns the sequence number of the corresponding Endpoint currently selected by DME2EndpointIterator. */
	public int getCurrentDME2EndpointSequence();
	
	/** Returns the RouteOffer of the corresponding Endpoint currently selected by DME2EndpointIterator. */
	public String getCurrentDME2EndpointRouteOffer();
	
	/** Removes all stale iterator elements from the StaleEndpointCache. */
	public void removeAllStaleIteratorElements();

	/** Removes the stale iterator element from the StaleEndpointCache with the provided service name. */
	public void removeStaleIteratorElement(String serviceName);
	
	/** Determines if all Iterator elements have been marked stale. */
	public boolean isAllElementsStale();
	
	/** Determines if all Iterator elements have been exhausted.*/
	public boolean isAllElementsExhausted();
}