package com.att.aft.dme2.iterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.iterator.domain.DME2EndpointReference;
import com.att.aft.dme2.iterator.domain.DME2RouteOffer;
import com.att.aft.dme2.iterator.domain.IteratorCreatingAttributes;
import com.att.aft.dme2.iterator.helper.StaleProcessor;
import com.att.aft.dme2.iterator.metrics.DefaultEndpointIteratorMetricsCollection;
import com.att.aft.dme2.iterator.service.DME2BaseEndpointIterator;
import com.att.aft.dme2.logging.LogMessage;
import com.att.aft.dme2.logging.Logger;
import com.att.aft.dme2.logging.LoggerFactory;
import com.att.aft.dme2.manager.registry.DME2Endpoint;
import com.att.aft.dme2.util.DME2Constants;

public class DME2EndpointIterator extends DefaultEndpointIteratorMetricsCollection implements DME2BaseEndpointIterator {
  private static final Logger LOGGER = LoggerFactory.getLogger( DME2EndpointIterator.class.getName() );
  private static final Logger FAILBACK_LOGGER = LoggerFactory.getLogger( "com.att.aft.dme2.events.failbackLogger" );
  private final boolean failbackLoggingEnabled = true;// DME2Constants.DME2_EXCHANGE_ENABLE_FAILOVER_LOGGING;
  private String routeOffersTried;
  private DME2Manager manager;
  // private List<OrderedEndpointHolder> endpointHolders = null;
  private String queryParamMinActiveEndPoint;

  /**
   * The orginal and unmodifiable list of EndpointReferences that was set on this DME2EndpointIterator object on
   * initialization.
   */
  private final List<DME2EndpointReference> originalOrderedEndpointHolders;

  /**
   * List of EndpointReferences that will be used for iteration.
   */
  private List<DME2EndpointReference> orderedEndpointHolders;

  /**
   * A list of all stale EndpointReferences
   */
  private final List<DME2EndpointReference> staleEndpointReferences = new ArrayList<DME2EndpointReference>();
  private final List<DME2EndpointReference> removedEndpointReferences = new ArrayList<DME2EndpointReference>();

  // private OrderedEndpointHolder currentOrderedEndpointHolder;
  private DME2Endpoint currentEndpoint;
  private DME2RouteOffer currentRouteOffer;
  private DME2EndpointReference currentEndpointHolder;

  private int currentSequence;
  private int currentEndpointListIndexPosition;
  private int staleEndpointListIndexPosition;
  private int endpointListSize;

  private double currentDistanceBand;
  private boolean elementReturned;
  private int minActiveEndPoints;

  public DME2EndpointIterator( DME2Manager manager, List<DME2EndpointReference> endpointReferenceList,
                               String queryParamMinActiveEndPoint ) throws DME2Exception {
    super( manager, endpointReferenceList, queryParamMinActiveEndPoint );
    LOGGER.debug( null, "DefaultEndpointIterator", "start" );
    this.manager = manager;
    this.originalOrderedEndpointHolders = Collections.unmodifiableList( endpointReferenceList );
    this.orderedEndpointHolders = endpointReferenceList;

    LOGGER.debug( null, "DefaultEndpointIterator", "queryParamMinActiveEndPoint=" + queryParamMinActiveEndPoint );
    if ( queryParamMinActiveEndPoint == null ) {
      this.minActiveEndPoints = manager.getIntProp( DME2Constants.DME2_MIN_ACTIVE_END_POINTS, 0 );
    } else {
      this.minActiveEndPoints = Integer.parseInt( queryParamMinActiveEndPoint );
    }
    if ( this.minActiveEndPoints == 0 ) {
      this.orderedEndpointHolders = endpointReferenceList;
    } else {
      this.orderedEndpointHolders = getMinActiveEndpointsList( orderedEndpointHolders, this.minActiveEndPoints );
    }
    initialize();
    LOGGER.debug( null, "DefaultEndpointIterator", "end" );
    /*
		 * Registering this class as an MBean so that it can be monitored via
		 * JMX
		 */
  }

