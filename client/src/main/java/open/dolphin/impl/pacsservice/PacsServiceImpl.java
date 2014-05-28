package open.dolphin.impl.pacsservice;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import open.dolphin.client.MainWindow;
import open.dolphin.client.PacsService;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import org.apache.log4j.Logger;
import org.dcm4che2.data.*;
import org.dcm4che2.net.*;
import org.dcm4che2.net.service.StorageService;

/**
 * Pacs接続サービス
 *
 * @author masuda, Masuda Naika
 */
public class PacsServiceImpl implements PacsService {

    // クエリで取得する追加の情報
    private static final String[] RETURN_KEYS
            = new String[]{"PatientName", "PatientSex", "PatientBirthDate", "ModalitiesInStudy", "StudyDescription"};
    
    // 受け入れるModality
    private static final String[] STORE_TCs
            = new String[]{"CR", "US", "CT", "MR", "SC", "ES"};
    
    // 受け入れるTransferSyntax
    private static final String[] STORE_TSs = {
        UID.ExplicitVRLittleEndian,
        UID.ImplicitVRLittleEndian};

    private static final String[] STUDY_LEVEL_FIND_CUID = {
        UID.StudyRootQueryRetrieveInformationModelFIND,
        UID.PatientRootQueryRetrieveInformationModelFIND,
        UID.PatientStudyOnlyQueryRetrieveInformationModelFINDRetired};

    private static final String[] STUDY_LEVEL_MOVE_CUID = {
        UID.StudyRootQueryRetrieveInformationModelMOVE,
        UID.PatientRootQueryRetrieveInformationModelMOVE,
        UID.PatientStudyOnlyQueryRetrieveInformationModelMOVERetired};

    private static final int[] STUDY_RETURN_KEYS = {
        Tag.StudyDate,
        Tag.StudyTime,
        Tag.AccessionNumber,
        Tag.StudyID,
        Tag.StudyInstanceUID,
        Tag.NumberOfStudyRelatedSeries,
        Tag.NumberOfStudyRelatedInstances};

    private static final int[] MOVE_KEYS = {
        Tag.QueryRetrieveLevel,
        Tag.PatientID,
        Tag.StudyInstanceUID,
        Tag.SeriesInstanceUID,
        Tag.SOPInstanceUID,};

    private static final String[] IVRLE_TS = {
        UID.ImplicitVRLittleEndian};

    private static enum CUID {

        CR(UID.ComputedRadiographyImageStorage),
        CT(UID.CTImageStorage),
        MR(UID.MRImageStorage),
        US(UID.UltrasoundImageStorage),
        NM(UID.NuclearMedicineImageStorage),
        PET(UID.PositronEmissionTomographyImageStorage),
        SC(UID.SecondaryCaptureImageStorage),
        XA(UID.XRayAngiographicImageStorage),
        XRF(UID.XRayRadiofluoroscopicImageStorage),
        DX(UID.DigitalXRayImageStorageForPresentation),
        MG(UID.DigitalMammographyXRayImageStorageForPresentation),
        PR(UID.GrayscaleSoftcopyPresentationStateStorageSOPClass),
        KO(UID.KeyObjectSelectionDocumentStorage),
        SR(UID.BasicTextSRStorage),
        ES(UID.VLEndoscopicImageStorage);
        
        final String uid;

        CUID(String uid) {
            this.uid = uid;
        }

    }

    public static final String PACS_IMAGE_ARRIVED = "pacsServiceImageArrived";
    private String name;
    private MainWindow context;
    private PropertyChangeSupport boundSupport;

