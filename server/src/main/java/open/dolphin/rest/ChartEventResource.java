package open.dolphin.rest;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.ChartEventModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.mbean.AsyncResponseModel;
import open.dolphin.mbean.ServletContextHolder;
import open.dolphin.session.ChartEventServiceBean;

/**
 * ChartEventResource
 * @author masuda, Masuda Naika
 */
@Path("chartEvent")
public class ChartEventResource extends AbstractResource {
    
    private static final boolean debug = false;
    
    private static final int asyncTimeout = 60; // 60 minutes
    
    @Inject
    private ChartEventServiceBean eventServiceBean;
    
    @Inject
    private ServletContextHolder contextHolder;

    
    // JAX-RS 2.0, use AsyncResponse
    @GET
    @Path("subscribe")
    public void subscribe(@Suspended final AsyncResponse ar) {
        
        String fid = getRemoteFacility();
        String clientUUID = servletReq.getHeader(IInfoModel.CLIENT_UUID);
        
        final AsyncResponseModel arModel = new AsyncResponseModel(ar, fid, clientUUID);
        
        // register timeout handler
        ar.setTimeout(asyncTimeout, TimeUnit.MINUTES);
        ar.setTimeoutHandler(new TimeoutHandler(){

            @Override
            public void handleTimeout(AsyncResponse ar) {
                contextHolder.getAsyncResponseList().remove(arModel);
                ar.resume(Response.noContent().status(Response.Status.SERVICE_UNAVAILABLE).build());
            }
        });
        
        // register callbacks
        ar.register(new CompletionCallback(){

            @Override
            public void onComplete(Throwable thrwbl) {
                contextHolder.getAsyncResponseList().remove(arModel);
            }
        });
        ar.register(new ConnectionCallback(){

            @Override
            public void onDisconnect(AsyncResponse ar) {
                contextHolder.getAsyncResponseList().remove(arModel);
            }
        });
        
        contextHolder.getAsyncResponseList().add(arModel);
    }
    
    @PUT
    @Path("event")
    @Consumes(MEDIATYPE_JSON_UTF8)
    @Produces(MEDIATYPE_TEXT_UTF8)
    public Response putChartEvent(String json) {
        
        ChartEventModel msg = getConverter().fromJson(json, ChartEventModel.class);

        int cnt = eventServiceBean.processChartEvent(msg);

        return Response.ok(String.valueOf(cnt)).build();
    }
    
    @Override
    protected void debug(String msg) {
        if (debug || DEBUG) {
            super.debug(msg);
        }
    }
}