  public DME2EndpointIterator( final IteratorCreatingAttributes iteratorCreatingAttributes ) throws DME2Exception {
    super( iteratorCreatingAttributes );

    LOGGER.debug( null, "DefaultEndpointIterator", "start" );
    this.manager = iteratorCreatingAttributes.getManager();
    if ( this.manager == null ) {
      this.manager = DME2Manager.getDefaultInstance();
    }
    this.originalOrderedEndpointHolders = Collections
        .unmodifiableList( iteratorCreatingAttributes.getEndpointHolders() );
    this.orderedEndpointHolders = iteratorCreatingAttributes.getEndpointHolders();

    LOGGER.debug( null, "DefaultEndpointIterator",
        "queryParamMinActiveEndPoint=" + iteratorCreatingAttributes.getQueryParamMinActiveEndPoint() );
    if ( iteratorCreatingAttributes.getQueryParamMinActiveEndPoint() == null ) {
      this.minActiveEndPoints = manager.getIntProp( DME2Constants.DME2_MIN_ACTIVE_END_POINTS, 0 );
    } else {
      this.minActiveEndPoints = Integer.parseInt( iteratorCreatingAttributes.getQueryParamMinActiveEndPoint() );
    }
    if ( this.minActiveEndPoints == 0 ) {
      this.orderedEndpointHolders = iteratorCreatingAttributes.getEndpointHolders();
    } else {
      this.orderedEndpointHolders = getMinActiveEndpointsList( orderedEndpointHolders, this.minActiveEndPoints );
    }
    initialize();
    LOGGER.debug( null, "DefaultEndpointIterator", "end" );
		/*
		 * Registering this class as an MBean so that it can be monitored via
		 * JMX
		 */
  }

  private void initialize() {
    LOGGER.debug( null, "initialize", "start" );

    endpointListSize = originalOrderedEndpointHolders.size();
    LOGGER.debug( null, "initialize", "Size of EndpointReference list: {}", endpointListSize );

    currentEndpointListIndexPosition = -1;
    LOGGER.debug( null, "initialize", "Starting EndpointReference list index position: {}",
        currentEndpointListIndexPosition );

    staleEndpointListIndexPosition = -1;

    LOGGER.debug( null, "initialize", "end" );
  }

  private List<DME2EndpointReference> getMinActiveEndpointsList(
      List<DME2EndpointReference> copyOforiginalOrderedEndpointHolder, int minActiveEndpoints ) {

    LOGGER.debug( null, "getMinActiveEndpointsList", "start" );

    // group all the endpoints based on sequence, in a map HashMap<sequence,List<endpoints>>, in increasing seq order 
    // then check the number of good end points , (size of end points-stale end points= good end points) in that 
    // sequence number, if they are >= than the minActiveEndpoint configured, then stop and use only them good ones else
    // keep adding good end points in the sequence number one after other till a minActiveEndpoint count is reached

    int mAE = 0;
    List<DME2EndpointReference> orderedEndpointHolders = new ArrayList<DME2EndpointReference>();
    DME2EndpointReference lastReferenceBeforeMinActiveEndpointCount = null;

    Collections.sort( copyOforiginalOrderedEndpointHolder, new Comparator<DME2EndpointReference>() {
      public int compare( DME2EndpointReference o1, DME2EndpointReference o2 ) {
        return o1.getSequence().compareTo( o2.getSequence() );// increasing order sort
      }
    } );
    for ( DME2EndpointReference orderedEndpointHolder : copyOforiginalOrderedEndpointHolder ) {
      if ( mAE >= minActiveEndpoints ) {
        List<DME2EndpointReference> copyOforiginalEndpointReferenceListSubList = copyOforiginalOrderedEndpointHolder
            .subList( mAE, copyOforiginalOrderedEndpointHolder.size() );
        for ( DME2EndpointReference tmpOrderedEndpointHolder : copyOforiginalEndpointReferenceListSubList ) {
          if ( lastReferenceBeforeMinActiveEndpointCount.getSequence() == tmpOrderedEndpointHolder
              .getSequence()
              && !StaleProcessor.isStale( manager, tmpOrderedEndpointHolder.getEndpoint() ) ) {
            orderedEndpointHolders.add( tmpOrderedEndpointHolder ); // add all endpoints in the sequence that was last found to satisfy minActiveEndpoints count
          }
        }
        break;
      }
      if ( !StaleProcessor.isStale( manager, orderedEndpointHolder.getEndpoint() ) ) {
        orderedEndpointHolders.add( orderedEndpointHolder );
        lastReferenceBeforeMinActiveEndpointCount = orderedEndpointHolder;
        mAE++;
      }

    }

    for ( DME2EndpointReference tmpOrderedEndpointHolder : orderedEndpointHolders ) {
      tmpOrderedEndpointHolder.getRouteOffer().getRouteOffer()
          .setSequence( lastReferenceBeforeMinActiveEndpointCount.getSequence() );
      tmpOrderedEndpointHolder.setSequence( lastReferenceBeforeMinActiveEndpointCount.getSequence() );
    }
    LOGGER.debug( null, "getMinActiveEndpointsList", "end" );

    return orderedEndpointHolders;
  }

  @Override
  public boolean hasNext() {
    return ( currentEndpointListIndexPosition < ( orderedEndpointHolders.size() - 1 ) )
        || ( staleEndpointListIndexPosition < ( staleEndpointReferences.size() - 1 ) );
  }

