package open.dolphin.server.pvt;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Logger;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.mbean.JndiUtil;
import open.dolphin.pvtclaim.PVTBuilder;
import open.dolphin.session.MasudaServiceBean;
import open.dolphin.session.PVTServiceBean;

/**
 * PvtServerThread, server
 *
 * @author masuda, Masuda Naika
 */
public class PvtServletServer {

    private static final Logger logger = Logger.getLogger(PvtServletServer.class.getSimpleName());
    
    private static final int DEFAULT_PORT = 5002;
    private final int port = DEFAULT_PORT;
    
    // ServerSocketのスレッド nio!
    private Thread sThread;
    private PvtServerThread serverThread;

    private static final PvtServletServer instance;
    
    static {
        instance = new PvtServletServer();
    }
    
    public static PvtServletServer getInstance() {
        return instance;
    }

    private PvtServletServer() {
    }

    public void start() {

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), port);
            String msg = "PVT Server is binded " + address;
            logger.info(msg);

            serverThread = new PvtServerThread(address);
            sThread = new Thread(serverThread, "PVT server socket");
            sThread.start();

        } catch (IOException ex) {
            String msg = "IOException while creating the ServerSocket: " + ex.toString();
            logger.warning(msg);
        }
    }

    public void dispose() {

        // ServerThreadを中止させる
        serverThread.stop();

        // ServerSocketのThread破棄する
        sThread.interrupt();
        sThread = null;
        
        logger.info("PVT Server stopped.");
    }

    // PvtClaimIOHanlderから呼ばれる
    public void postPvt(String pvtXml) {

        MasudaServiceBean masudaService = JndiUtil.getJndiResource(MasudaServiceBean.class);
        PVTServiceBean pvtService = JndiUtil.getJndiResource(PVTServiceBean.class);
        if (masudaService == null || pvtService == null) {
            return;
        }

        // pvtXmlからPatientVisitModelを作成する
        Reader reader = new StringReader(pvtXml);
        PVTBuilder builder = new PVTBuilder();
        builder.parse(reader);
        PatientVisitModel pvt = builder.getProduct();

        // pvtがnullなら何もせずリターン
        if (pvt == null) {
            return;
        }

        // CLAIM送信されたJMARI番号からfacilityIdを取得する
        String jmariNum = pvt.getJmariNumber();
        String fid = masudaService.getFidFromJmari(jmariNum);
        pvt.setFacilityId(fid);

        // 施設プロパティーを取得する
        Map<String, String> propMap = masudaService.getUserPropertyMap(fid);
        boolean pvtOnServer = Boolean.valueOf(propMap.get("pvtOnServer"));
        boolean fevOnServer = Boolean.valueOf(propMap.get("fevOnServer"));
        String sharePath = propMap.get("fevSharePath");
        boolean sendToFEV
                = sharePath != null
                && !sharePath.isEmpty()
                && fevOnServer;

        // PVT登録処理
        if (pvtOnServer) {
            pvtService.addPvt(pvt);
            StringBuilder sb = new StringBuilder();
            sb.append("PVT post: ").append(pvt.getPvtDate());
            sb.append(", Fid=").append(pvt.getFacilityId());
            sb.append("(").append(jmariNum).append(")");
            sb.append(", PtID=").append(pvt.getPatientId());
            sb.append(", Name=").append(pvt.getPatientName());
            logger.info(sb.toString());
        }

        // FEV-70 export処理
        if (sendToFEV) {
            String ptId = pvt.getPatientId();
            PatientVisitModel oldPvt = masudaService.getLastPvtInThisMonth(fid, ptId);
            FEV70Exporter fev = new FEV70Exporter(pvt, oldPvt, sharePath);
            fev.export();
        }
    }
}
