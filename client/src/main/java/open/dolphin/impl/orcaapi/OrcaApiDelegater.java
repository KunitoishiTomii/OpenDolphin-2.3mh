package open.dolphin.impl.orcaapi;

import open.dolphin.impl.orcaapi.model.OrcaApiRequestBuilder;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import open.dolphin.client.ClientContext;
import open.dolphin.client.KarteSenderResult;
import open.dolphin.dao.SyskanriInfo;
import open.dolphin.order.MasterItem;
import open.dolphin.project.Project;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import open.dolphin.impl.orcaapi.model.*;
import open.dolphin.impl.orcaapi.parser.*;

/**
 * ORCA APIのデレゲータ
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaApiDelegater implements IOrcaApi {
    
    private static final String ORCA_API = "ORCA API";
    
    private static final OrcaApiDelegater instance;

    private final boolean DEBUG;
    
    private List<PhysicianInfo> physicianList;
    private List<DepartmentInfo> deptList;
    
    private final boolean xml2;
    
    static {
        instance = new OrcaApiDelegater();
    }
    
    public static OrcaApiDelegater getInstance() {
        return instance;
    }
    
    private OrcaApiDelegater() {
        DEBUG = (ClientContext.getBootLogger().getLevel() == Level.DEBUG);
        xml2 = SyskanriInfo.getInstance().isOrca47();
    }
    
    
    public KarteSenderResult sendMedicalModModel(MedicalModModel model) {
        try {
            return sendMedicalModModelImpl(model);
        } catch (Exception ex) {
            String code = ex.getMessage();      // HTTP404
            String msg = "接続を確認してください。";
            return new KarteSenderResult(ORCA_API, code, msg);
        }
    }
    
    private KarteSenderResult sendMedicalModModelImpl(MedicalModModel model) throws Exception {
        
        final String path = xml2
                ? "/api21/medicalmodv2"
                : "/api21/medicalmod";

        final String xml = xml2
                ? OrcaApiRequestBuilder.createSystem01ManagereqXml2()
                : OrcaApiRequestBuilder.createSystem01ManagereqXml();

        final Entity entity = toXmlEntity(xml);

        Response response = getWebTarget()
                .path(path)
                .queryParam(CLASS, "01")
                .request(MEDIATYPE_XML_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String resXml = response.readEntity(String.class);
        debug(status, resXml);
        
        response.close();

        KarteSenderResult result;
        try {
            MedicalreqResParser parser = new MedicalreqResParser(resXml);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            result = new KarteSenderResult(ORCA_API, code, msg);
        } catch (JDOMException | IOException ex) {
            result = new KarteSenderResult(ORCA_API, KarteSenderResult.ERROR, ex.getMessage());
        }

        return result;
    }
    
    private void getDepartmentInfo() throws Exception {
        
        final String path = xml2
                ? "/api01rv2/system01lstv2"
                : "/api01r/system01lst";

        final String xml = xml2
                ? OrcaApiRequestBuilder.createSystem01ManagereqXml2()
                : OrcaApiRequestBuilder.createSystem01ManagereqXml();
        
        final Entity entity = toXmlEntity(xml);

        Response response = getWebTarget()
                .path(path)
                .queryParam(CLASS, "01")
                .request(MEDIATYPE_XML_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String resXml = response.readEntity(String.class);
        debug(status, resXml);
        
        response.close();

        try {
            DepartmentResParser parser = new DepartmentResParser(resXml);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            if (!API_NO_ERROR.equals(code)) {
                deptList = parser.getList();
            }

        } catch (JDOMException | IOException ex) {
        }
    }

    private void getPhysicianInfo() throws Exception {
        
        final String path = xml2
                ? "/api01rv2/system01lstv2t"
                : "/api01r/system01lst";

        final String xml = xml2
                ? OrcaApiRequestBuilder.createSystem01ManagereqXml2()
                : OrcaApiRequestBuilder.createSystem01ManagereqXml();
        
        final Entity entity = toXmlEntity(xml);

        Response response = getWebTarget()
                .path(path)
                .queryParam(CLASS, "02")
                .request(MEDIATYPE_XML_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String resXml =  response.readEntity(String.class);
        debug(status, resXml);
        
        response.close();

        try {
            PhysicianResParser parser = new PhysicianResParser(resXml);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            if (!API_NO_ERROR.equals(code)) {
                physicianList = parser.getList();
            }
        } catch (JDOMException | IOException ex) {
        }
    }
    
    public List<String> getOrcaVisit(String patientId, String ymd) throws Exception {
        
        final String path = "/api01rv2/medicalgetv2";
        final String deptCode = Project.getUserModel().getDepartmentModel().getDepartment();
        final String xml = OrcaApiRequestBuilder.createMedicalgetreqXml2(patientId, ymd, deptCode);
        
        final Entity entity = toXmlEntity(xml);

        Response response = getWebTarget()
                .path(path)
                .queryParam(CLASS, "01")
                .request(MEDIATYPE_XML_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String resXml = response.readEntity(String.class);
        debug(status, resXml);
        
        response.close();

        try {
            MedicalgetResParser parser = new MedicalgetResParser(resXml);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            if (API_NO_ERROR.equals(code)) {
                return parser.getPerformDate();
            }
        } catch (JDOMException | IOException ex) {
        }
        return Collections.emptyList();
    }
    
    public List<MasterItem> getOrcaMed(String patientId, String ymd) throws Exception {
        
        final String path = "/api01rv2/medicalgetv2";
        final String deptCode = Project.getUserModel().getDepartmentModel().getDepartment();
        final String xml = OrcaApiRequestBuilder.createMedicalgetreqXml2(patientId, ymd, deptCode);
        
        final Entity entity = toXmlEntity(xml);

        Response response = getWebTarget()
                .path(path)
                .queryParam(CLASS, "02")
                .request(MEDIATYPE_XML_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String resXml = response.readEntity(String.class);
        debug(status, resXml);

        response.close();

        try {
            MedicalgetResParser parser = new MedicalgetResParser(resXml);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            if (API_NO_ERROR.equals(code)) {
                return parser.getMedMasterItem();
            }
        } catch (JDOMException | IOException ex) {
        }
        return Collections.emptyList();
    }
    

    private WebTarget getWebTarget() {
        return OrcaApiClient.getInstance().getWebTarget();
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
    
    private void debug(int status, String entity) {
        if (DEBUG) {
            Logger logger = ClientContext.getClaimLogger();
            logger.debug("---------------------------------------");
            logger.debug("status = " + status);
            logger.debug(entity);
        }
    }
}
