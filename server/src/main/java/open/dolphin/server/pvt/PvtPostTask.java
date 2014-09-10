package open.dolphin.server.pvt;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Logger;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.mbean.JndiUtil;
import open.dolphin.pvtclaim.PVTBuilder;
import open.dolphin.session.MasudaServiceBean;
import open.dolphin.session.PVTServiceBean;

/**
 * PvtPostTask
 *
 * @author masuda, Masuda Naika
 */
public class PvtPostTask implements Runnable {

    private static final Logger logger = Logger.getLogger(PvtPostTask.class.getSimpleName());
    
    private final String pvtXml;

    public PvtPostTask(String pvtXml) {
        this.pvtXml = pvtXml;
    }

    @Override
    public void run() {

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
        boolean fevOnServer = Boolean.valueOf(propMap.get("fevOnServer"));
        String sharePath = propMap.get("fevSharePath");
        boolean sendToFEV
                = sharePath != null
                && !sharePath.isEmpty()
                && fevOnServer;

        // PVT登録処理
        pvtService.addPvt(pvt);
        StringBuilder sb = new StringBuilder();
        sb.append("PVT post: ").append(pvt.getPvtDate());
        sb.append(", Fid=").append(pvt.getFacilityId());
        sb.append("(").append(jmariNum).append(")");
        sb.append(", PtID=").append(pvt.getPatientId());
        sb.append(", Name=").append(pvt.getPatientName());
        logger.info(sb.toString());

        // FEV-70 export処理
        if (sendToFEV) {
            String ptId = pvt.getPatientId();
            PatientVisitModel oldPvt = masudaService.getLastPvtInThisMonth(fid, ptId);
            FEV70Exporter fev = new FEV70Exporter(pvt, oldPvt, sharePath);
            fev.export();
        }
    }

}
