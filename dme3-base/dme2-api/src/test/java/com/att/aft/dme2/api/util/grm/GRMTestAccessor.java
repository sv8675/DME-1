package com.att.aft.dme2.api.util.grm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.att.aft.dme2.api.DME2Exception;
import com.att.aft.dme2.api.util.SecurityContext;
import com.att.aft.dme2.config.DME2Configuration;
import com.att.aft.dme2.registry.accessor.BaseAccessor;
import com.att.aft.dme2.registry.accessor.GRMAccessorFactory;
import com.att.aft.dme2.registry.dto.ServiceEndpoint;
import com.att.aft.dme2.util.grm.IGRMEndPointDiscovery;
import com.att.scld.grm.types.v1.ClientJVMInstance;
import com.att.scld.grm.v1.FindClientJVMInstanceRequest;
import com.att.scld.grm.v1.RegisterClientJVMInstanceRequest;

public class GRMTestAccessor implements BaseAccessor {
	public static Map<String, Object> serviceCache = new ConcurrentHashMap<String, Object>();

	private JAXBContext context;
	public static boolean debugRequests = false;

	BaseAccessor delegate;

	public GRMTestAccessor(DME2Configuration configuration) throws DME2Exception {
		SecurityContext.create(configuration);
		delegate = GRMAccessorFactory.getGrmAccessorHandlerInstance( configuration, SecurityContext.create( configuration ) );
	}

	public GRMTestAccessor(BaseAccessor template) throws DME2Exception {
		delegate = template;
	}

	/**
	 * @param serviceEndpoint
	 * @throws DME2Exception
	 * @see BaseAccessor#addServiceEndPoint(com.att.aft.dme2.registry.dto.ServiceEndpoint)
	 */
	public void addServiceEndPoint(ServiceEndpoint serviceEndpoint) throws DME2Exception {

		if (debugRequests) {
			try {
				Marshaller m = context.createMarshaller();
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				m.marshal(serviceEndpoint, System.out);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		serviceCache.put("add", serviceEndpoint);
		/*
		serviceCache.put("addSepContainer", req.getContainerInstanceRef());
		serviceCache.put("addSepLrm", req.getLrmRef());
		 */

		delegate.addServiceEndPoint(serviceEndpoint);
	}

	/**
	 * @param serviceEndpoint
	 * @throws DME2Exception
	 * @see BaseAccessor#updateServiceEndPoint(com.att.aft.dme2.registry.dto.ServiceEndpoint)
	 */
	public void updateServiceEndPoint(ServiceEndpoint serviceEndpoint) throws DME2Exception {

		if (debugRequests) {
			try {
				Marshaller m = context.createMarshaller();
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				m.marshal(serviceEndpoint, System.out);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		serviceCache.put("update", serviceEndpoint);
		/*
		serviceCache.put("updateSepContainer", req.getContainerInstanceRef());
		serviceCache.put("updateSepLrm", req.getLrmRef());
		 */
		delegate.updateServiceEndPoint( serviceEndpoint );
	}

	/**
	 * @param serviceEndpoint
	 * @throws DME2Exception
	 * @see BaseAccessor#deleteServiceEndPoint(com.att.aft.dme2.registry.dto.ServiceEndpoint)
	 */
	public void deleteServiceEndPoint(ServiceEndpoint serviceEndpoint) throws DME2Exception {
		serviceCache.put("delete", serviceEndpoint);

		if (debugRequests) {
			try {
				Marshaller m = context.createMarshaller();
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				m.marshal(serviceEndpoint, System.out);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		delegate.deleteServiceEndPoint(serviceEndpoint);
	}

	/**
	 * @param serviceEndpoint
	 * @return
	 * @throws DME2Exception
	 * @see BaseAccessor#findRunningServiceEndPoint(com.att.aft.dme2.registry.dto.ServiceEndpoint)
	 */
	public List<ServiceEndpoint> findRunningServiceEndPoint(ServiceEndpoint serviceEndpoint) throws DME2Exception {
		if (debugRequests) {
			try {
				Marshaller m = context.createMarshaller();
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				m.marshal(serviceEndpoint, System.out);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		serviceCache.put("find", serviceEndpoint);
		return delegate.findRunningServiceEndPoint(serviceEndpoint);
	}

	@Override
	public String getRouteInfo( ServiceEndpoint req ) throws DME2Exception {
		return null;
	}

	@Override
	public void registerClientJVMInstance(RegisterClientJVMInstanceRequest req) throws DME2Exception {
		delegate.registerClientJVMInstance(req);
	}

	@Override
	public List<ClientJVMInstance> findClientJVMInstance(FindClientJVMInstanceRequest req) throws DME2Exception {
		return delegate.findClientJVMInstance(req);
	}

	public IGRMEndPointDiscovery getGrmEndPointDiscovery () {
		return delegate.getGrmEndPointDiscovery();
	}
}
