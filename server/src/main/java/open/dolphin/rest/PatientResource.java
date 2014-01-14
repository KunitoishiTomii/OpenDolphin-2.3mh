package open.dolphin.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.session.PatientServiceBean;

/**
 * PatientResource
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */

@Path("patient")
public class PatientResource extends AbstractResource {

    private static final boolean debug = false;
    
    @Inject
    private PatientServiceBean patientServiceBean;
    
    
    public PatientResource() {
    }


    @GET
    @Path("name/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientsByName(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String name = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByName(fid, name);
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(patients, typeRef);
        
        return Response.ok(so).build();
    }


    @GET
    @Path("kana/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientsByKana(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String kana = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByKana(fid, kana);
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(patients, typeRef);
        
        return Response.ok(so).build();
    }
    

    @GET
    @Path("digit/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientsByDigit(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String digit = param;
        debug(fid);
        debug(digit);

        List<PatientModel> patients = patientServiceBean.getPatientsByDigit(fid, digit);
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(patients, typeRef);
        
        return Response.ok(so).build();
    }


    @GET
    @Path("id/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientById(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String pid = param;

        PatientModel patient = patientServiceBean.getPatientById(fid, pid);
        
        StreamingOutput so = getJsonOutStream(patient);
        
        return Response.ok(so).build();
    }

    @GET
    @Path("pvt/{param}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientsByPvt(@PathParam("param") String param) {

        String fid = getRemoteFacility();
        String pvtDate = param;

        List<PatientModel> patients = patientServiceBean.getPatientsByPvtDate(fid, pvtDate);
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(patients, typeRef);
        
        return Response.ok(so).build();
    }


    @POST
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response postPatient(String json) {

        String fid = getRemoteFacility();

        PatientModel patient = (PatientModel)
                getConverter().fromJson(json, PatientModel.class);
        patient.setFacilityId(fid);

        long pk = patientServiceBean.addPatient(patient);
        String pkStr = String.valueOf(pk);
        debug(pkStr);

        return Response.ok(pkStr).build();
    }


    @PUT
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putPatient(String json) {

        String fid = getRemoteFacility();

        PatientModel patient = (PatientModel)
                getConverter().fromJson(json, PatientModel.class);
        patient.setFacilityId(fid);

        int cnt = patientServiceBean.update(patient);
        String pkStr = String.valueOf(cnt);
        debug(pkStr);

        return Response.ok(pkStr).build();
    }
    
    @GET
    @Path("list")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPatientList(@QueryParam(IDS) String ids) {
        
        String fid = getRemoteFacility();
        List<String> idList = getConverter().toStrList(ids);
        
        List<PatientModel> patients = patientServiceBean.getPatientList(fid, idList);
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(patients, typeRef);
        
        return Response.ok(so).build();
    }

    @GET
    @Path("insurances/{id}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getHealthInsurances(@PathParam("id") Long pk) {
        
        List<HealthInsuranceModel> list = patientServiceBean.getHealthInsurances(pk);
        
        TypeReference typeRef = new TypeReference<List<HealthInsuranceModel>>(){};
        StreamingOutput so = getJsonOutStream(list, typeRef);
        
        return Response.ok(so).build();
    }

//masuda^
    @GET
    @Path("past100day/{pastDay}/")
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response getPast100DayPatients(@PathParam("pastDay") int pastDay) {
        
        String fid = getRemoteFacility();
        List<PatientModel> patients = patientServiceBean.getPast100DayPatients(fid, pastDay);
        
        TypeReference typeRef = new TypeReference<List<PatientModel>>(){};
        StreamingOutput so = getJsonOutStream(patients, typeRef);
        
        return Response.ok(so).build();
    }
//masuda$
    
    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
