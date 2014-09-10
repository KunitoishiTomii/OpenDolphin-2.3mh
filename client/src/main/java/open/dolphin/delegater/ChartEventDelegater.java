package open.dolphin.delegater;

import java.util.concurrent.Future;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import open.dolphin.client.Dolphin;
import open.dolphin.infomodel.ChartEventModel;

/**
 * State変化関連のデレゲータ
 * @author masuda, Masuda Naika
 */
public class ChartEventDelegater extends BusinessDelegater {
    
    private static final String RES_CE = "chartEvent/";
    private static final String SUBSCRIBE_PATH = RES_CE + "subscribe";
    private static final String PUT_EVENT_PATH = RES_CE + "event";
    
    private final String clientUUID;
    
    private static final boolean debug = false;
    private static final ChartEventDelegater instance;
    
    static {
        instance = new ChartEventDelegater();
    }
    
    private ChartEventDelegater() {
        clientUUID = Dolphin.getInstance().getClientUUID();
    }
    
    public static ChartEventDelegater getInstance() {
        return instance;
    }
    
    public int putChartEvent(ChartEventModel evt) throws Exception {
        
        Entity entity = toJsonEntity(evt);

        Response response = getWebTarget()
                .path(PUT_EVENT_PATH)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String enityStr = response.readEntity(String.class);
        debug(status, enityStr);
        
        response.close();

        return Integer.parseInt(enityStr);
    }

    public Future<Response> subscribe() throws Exception {
        
        Future<Response> future = getAsyncWebTarget()
                .path(SUBSCRIBE_PATH)
                .request(MEDIATYPE_JSON_UTF8)
                .header(CLIENT_UUID, clientUUID)
                .async()
                .get();

        return future;
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
