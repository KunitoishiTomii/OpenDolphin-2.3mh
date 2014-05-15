package open.dolphin.server.pvt;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.mbean.JndiUtil;
import open.dolphin.pvtclaim.PVTBuilder;
import open.dolphin.session.MasudaServiceBean;
import open.dolphin.session.PVTServiceBean;

/**
 * PvtPostThread
 *
 * @author masuda, Masuda Naika
 */
public class PvtPostThread implements Runnable {

    private static final Logger logger = Logger.getLogger(PvtPostThread.class.getSimpleName());

    private final LinkedBlockingQueue<String> queue;

    public PvtPostThread() {
        queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {

        while (true) {
            try {
                String pvtXml = queue.take();
                postPvt(pvtXml);
            } catch (InterruptedException ex) {
                break;
            }
        }
    }

    public void offerPvt(String pvtXml) {
        queue.offer(pvtXml);
    }

    private void postPvt(String pvtXml) {

        MasudaServiceBean masudaService = (MasudaServiceBean) JndiUtil.getJndiResource(MasudaServiceBean.class);
        PVTServiceBean pvtService = (PVTServiceBean) JndiUtil.getJndiResource(PVTServiceBean.class);
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
