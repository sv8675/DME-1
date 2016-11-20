package com.att.aft.dme2.server.jetty;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.SelectChannelEndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.log.Log;

public class DME2SelectChannelConnector extends ServerConnector {
	
	public DME2SelectChannelConnector(Server server, int acceptors, int selectors, ConnectionFactory[] factories) {
		super(server, acceptors, selectors, factories);
	}

	public DME2SelectChannelConnector(Server server) {
		super(server, 10, 10);
		// TODO Auto-generated constructor stub
	}

	/**
	 * New end point.
	 * 
	 * @param channel
	 *            the channel
	 * @param selectSet
	 *            the select set
	 * @param key
	 *            the key
	 * @return the e http select channel end point
	 * @throws java.io.IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	protected DME2SelectChannelEndPoint newEndPoint(SocketChannel channel, ManagedSelector selectSet, SelectionKey key) throws IOException {
		
		//TODO this needs to be revisted.. not sure how to create the connection
		DME2SelectChannelEndPoint endp = new DME2SelectChannelEndPoint(channel, selectSet, key, (int)this.getIdleTimeout());
		//endp.setConnection(selectSet.getManager().newConnection(channel, endp, key.attachment()));
		ConnectHandler hand = new ConnectHandler();
		SelectorManager selectorManager = hand.getBean(SelectorManager.class);
		endp.setConnection(selectorManager.newConnection(channel, endp, key.attachment()));
		return endp;
	}

	public static class DME2SelectChannelEndPoint extends SelectChannelEndPoint {

		public DME2SelectChannelEndPoint(SocketChannel channel, ManagedSelector selectSet, SelectionKey key, int maxIdleTime) throws IOException {
			super(channel, selectSet, key, null, maxIdleTime);
		}

		//@Override
		public void dispatch() {
			try {
				//super.dispatch();
			} catch (java.util.concurrent.RejectedExecutionException e) {
				Log.getLogger(DME2SelectChannelEndPoint.class).debug(
						"Execution was rejected...Closing...");
				//try {
					close();
				//} catch (IOException e1) {
				//	Log.getLogger(DME2SelectChannelEndPoint.class).ignore(e1);
				//}
				return;
			}
		}
	}
}
