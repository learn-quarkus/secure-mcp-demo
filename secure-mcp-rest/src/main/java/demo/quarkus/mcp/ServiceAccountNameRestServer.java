package demo.quarkus.mcp;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.jboss.logging.Logger;

@Path("/service-account-name")
public class ServiceAccountNameRestServer {

    private static final Logger LOG = Logger.getLogger(ServiceAccountNameRestServer.class);

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Produces("text/plain")
    @Authenticated
    public String getServiceAccountName() {
        LOG.infof("getServiceAccountName() called by %s", securityIdentity.getPrincipal().getName());
        return securityIdentity.getPrincipal().getName();
    }
}