  @Override
  public DME2EndpointReference next() {
    LOGGER.debug( null, "next", LogMessage.METHOD_ENTER );
    DME2EndpointReference orderedEndpointHolder = null;

		/* If no more elements are available, throw Exception */
    if ( !hasNext() ) {
      throw new NoSuchElementException( "No more Endpoint References are available in the Iterator." );
    }

    while ( hasNext() ) {
      if ( !isPrimaryEndpointReferenceListExhausted() ) {
        ++currentEndpointListIndexPosition;
        LOGGER.debug( null, "next", LogMessage.DEBUG_MESSAGE, "Current primary EndpointHolders list index position: {}",
            currentEndpointListIndexPosition );

        orderedEndpointHolder = orderedEndpointHolders.get( currentEndpointListIndexPosition );

        if ( orderedEndpointHolder.getEndpoint() != null
            && StaleProcessor.isStale( manager, orderedEndpointHolder.getEndpoint() ) ) {
          String msg = String.format(
              "DME2Endpoint %s is stale. Skipping and continuing through the iteration to find the next available Endpoint.",
              orderedEndpointHolder.getEndpoint() );
          LOGGER.debug( null, "next", LogMessage.DEBUG_MESSAGE, msg );
          staleEndpointReferences.add( orderedEndpointHolder );

					/*
					 * Removing stale element from the active list and adjusting
					 * the element position.
					 */
          orderedEndpointHolders.remove( currentEndpointListIndexPosition );
          --currentEndpointListIndexPosition;
          continue;
        }
      } else if ( !isStaleEndpointReferenceListExhausted() ) {
        ++staleEndpointListIndexPosition;
        LOGGER.debug( null, "next", LogMessage.DEBUG_MESSAGE, "Current stale Endpoint Holders index position: {}",
            staleEndpointListIndexPosition );
        orderedEndpointHolder = staleEndpointReferences.get( staleEndpointListIndexPosition );
        if ( failbackLoggingEnabled ) {
          FAILBACK_LOGGER.debug( null, "next", "FailBack: Code=Client.Send; Failing back endpoint(s): {}",
              orderedEndpointHolder.getEndpoint().toURLString() );
        }
      }

      currentEndpointHolder = orderedEndpointHolder;
      currentEndpoint = currentEndpointHolder.getEndpoint();
      currentSequence = currentEndpointHolder.getSequence();
      currentRouteOffer = currentEndpointHolder.getRouteOffer();
      currentDistanceBand = currentEndpointHolder.getDistanceBand();

      LOGGER.debug( null, "next", LogMessage.DEBUG_MESSAGE, "printEndpointReferenceDetails: {}",
          printEndpointReferenceDetails() );
      break;
    }

    elementReturned = true;
    LOGGER.debug( null, "next", LogMessage.METHOD_EXIT );

    return orderedEndpointHolder;
  }

  @Override
  public void remove() {
    LOGGER.debug( null, "remove", LogMessage.METHOD_ENTER );

		/* Throw Exception if next() hasn't been called yet. */
    if ( !elementReturned ) {
      throw new IllegalStateException(
          "Error occured - removed() cannot be called before called next(). Additionally, the remove() method can be called only once per call to next()." );
    }

    LOGGER.debug( null, "remove", "Removing EndpointReference from the Iterator: {}", printEndpointReferenceDetails() );

    if ( orderedEndpointHolders.size() != 0 ) {
      removedEndpointReferences.add( currentEndpointHolder );
      orderedEndpointHolders.remove( currentEndpointListIndexPosition );

      --currentEndpointListIndexPosition;

      LOGGER.debug( null, "remove",
          "Current primary EndpointReference list index position after removing EndpointReference from the Iterator: {}",
          currentEndpointListIndexPosition );
    } else if ( staleEndpointReferences.size() != 0 ) {
      removedEndpointReferences.add( currentEndpointHolder );
      staleEndpointReferences.remove( staleEndpointListIndexPosition );

      --staleEndpointListIndexPosition;
      LOGGER.debug( null, "remove",
          "Current stale EndpointReference list index position after removing EndpointReference from the Iterator: {}",
          staleEndpointListIndexPosition );
      if ( failbackLoggingEnabled ) {
        FAILBACK_LOGGER.debug( null, "remove", "Code=Client.Send; Failing back endpoint(s) [{}]",
            currentEndpointHolder.getEndpoint().toURLString() );
      }
    }

    elementReturned = false;
    LOGGER.debug( null, "remove", LogMessage.METHOD_EXIT );
  }

  @Override
  public void setRouteOffersTried( String routeOffersTried ) {
    this.routeOffersTried = routeOffersTried;
  }

