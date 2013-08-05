package open.dolphin.impl.orcaapi;

import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.project.Project;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;

/**
 * ORCA API用のJersey Client
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaApiClient {
    
    private static final String ENCODING = "UTF-8";
    private static final int API_PORT = 8000;
    
    private static final OrcaApiClient instance;
    
    private Client client;
    private WebTarget webTarget;

    static {
        instance = new OrcaApiClient();
    }
    
    private OrcaApiClient() {
        client = ClientBuilder.newClient();
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
        
        webTarget = client.target(uri);
        
        client.register(new HttpBasicAuthFilter(username, password));
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
        
        return target.request(MediaType.APPLICATION_XML_TYPE).acceptEncoding(ENCODING);
    }
}
