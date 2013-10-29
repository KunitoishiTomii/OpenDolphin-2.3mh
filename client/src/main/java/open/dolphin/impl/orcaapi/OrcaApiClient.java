package open.dolphin.impl.orcaapi;

import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * ORCA API用のJersey Client
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
    
    final public void setup() {

        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(Project.getString(Project.CLAIM_ADDRESS));
        sb.append(":").append(String.valueOf(API_PORT));
        String uri = sb.toString();
        String username = Project.getString(Project.ORCA_USER_ID);
        String password = Project.getString(Project.ORCA_USER_PASSWORD);
        
        boolean useJersey = Project.getBoolean(MiscSettingPanel.USE_JERSEY, MiscSettingPanel.DEFAULT_USE_JERSEY);
        
        if (useJersey) {
            client = new JerseyClientBuilder().build();
            webTarget = client.target(uri);
            webTarget.register(new HttpBasicAuthFilter(username, password));
        } else {
            client = new ResteasyClientBuilder().build();
            webTarget = client.target(uri);
            webTarget.register(new BasicAuthentication(username, password));
        }
    }

    public Invocation.Builder buildRequest(String path, MultivaluedMap<String, String> qmap) {
        
        WebTarget target = webTarget.path(path);
        
        // めんどくさいなぁ…
        if (qmap != null) {
            for (Map.Entry<String, List<String>> entry : qmap.entrySet()) {
                for (String value : entry.getValue()) {
                    target = target.queryParam(entry.getKey(), value);
                }
            }
        }
        
        return target.request(MEDIATYPE_XML_UTF8);
    }
}
