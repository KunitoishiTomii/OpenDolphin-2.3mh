package open.dolphin.impl.orcaapi;

import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * ORCA API用のRest Client
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaApiClient implements IOrcaApi {
    
    private static final OrcaApiClient instance;
    
    private Client client;
    private WebTarget webTarget;

    static {
        instance = new OrcaApiClient();
    }
    
    private OrcaApiClient() {
        setup();
    }
    
    public static OrcaApiClient getInstance() {
        return instance;
    }
    
    public final void setup() {

        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(Project.getString(Project.CLAIM_ADDRESS));
        sb.append(":").append(String.valueOf(API_PORT));
        URI uri = URI.create(sb.toString());
        String username = Project.getString(Project.ORCA_USER_ID);
        String password = Project.getString(Project.ORCA_USER_PASSWORD);
        
        boolean useJersey = Project.getBoolean(MiscSettingPanel.USE_JERSEY, MiscSettingPanel.DEFAULT_USE_JERSEY);
        
        if (useJersey) {
            client = new JerseyClientBuilder().build();
            webTarget = client.target(uri);
            webTarget.register(HttpAuthenticationFeature.basic(username, password));
        } else {
            client = new ResteasyClientBuilder().build();
            webTarget = client.target(uri);
            webTarget.register(new BasicAuthentication(username, password));
        }
    }
    
    public WebTarget getWebTarget() {
        return webTarget;
    }
}
