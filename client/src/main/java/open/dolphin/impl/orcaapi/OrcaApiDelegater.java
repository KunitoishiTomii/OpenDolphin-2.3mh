package open.dolphin.impl.orcaapi;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

/**
 * ORCA APIのデレゲータ
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaApiDelegater implements IOrcaApi {
    
    private static final String ORCA_API = "ORCA API";
    
    private static final OrcaApiDelegater instance;

    private final boolean DEBUG;
    private final XMLOutputter outputter;
    private final SAXBuilder builder;
    
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
        DEBUG = (ClientContext.getBootLogger().getLevel()==Level.DEBUG);
        outputter = new XMLOutputter();
        builder = new SAXBuilder();
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

        final Document post = xml2
                ? new Document(new OrcaApiElement2.MedicalMod(model))
                : new Document(new OrcaApiElement.MedicalMod(model));
        final String xml = outputter.outputString(post);
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
            Document res = builder.build(new StringReader(resXml));
            MedicalreqResParser parser = new MedicalreqResParser(res);
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
                ? createSystem01ManagereqXml2()
                : createSystem01ManagereqXml();
        
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
            Document res = builder.build(new StringReader(resXml));
            DepartmentResParser parser = new DepartmentResParser(res);
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
                ? createSystem01ManagereqXml2()
                : createSystem01ManagereqXml();
        
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
            Document res = builder.build(new StringReader(resXml));
            PhysicianResParser parser = new PhysicianResParser(res);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            if (!API_NO_ERROR.equals(code)) {
                physicianList = parser.getList();
            }
        } catch (JDOMException | IOException ex) {
        }
    }
    
    private Entity toXmlEntity(String xml) {
        return Entity.entity(xml, MEDIATYPE_XML_UTF8);
    }
    
    private String createSystem01ManagereqXml() {
        
        final SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd");
        Element data = new Element(DATA);
        Element record1 = new Element(RECORD);
        data.addContent(record1);
        Element record2 = new Element(RECORD);
        record2.setAttribute(new Attribute(NAME, "system01_managereq"));
        record1.addContent(record2);
        Element string2 = new Element(STRING);
        string2.setAttribute(new Attribute(NAME, "Base_Date"));
        string2.addContent(frmt.format(new Date()));
        record2.addContent(string2);
        
        Document post = new Document(data);
        String xml = outputter.outputString(post);
        return xml;
    }
    
    private String createSystem01ManagereqXml2() {
        
        final SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd");
        Element data = new Element(DATA);
        Element elm1 = new Element("system01_managereq");
        elm1.setAttribute(TYPE, RECORD);
        Element elm2 = new Element(BASE_DATE);
        elm2.setAttribute(TYPE, STRING);
        elm2.addContent(frmt.format(new Date()));
        elm1.addContent(elm2);
        data.addContent(elm1);
        
        Document post = new Document(data);
        String xml = outputter.outputString(post);
        return xml;
    }
    
    public List<String> getOrcaVisit(String patientId, String ymd) throws Exception {
        
        final String path = "/api01rv2/medicalgetv2";
        final String deptCode = Project.getUserModel().getDepartmentModel().getDepartment();
        final String xml = createMedicalgetreqtXml2(patientId, ymd, deptCode);
        
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
            Document res = builder.build(new StringReader(resXml));
            MedicalgetResParser parser = new MedicalgetResParser(res);
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
        final String xml = createMedicalgetreqtXml2(patientId, ymd, deptCode);
        
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
            Document res = builder.build(new StringReader(resXml));
            MedicalgetResParser parser = new MedicalgetResParser(res);
            String code = parser.getApiResult();
            String msg = parser.getApiResultMessage();
            if (API_NO_ERROR.equals(code)) {
                return parser.getMedMasterItem();
            }
        } catch (JDOMException | IOException ex) {
        }
        return Collections.emptyList();
    }

    private String createMedicalgetreqtXml2(String patientId, String ymd, String deptCode) {

        Element data = new Element(DATA)
                .addContent(new Element("medicalgetreq").setAttribute(TYPE, RECORD)
                        .addContent(new Element("Patient_ID").setAttribute(TYPE,
                                        STRING).addContent(patientId))
                        .addContent(new Element("Perform_Date").setAttribute(TYPE,
                                        STRING).addContent(ymd))
                        .addContent(createMedicalInfoElem2(deptCode))
                );

        Document post = new Document(data);
        String xml = outputter.outputString(post);
        return xml;
    }

    private Element createMedicalInfoElem2(String deptCode) {

        Element elem = new Element("Medical_Information").setAttribute(TYPE, RECORD)
                .addContent(new Element("Department_Code").setAttribute(TYPE, STRING).addContent(deptCode))
                .addContent(new Element("Sequential_Number").setAttribute(TYPE, STRING))
                .addContent(new Element("Insurance_Combination_Number").setAttribute(TYPE, STRING))
                .addContent(new Element("HealthInsurance_Information").setAttribute(TYPE, RECORD)
                        .addContent(new Element("InsuranceProvider_Class").setAttribute(TYPE, STRING))
                        .addContent(new Element("InsuranceProvider_WholeName").setAttribute(TYPE, STRING))
                        .addContent(new Element("InsuranceProvider_Number").setAttribute(TYPE, STRING))
                        .addContent(new Element("HealthInsuredPerson_Symbol").setAttribute(TYPE, STRING))
                        .addContent(new Element("HealthInsuredPerson_Number").setAttribute(TYPE, STRING))
                );

        return elem;
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
    
    private void debug(int status, String entity) {
        if (DEBUG) {
            Logger logger = ClientContext.getClaimLogger();
            logger.debug("---------------------------------------");
            logger.debug("status = " + status);
            logger.debug(entity);
        }
    }
}
