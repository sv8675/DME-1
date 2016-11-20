package com.att.aft.dme2.server.api.websocket;

import java.util.concurrent.ThreadPoolExecutor;

import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.DME2Manager;
import com.att.aft.dme2.api.util.DME2ThreadPoolConfig;
import com.att.aft.dme2.util.ErrorContext;

/**
 * Creates and initializes the Jetty websocket client connection factory
 *
 */
public class DME2WSClientFactory {
	
	private WebSocketClient wsClient = null;
	//private WebSocketClientFactory wsClientFactory = null;
	private transient ThreadPoolExecutor wsFactoryThreadpool = null;


	public DME2WSClientFactory(DME2Manager mgr) throws DME2Exception {
		//create the websocket client connection factory. This can be created once and used across DME2Clients
		try {
			if (wsFactoryThreadpool == null) {
				wsFactoryThreadpool = DME2ThreadPoolConfig.getInstance(mgr).createWebSocketFactoryThreadPool();
			}
			
			wsClient = new WebSocketClient(new ExecutorThreadPool(wsFactoryThreadpool));
			//wsClientFactory = new WebSocketClientFactory(new ExecutorThreadPool(wsFactoryThreadpool));			
		} catch (Exception e) {
			ErrorContext ec = new ErrorContext();
			ec.add("factory","DME2_WS_CLIENT_FACTORY_CREATION");
			ec.add("ErrorMessage",e.getMessage());
			throw new DME2Exception("AFT-DME2-3003", ec,e);
		}
	}
	
	public void start() throws DME2Exception {
		try {
			wsClient.start();
			//wsClientFactory.start();
		} catch (Exception e) {
			ErrorContext ec = new ErrorContext();
			ec.add("Factory", "DME2_WS_CLIENT_FACTORY_START");
			ec.add("ErrorMessage",e.getMessage());
			throw new DME2Exception("AFT-DME2-3003", ec,e);
		}
	}
	
	
	public void stop() throws DME2Exception {
		try {
			if ((wsClient != null)  && (wsClient.isStarted()))
				wsClient.stop();
		} catch (Exception e) {
			throw new DME2Exception("AFT-DME2-3003", new ErrorContext().add("Factory", "DME2_WS_CLIENT_FACTORY_STOP"));
		}
	}

	public WebSocketClient getWsClientFactory() {
		return wsClient;
	}
	
}
