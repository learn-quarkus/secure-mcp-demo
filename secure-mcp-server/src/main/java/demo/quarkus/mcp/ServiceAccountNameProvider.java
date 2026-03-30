package demo.quarkus.mcp;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import jakarta.inject.Inject;

public class ServiceAccountNameProvider {

    private static final Logger LOG = Logger.getLogger(ServiceAccountNameProvider.class);

    @RestClient
    @Inject
    ServiceAccountNameRestClient serviceAccountNameRestClient;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-client.auth-server-url")
    String authServerUrl;

    @Inject
    @ConfigProperty(name = "quarkus.oidc-client.credentials.secret")
    String authCredSecret;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.client-id")
    String authClientId;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.service-account-name-rest-server.auth-server-url")
    String serviceAccountNameRestServerAuthServerUrl;

    @Tool(name = "sevice-account-name-provider", description = "Provides a name of the current service account")
    TextContent provideServiceAccountName() {

        LOG.info("==================provideServiceAccountName==================");
        LOG.infof("Auth Server URL: %s", authServerUrl);
        LOG.infof("Auth Cred Secret: %s", authCredSecret);
        LOG.infof("Auth Client ID: %s", authClientId);
        LOG.infof("Service Account Name Rest Server Auth Server URL: %s", serviceAccountNameRestServerAuthServerUrl);
        LOG.info("================provideServiceAccountName END====================");

        return new TextContent(serviceAccountNameRestClient.getServiceAccountName());
    }
}
