package com.att.aft.dme2.server.jetty;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.io.ssl.SslConnection.DecryptedEndPoint;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.att.aft.dme2.server.jetty.DME2SelectChannelConnector.DME2SelectChannelEndPoint;

/**
 * Override default SslSelectChannelConnector so we can provide a custom
 * dispatch() implementation. This implementation allows 0 length queue. Out of
 * the box Jetty 7 has a bug where even if you specify a 0 length queue it will
 * still queue requests.
 */
public class DME2SslSelectChannelConnector extends ServerConnector {

	private SslContextFactory sslContextFactory;
	public DME2SslSelectChannelConnector(Server server, SslContextFactory sslContextFactory, ConnectionFactory[] factories) {
		super(server, sslContextFactory, factories);
		this.sslContextFactory = sslContextFactory;
	}

	@Override
	protected DME2SelectChannelEndPoint newEndPoint(SocketChannel channel, ManagedSelector selectSet, SelectionKey key) throws IOException {
		DME2SelectChannelEndPoint endp = new DME2SelectChannelEndPoint(channel, selectSet, key, (int)this.getIdleTimeout());
		//endp.setConnection(selectSet.getManager().newConnection(channel, endp, key.attachment()));
		ConnectHandler hand = new ConnectHandler();
		SelectorManager selectorManager = hand.getBean(SelectorManager.class);
		endp.setConnection(selectorManager.newConnection(channel, endp, key.attachment()));
		return endp;
	}

	//@Override
	protected SslConnection newSslConnection(DecryptedEndPoint endpoint, SSLEngine engine) {
		return new DME2SslConnection(null, null, endpoint, engine);
	}

	public SslContextFactory getSslContextFactory() {
		return sslContextFactory;
	}
	
	public class DME2SslConnection extends SslConnection {
		
		public DME2SslConnection(ByteBufferPool byteBufferPool, Executor executor, EndPoint endPoint, SSLEngine sslEngine)
		{
			super(byteBufferPool, executor, endPoint, sslEngine);
		}
		
		//public DME2SslConnection(SSLEngine engine, EndPoint endp) {
		//	super(engine, endp);
		//}

		//@Override
		protected DecryptedEndPoint newSslEndPoint() {
			return new DME2SslEndPoint();
		}

		public class DME2SslEndPoint extends DecryptedEndPoint {
			public DME2SslEndPoint() {
				super();
			}

	
			/*@Override
			public void asyncDispatch() {
				try {
					super.asyncDispatch();
				} catch (java.util.concurrent.RejectedExecutionException e) {
					Log.getLogger(DME2SslEndPoint.class)
							.debug("asyncDispatch Execution was rejected...Closing...");
					try {
						close();
					} catch (IOException e1) {
						Log.getLogger(DME2SslEndPoint.class).ignore(e1);
					}
					return;
				}
			}

			@Override
			public void dispatch() {
				try {
					super.dispatch();
				} catch (java.util.concurrent.RejectedExecutionException e) {
					Log.getLogger(DME2SslEndPoint.class).debug(
							"Execution was rejected...Closing...");
					try {
						close();
					} catch (IOException e1) {
						Log.getLogger(DME2SslEndPoint.class).ignore(e1);
					}
					return;
				}
			}*/
		}
	}
}
