package open.dolphin.delegater;

import java.util.concurrent.Future;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.ChartEventModel;

/**
 * State変化関連のデレゲータ
 * @author masuda, Masuda Naika
 */
public class ChartEventDelegater extends BusinessDelegater {
    
    private static final String RES_CE = "chartEvent/";
    private static final String SUBSCRIBE_PATH = RES_CE + "subscribe";
    private static final String PUT_EVENT_PATH = RES_CE + "event";
    
    private static final boolean debug = false;
    private static final ChartEventDelegater instance;
    
    static {
        instance = new ChartEventDelegater();
    }
    
    private ChartEventDelegater() {
    }
    
    public static ChartEventDelegater getInstance() {
        return instance;
    }
    
    public int putChartEvent(ChartEventModel evt) throws Exception {
        
        Entity entity = toJsonEntity(evt);

        Response response = buildRequest(PUT_EVENT_PATH, null, MediaType.TEXT_PLAIN_TYPE)
                .put(entity, Response.class);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();

        return Integer.parseInt(enityStr);
    }

    public Future<Response> subscribe() throws Exception {
        
        Future<Response> future = 
                buildAsyncRequest(SUBSCRIBE_PATH, MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(Response.class);

        return future;
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