  @Override
  public String getRouteOffersTried() {
    return routeOffersTried;
  }

  private boolean isPrimaryEndpointReferenceListExhausted() {
    return ( currentEndpointListIndexPosition == ( orderedEndpointHolders.size() - 1 ) );
  }

  private boolean isStaleEndpointReferenceListExhausted() {
    return ( staleEndpointListIndexPosition == ( staleEndpointReferences.size() - 1 ) );
  }

  /**
   * Returns a string containing information about the current Endpoint Refernence that can be used from printing or
   * logging messages.
   */
  private String printEndpointReferenceDetails() {
    StringBuffer sb = new StringBuffer();
    sb.append( "[" );
    sb.append( "currentSequence=" + currentSequence + "; " );
    sb.append( "currentDistanceBand=" + currentDistanceBand + "; " );
    sb.append( "currentEndpoint=" + ( currentEndpoint != null ? currentEndpoint.toString() : "" ) + "; " );
    sb.append( "currentRouteOffer="
        + ( currentRouteOffer != null ? currentRouteOffer.getFqName() : currentEndpoint.getRouteOffer() + "; " ) );
    sb.append( "]" );

    return sb.toString();
  }

  @Override
  public void resetIterator() {
    LOGGER.debug( null, "resetIterator", LogMessage.METHOD_ENTER );

    StringBuilder sb = new StringBuilder();
    String delim = "";
    for ( DME2EndpointReference reference : staleEndpointReferences ) {
      sb.append( delim ).append( reference.getEndpoint().toURLString() );
      delim = ", ";

    }
    if ( failbackLoggingEnabled ) {
      FAILBACK_LOGGER.debug( null, "remove", LogMessage.SEP_FAILBACK, sb.toString() );
    }
    staleEndpointReferences.clear();
    removedEndpointReferences.clear();

    currentEndpointListIndexPosition = -1;
    staleEndpointListIndexPosition = -1;
    orderedEndpointHolders = new ArrayList<DME2EndpointReference>( originalOrderedEndpointHolders );

    LOGGER.debug( null, "resetIterator", LogMessage.METHOD_EXIT );
  }

  @Override
  public int getTotalNumberOfElements() {
    return originalOrderedEndpointHolders.size();
  }

  @Override
  public int getNumberOfActiveElements() {
    return this.orderedEndpointHolders.size();
  }

  @Override
  public int getNumberOfStaleElements() {
    return staleEndpointReferences.size();
  }

  @Override
  public int getNumberOfRemovedElements() {
    return removedEndpointReferences.size();
  }

  @Override
  public int getCurrentIteratorActiveIndexPosition() {
    return currentEndpointListIndexPosition;
  }

  @Override
  public int getCurrentIteratorStaleIndexPosition() {
    return staleEndpointListIndexPosition;
  }

  @Override
  public String getCurrentDME2Endpoint() {
    return currentEndpoint.toString();
  }

  @Override
  public double getCurrentDME2EndpointDistanceBand() {
    return currentDistanceBand;
  }

  @Override
  public int getCurrentDME2EndpointSequence() {
    return currentSequence;
  }

  @Override
  public String getCurrentDME2EndpointRouteOffer() {
    return currentRouteOffer != null
        ? ( currentRouteOffer.getRouteOffer() != null ? currentRouteOffer.getRouteOffer().getName() : null )
        : null;
  }

  @Override
  public void removeAllStaleIteratorElements() {
    manager.getStaleCache().clearStaleEndpoints();
  }

  @Override
  public void removeStaleIteratorElement( String serviceName ) {
    manager.removeStaleEndpoint( serviceName );
  }

  @Override
  public boolean isAllElementsStale() {
    return ( originalOrderedEndpointHolders.size() == staleEndpointReferences.size() );
  }

  @Override
  public boolean isAllElementsExhausted() {
    if ( !hasNext() ) {
      return true;
    }
    return false;
  }

  @Override
  public DME2RouteOffer getCurrentDME2RouteOffer() {
    return currentRouteOffer;
  }

  @Override
  public void setStale() {
    StaleProcessor.setStale( currentEndpointHolder.getRouteOffer(), manager, currentEndpointHolder.getEndpoint() );
  }

  @Override
  public boolean isStale() {
    return StaleProcessor.isStale( manager, currentEndpointHolder.getEndpoint() );
  }

  @Override
  public List<DME2EndpointReference> getEndpointReferenceList() {
    return orderedEndpointHolders;
  }

  @Override
  public int getMinActiveEndPoints() {
    return minActiveEndPoints;
  }

  @Override
  public DME2Manager getManager() {
    return manager;
  }

  @Override
  public DME2EndpointReference getCurrentEndpointReference() {
    return currentEndpointHolder;
  }

}