    // dcm4che2 tool kit
    private final int priority = 0;
    private final int cancelAfter = Integer.MAX_VALUE;
    private final Executor executor;
    private final Device device;
    private final NetworkConnection conn;
    private final NetworkApplicationEntity ae;
    private final NetworkApplicationEntity remoteAE;
    private final NetworkConnection remoteConn;
    private List<TransferCapability> storageCapability;
    
    
    public PacsServiceImpl() {
        
        String pacsLocalAE = Project.getString(MiscSettingPanel.PACS_LOCAL_AE, MiscSettingPanel.DEFAULT_PACS_LOCAL_AE);
        device = new Device(pacsLocalAE);
        executor = new NewThreadExecutor(pacsLocalAE);
        conn = new NetworkConnection();
        remoteConn = new NetworkConnection();
        remoteAE = new NetworkApplicationEntity();
        ae = new NetworkApplicationEntity();
        ae.setAETitle(pacsLocalAE);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(PACS_IMAGE_ARRIVED, listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.removePropertyChangeListener(PACS_IMAGE_ARRIVED, listener);
    }

    @Override
    public void start() {
        setupPacsConnection();
        if (conn.isListening()) {
            try {
                conn.bind(executor);
            } catch (IOException ex) {
                getLogger().warn("Failed to start PacsService");
            }
            getLogger().info("PacsService listening on port " + conn.getPort());
        }
    }

    @Override
    public void stop() {
        if (conn != null && conn.isListening()) {
            conn.unbind();
        }
        getLogger().info("PacsService stopped.");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public MainWindow getContext() {
        return context;
    }

    @Override
    public void setContext(MainWindow context) {
        this.context = context;
    }

    private Logger getLogger() {
        return Logger.getLogger("pacsService.logger");
    }

    // Pacs通信の初期設定
    private void setupPacsConnection() {

        String pacsRemoteHost = Project.getString(MiscSettingPanel.PACS_REMOTE_HOST, MiscSettingPanel.DEFAULT_PACS_REMOTE_HOST);
        int pacsRemotePort = Project.getInt(MiscSettingPanel.PACS_REMOTE_PORT, MiscSettingPanel.DEFAULT_PACS_REMOTE_PORT);
        String pacsRemoteAE = Project.getString(MiscSettingPanel.PACS_REMOTE_AE, MiscSettingPanel.DEFAULT_PACS_REMOTE_AE);
        String pacsLocalHost = Project.getString(MiscSettingPanel.PACS_LOCAL_HOST, MiscSettingPanel.DEFAULT_PACS_LOCAL_HOST);
        int pacsLocalPort = Project.getInt(MiscSettingPanel.PACS_LOCAL_PORT, MiscSettingPanel.DEFAULT_PACS_LOCAL_PORT);
        
        conn.setHostname(pacsLocalHost);
        conn.setPort(pacsLocalPort);
        ae.setNetworkConnection(conn);
        ae.setAssociationInitiator(true);
        ae.setAssociationAcceptor(true);
        device.setNetworkApplicationEntity(ae);
        device.setNetworkConnection(conn);

        remoteConn.setHostname(pacsRemoteHost);
        remoteConn.setPort(pacsRemotePort);
        remoteAE.setAETitle(pacsRemoteAE);
        remoteAE.setInstalled(true);
        remoteAE.setAssociationAcceptor(true);
        remoteAE.setNetworkConnection(remoteConn);

        setupStorageCapability();
    }

    @Override
    public List<DicomObject> findStudy(String[] matchingKeys) throws Exception {

        final DicomObject keys = new BasicDicomObject();
        keys.putString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");

        // 検索条件の設定
        for (int i = 1; i < matchingKeys.length; i++, i++) {
            keys.putString(Tag.toTagPath(matchingKeys[i - 1]), null, matchingKeys[i]);
        }
        // return keyの設定
        for (int tag : STUDY_RETURN_KEYS) {
            keys.putNull(tag, null);
        }
        for (String returnKey : RETURN_KEYS) {
            keys.putNull(Tag.toTagPath(returnKey), null);
        }
        // SpecificCharacterも取得する
        keys.putNull(Tag.SpecificCharacterSet, VR.CS);
        
        List<DicomObject> result = new ArrayList<>();
        Association assoc = null;

        synchronized (ae) {

            try {
                setFindTransferCapability();
                assoc = ae.connect(remoteAE, executor);

                TransferCapability tc = selectTransferCapability(assoc, STUDY_LEVEL_FIND_CUID);
                String cuid = tc.getSopClass();
                String tsuid = selectTransferSyntax(tc);

                DimseRSP rsp = assoc.cfind(cuid, priority, keys, tsuid, cancelAfter);
                while (rsp.next()) {
                    DicomObject cmd = rsp.getCommand();
                    if (CommandUtils.isPending(cmd)) {
                        DicomObject data = rsp.getDataset();
                        result.add(data);
                    }
                }
            } finally {
                if (assoc != null) {
                    assoc.release(true);
                }
            }
        }

        return result;
    }

    @Override
    public void retrieveDicomObject(final DicomObject obj) throws Exception {
        
        DimseRSPHandler rspHandler = new DimseRSPHandler() {
            @Override
            public void onDimseRSP(Association as, DicomObject cmd, DicomObject data) {
            }
        };
        
        Association assoc = null;
        synchronized (ae) {

            try {
                setMoveTransferCapability();
                assoc = ae.connect(remoteAE, executor);

                TransferCapability tc = selectTransferCapability(assoc, STUDY_LEVEL_MOVE_CUID);
                if (tc == null) {
                    throw new NoPresentationContextException(UIDDictionary.getDictionary().prompt(STUDY_LEVEL_MOVE_CUID[0])
                            + " not supported by " + assoc.getRemoteAET());
                }

                String cuid = tc.getSopClass();
                String tsuid = selectTransferSyntax(tc);
                DicomObject dokeys = obj.subSet(MOVE_KEYS);
                assoc.cmove(cuid, priority, dokeys, tsuid, assoc.getLocalAET(), rspHandler);

                assoc.waitForDimseRSP();
            } catch (ConfigurationException | IOException | InterruptedException ex) {
                getLogger().error(ex);
            } finally {
                try {
                    if (assoc != null) {
                        assoc.release(true);
                    }
                } catch (InterruptedException ex) {
                }

            }
        }
    }

    private void setupStorageCapability() {

        storageCapability = new ArrayList<>();

        for (String storeTC : STORE_TCs) {
            try {
                String cuid = CUID.valueOf(storeTC).uid;
                TransferCapability tc = new TransferCapability(cuid, STORE_TSs, TransferCapability.SCP);
                storageCapability.add(tc);
            } catch (IllegalArgumentException e) {
            }
        }

        String[] cuids = new String[storageCapability.size()];
        for (int i = 0; i < cuids.length; ++i) {
            cuids[i] = storageCapability.get(i).getSopClass();
        }

        StorageService service = new StorageService(cuids) {

            @Override
            protected void onCStoreRQ(Association as, int pcid, DicomObject rq,
                    PDVInputStream dataStream, String tsuid, DicomObject rsp)
                    throws IOException, DicomServiceException {

                String cuid = rq.getString(Tag.AffectedSOPClassUID);
                String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
                DicomObject object = dataStream.readDataset();
                object.initFileMetaInformation(cuid, iuid, tsuid);

                // 受信した旨firePropertyChangeする。newにDicomObjectを登録する
                boundSupport.firePropertyChange(PACS_IMAGE_ARRIVED, null, object);
                getLogger().debug("DicomObject Received:" + object.getString(Tag.SOPInstanceUID));
            }
        };
        ae.register(service);
    }

    private void setFindTransferCapability() {

        List<TransferCapability> tcList = new ArrayList<>();
        for (String cuid : STUDY_LEVEL_FIND_CUID) {
            tcList.add(mkFindTC(cuid, IVRLE_TS));
        }
        TransferCapability[] tcs = new TransferCapability[tcList.size()];
        tcList.toArray(tcs);
        ae.setTransferCapability(tcs);
    }

    private TransferCapability mkFindTC(String cuid, String[] ts) {

        ExtQueryTransferCapability tc
                = new ExtQueryTransferCapability(cuid, ts, TransferCapability.SCU);
        tc.setExtInfoBoolean(ExtQueryTransferCapability.RELATIONAL_QUERIES, false);
        tc.setExtInfoBoolean(ExtQueryTransferCapability.DATE_TIME_MATCHING, false);
        tc.setExtInfoBoolean(ExtQueryTransferCapability.FUZZY_SEMANTIC_PN_MATCHING, false);

        return tc;
    }

    private void setMoveTransferCapability() {

        List<TransferCapability> tcList = new ArrayList<>();
        for (String cuid : STUDY_LEVEL_MOVE_CUID) {
            tcList.add(mkRetrieveTC(cuid, IVRLE_TS));
        }
        tcList.addAll(storageCapability);

        TransferCapability[] tcs = new TransferCapability[tcList.size()];
        tcList.toArray(tcs);
        ae.setTransferCapability(tcs);

    }

    private TransferCapability mkRetrieveTC(String cuid, String[] ts) {

        ExtRetrieveTransferCapability tc
                = new ExtRetrieveTransferCapability(cuid, ts, TransferCapability.SCU);
        tc.setExtInfoBoolean(ExtRetrieveTransferCapability.RELATIONAL_RETRIEVAL, false);

        return tc;
    }

    private String selectTransferSyntax(TransferCapability tc) {

        String[] tcuids = tc.getTransferSyntax();
        if (Arrays.asList(tcuids).indexOf(UID.DeflatedExplicitVRLittleEndian) != -1) {
            return UID.DeflatedExplicitVRLittleEndian;
        }
        return tcuids[0];
    }

    private TransferCapability selectTransferCapability(Association assoc, String[] cuids) {

        for (String cuid : cuids) {
            TransferCapability tc = assoc.getTransferCapabilityAsSCU(cuid);
            if (tc != null) {
                return tc;
            }
        }
        return null;
    }
}
