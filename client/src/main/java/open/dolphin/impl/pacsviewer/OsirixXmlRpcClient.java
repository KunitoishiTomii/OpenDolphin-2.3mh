package open.dolphin.impl.pacsviewer;

import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * OsiriXをXML-RPCで開く
 * 
 * @author masuda, Masuda Naika
 */
public class OsirixXmlRpcClient {

    private static final boolean DEBUG = false;

    public static final MediaType MEDIATYPE_XML_UTF8
            = MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8");

    private static final OsirixXmlRpcClient instance;

    static {
        instance = new OsirixXmlRpcClient();
    }

    private Client client;
    private WebTarget webTarget;

    private OsirixXmlRpcClient() {
        setupClient();
    }

    public static OsirixXmlRpcClient getInstance() {
        return instance;
    }

    public final void setupClient() {

        boolean useJersey = Project.getBoolean(MiscSettingPanel.USE_JERSEY, MiscSettingPanel.DEFAULT_USE_JERSEY);
        if (useJersey) {
            client = new JerseyClientBuilder().build();
        } else {
            client = new ResteasyClientBuilder().build();
        }
        setupWebTarget();
    }

    public final void setupWebTarget() {
        URI uri = URI.create(Project.getString(MiscSettingPanel.PACS_OSIRIX_ADDRESS));
        webTarget = client.target(uri);
    }

    public void openByPatientId(String patientId) throws Exception {

        final String xml = createRpcXml("DisplayStudy", "patientID", patientId);

        final Entity entity = toXmlEntity(xml);

        Response response = webTarget
                .request(MEDIATYPE_XML_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String resXml = response.readEntity(String.class);

        debug(status, resXml);

        response.close();
    }

    public void openByStudyUID(String studyUID) throws Exception {

        final String xml = createRpcXml("DisplayStudy", "studyInstanceUID", studyUID);

        final Entity entity = toXmlEntity(xml);

        Response response = webTarget
                .request(MEDIATYPE_XML_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String resXml = response.readEntity(String.class);

        debug(status, resXml);

        response.close();
    }

    private int checkHttpStatus(Response response) throws Exception {
        int status = response.getStatus();
        if (status / 100 != 2) {
            String msg = "HTTP" + String.valueOf(status);
            response.close();
            throw new Exception(msg);
        }
        return status;
    }

    private Entity toXmlEntity(String xml) {
        return Entity.entity(xml, MEDIATYPE_XML_UTF8);
    }

    private String createRpcXml(String methodName, String paramName, String value) {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<methodCall><methodName>").append(methodName).append("</methodName>");
        sb.append("<params>");
        sb.append("<param>");
        sb.append("<value><struct>");
        sb.append("<member>");
        sb.append("<name>").append(paramName).append("</name>");
        sb.append("<value><string>").append(value).append("</string></value>");
        sb.append("</member>");
        sb.append("</struct></value>");
        sb.append("</param>");
        sb.append("</params>");
        sb.append("</methodCall>");

        return sb.toString();
    }

    private void debug(int status, String entity) {

        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP status = ").append(status);
            sb.append(" entity = ").append(entity);
            System.out.println(sb.toString());
        }
    }
}
