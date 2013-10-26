package open.dolphin.delegater;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import open.dolphin.client.ClientContext;
import open.dolphin.infomodel.*;
import open.dolphin.util.BeanUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * Bsiness Delegater のルートクラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class BusinessDelegater {

    protected static final String CAMMA = ",";
    
    protected Logger logger;

    protected boolean DEBUG;
    
    public BusinessDelegater() {
        logger = ClientContext.getDelegaterLogger();
        DEBUG = (logger.getLevel() == Level.DEBUG);
    }
    
    protected Invocation.Builder buildRequest(String path, MultivaluedMap<String, String> qmap, MediaType mt) {
        return JerseyClient.getInstance().buildRequest(path, qmap, mt);
    }
    
    protected Invocation.Builder buildAsyncRequest(String path, MediaType mt) {
        return JerseyClient.getInstance().buildAsyncRequest(path, mt);
    }

    protected void debug(int status, String entity) {
        logger.debug("---------------------------------------");
        logger.debug("status = " + status);
        logger.debug(entity);
    }
    
    protected JsonConverter getConverter() {
        return JsonConverter.getInstance();
    }
    
    protected Entity toJsonEntity(Object obj) {
        return Entity.json(getConverter().toJson(obj));
    }
    
    protected Entity toTextEntity(String text) {
        return Entity.text(text);
    }

    protected String toRestFormat(Date date) {
        try {
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_DF_FORMAT);
            return frmt.format(date);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    /**
     * バイナリの健康保険データをオブジェクトにデコードする。
     */
    protected void decodePvtHealthInsurance(Collection<PatientVisitModel> list) {
        
        if (list != null && !list.isEmpty()) {
            for (PatientVisitModel pm : list) {
                decodeHealthInsurance(pm.getPatientModel());
            }
        }
    }

    protected void decodeHealthInsurance(PatientModel patient) {

        // Health Insurance を変換をする beanXML2PVT
        Collection<HealthInsuranceModel> c = patient.getHealthInsurances();

        if (c != null && !c.isEmpty()) {

            for (HealthInsuranceModel model : c) {
                try {
                    // byte[] を XMLDecord
                    PVTHealthInsuranceModel hModel = (PVTHealthInsuranceModel) 
                            BeanUtils.xmlDecode(model.getBeanBytes());
                    patient.addPvtHealthInsurance(hModel);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            c.clear();
            patient.setHealthInsurances(null);
        }
    }
    
    protected int checkHttpStatus(Response response) throws Exception {
        int status = response.getStatus();
        if (status / 100 != 2) {
            String msg = "HTTP" + String.valueOf(status);
            response.close();
            throw new Exception(msg);
        }
        return status;
    }
}
