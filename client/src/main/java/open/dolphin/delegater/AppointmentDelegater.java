package open.dolphin.delegater;

import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.AppointmentModel;

/**
 * AppointmentDelegater
 * 
 * @author Kazushi Minagawa. Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class AppointmentDelegater extends BusinessDelegater {
    
    private static final boolean debug = false;
    private static final AppointmentDelegater instance;

    static {
        instance = new AppointmentDelegater();
    }

    public static AppointmentDelegater getInstance() {
        return instance;
    }

    private AppointmentDelegater() {
    }

    public int putAppointments(List<AppointmentModel> list) throws Exception {

        String path = "appo/";
        Entity entity = toJsonEntity(list);
        
        Response response = buildRequest(path, null, MediaType.TEXT_PLAIN_TYPE)
                .put(entity, Response.class);

        int status = response.getStatus();
        String entityStr = (String) response.readEntity(String.class);
        debug(status, entityStr);
        isHTTP200(status);

        return Integer.parseInt(entityStr);
    }

    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
