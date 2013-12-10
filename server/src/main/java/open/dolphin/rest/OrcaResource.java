package open.dolphin.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.ClaimMessageModel;
import open.dolphin.infomodel.OrcaSqlModel;
import open.dolphin.server.orca.OrcaService;

/**
 * OrcaResource
 * @author masuda, Masuda Naika
 */
@Path("orca")
public class OrcaResource extends AbstractResource {
    
    public static final String CLAIMRES_URL = "/rest/orca/claimres";
    
    private static final boolean debug = false;
    
    public OrcaResource() {
    }
    
    @POST
    @Path("query")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response executeQuery(String json) {
        
        OrcaSqlModel sqlModel = (OrcaSqlModel) 
                getConverter().fromJson(json, OrcaSqlModel.class);
        
        OrcaService.getInstance().executeSql(sqlModel);
        
        StreamingOutput so = getJsonOutStream(sqlModel);
        
        return Response.ok(so).build();
    }
    
    @POST
    @Path("claim")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_JSON_UTF8)
    public Response postClaim(String json) {

        ClaimMessageModel model = (ClaimMessageModel)
                getConverter().fromJson(json, ClaimMessageModel.class);
        
        ClaimMessageModel ret = OrcaService.getInstance().sendClaim(model);
        
        StreamingOutput so = getJsonOutStream(ret);
        
        return Response.ok(so).build();
    }
    
    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
