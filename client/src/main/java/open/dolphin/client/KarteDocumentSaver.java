package open.dolphin.client;

import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.util.List;
import javax.swing.ImageIcon;
import open.dolphin.common.util.BeanUtils;
import open.dolphin.common.util.ZenkakuUtils;
import open.dolphin.delegater.DocumentDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.AccessRightModel;
import open.dolphin.infomodel.AdmissionModel;
import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.DocumentModel;
import open.dolphin.infomodel.ExtRefModel;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.KarteBean;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.ProgressCourse;
import open.dolphin.infomodel.SchemaModel;
import open.dolphin.project.Project;
import open.dolphin.util.ImageTool;

/**
 * KarteEditorから保存部分を分離
 *
 * @author masuda, Masuda Naika
 */
public class KarteDocumentSaver implements IInfoModel {

    private final KarteEditor editor;
    private final DocumentModel baseModel;
    private final boolean isDouble;

    public KarteDocumentSaver(KarteEditor editor) {
        this.editor = editor;
        this.baseModel = editor.getModel();
        this.isDouble = (editor.getMode() == KarteEditor.DOUBLE_MODE);
    }

    public void saveAndSend(SaveParams params) {

        if (isDouble) {
            saveDouble(params);
        } else {
            saveSingle(params);
        }
    }

    private void saveSingle(final SaveParams params) {

        DBTask task = new DBTask<DocumentModel, Void>(editor.getContext()) {

            @Override
            protected DocumentModel doInBackground() throws Exception {

                // 保存用DocumentModelを作成する
                DocumentModel docModel = createDocModelToSave(params);
                // SOAを構成する
                processSOAPane(docModel);
                // データベースに保存する
                DocumentDelegater.getInstance().postDocument(docModel);
                
                return docModel;
            }

            @Override
            protected void succeeded(DocumentModel docModel) {
 
                // callback
                DocInfoModel savedInfo = docModel.getDocInfoModel();
                editor.saveSingleDone(params, savedInfo);

            }
        };

        task.execute();
    }

    private void saveDouble(final SaveParams params) {

        DBTask task = new DBTask<DocumentModel, Void>(editor.getContext()) {

            @Override
            protected DocumentModel doInBackground() throws Exception {
                
                // 保存用DocumentModelを作成する
                DocumentModel docModel = createDocModelToSave(params);
                // SOAを構成する
                processSOAPane(docModel);
                // Pを構成する
                processPPane(docModel);
                // データベースに保存する
                DocumentDelegater.getInstance().postDocument(docModel);
                
                return docModel;
            }

            @Override
            protected void succeeded(DocumentModel docModel) {
                
                // callback
                DocInfoModel savedInfo = docModel.getDocInfoModel();
                editor.saveDoubleDone(params, savedInfo);
                
                // カルテ内容を送信する
                KarteContentSender sender = new KarteContentSender();
                sender.sendKarte(chart, docModel);
            }
        };

        task.execute();
    }
    
    // 保存用DocumentModelのSingle/Double共通部分を作成する
    private DocumentModel createDocModelToSave(SaveParams params) {
        
        // DocumentModelを新たに作成する
        DocumentModel docModel = new DocumentModel();
        // DocInfoを設定する。編集元を引き継ぐ
        docModel.setDocInfoModel(baseModel.getDocInfoModel());
        DocInfoModel docInfo = docModel.getDocInfoModel();

        // titleを設定する
        docInfo.setTitle(params.getTitle());
        // SaveParamsからConfirmDateを設定する
        docInfo.setConfirmDate(params.getConfirmed());

        //----------------------------------------------------
        // 修正でない場合は FirstConfirmDate = ConfirmDate にする
        // 修正の場合は FirstConfirmDate は既に設定されている
        // 修正でない新規カルテは parentId = null である
        //----------------------------------------------------
        if (docInfo.getParentId() == null) {
            // SaveParamsから取得
            docInfo.setFirstConfirmDate(params.getConfirmed());
        }
        // 編集元が仮保存カルテならばFirstConfirmDateを上書きする
        String oldStatus = docInfo.getStatus();
        if (STATUS_TMP.equals(oldStatus)) {
            docInfo.setFirstConfirmDate(params.getConfirmed());
        }

        // Status 仮保存か確定保存かを設定する
        if (params.isTmpSave()) {
            docInfo.setStatus(STATUS_TMP);
        } else {
            docInfo.setStatus(STATUS_FINAL);
        }

        // 送信に必要な環境を設定する
        boolean sendClaim = params.isSendClaim() && isDouble;
        docModel.getDocInfoModel().setSendClaim(sendClaim);
        boolean sendLabtest = params.isSendLabtest() && isDouble;
        docModel.getDocInfoModel().setSendLabtest(sendLabtest);
        boolean sendMml = params.isSendMML();
        docModel.getDocInfoModel().setSendMml(sendMml);

        // デフォルトのアクセス権を設定をする TODO
        AccessRightModel ar = new AccessRightModel();
        ar.setPermission(PERMISSION_ALL);
        ar.setLicenseeCode(ACCES_RIGHT_CREATOR);
        ar.setLicenseeName(ACCES_RIGHT_CREATOR_DISP);
        ar.setLicenseeCodeType(ACCES_RIGHT_FACILITY_CODE);
        docInfo.addAccessRight(ar);

        // 患者のアクセス権を設定をする
        if (params.isAllowPatientRef()) {
            ar = new AccessRightModel();
            ar.setPermission(PERMISSION_READ);
            ar.setLicenseeCode(ACCES_RIGHT_PATIENT);
            ar.setLicenseeName(ACCES_RIGHT_PATIENT_DISP);
            ar.setLicenseeCodeType(ACCES_RIGHT_PERSON_CODE);
            docInfo.addAccessRight(ar);
        }

        // 診療履歴のある施設のアクセス権を設定をする
        if (params.isAllowClinicRef()) {
            ar = new AccessRightModel();
            ar.setPermission(PERMISSION_READ);
            ar.setLicenseeCode(ACCES_RIGHT_EXPERIENCE);
            ar.setLicenseeName(ACCES_RIGHT_EXPERIENCE_DISP);
            ar.setLicenseeCodeType(ACCES_RIGHT_EXPERIENCE_CODE);
            docInfo.addAccessRight(ar);
        }

        // 入院モデルに退院日をセット
        AdmissionModel admission = docInfo.getAdmissionModel();
        if (admission != null && params.isRegistEndDate()) {
            admission.setEnded(docInfo.getConfirmDate());
        }

        //-------------------------------------
        // EJB3.0 Model の関係を構築する
        // confirmed, firstConfirmed は設定済み
        //-------------------------------------
        Chart chart = editor.getContext();
        KarteBean karte = chart.getKarte();
        docModel.setKarteBean(karte);                          // karte
        docModel.setUserModel(Project.getUserModel());         // 記録者
        docModel.setRecorded(docInfo.getConfirmDate());        // 記録日
        
        return docModel;
    }

    // DocumentModelに永続化SOA内容を構成する
    private void processSOAPane(DocumentModel docModel) {

        KartePane soaPane = editor.getSOAPane();
        DocInfoModel docInfo = docModel.getDocInfoModel();

        // SOAPane をダンプし model に追加する
        KartePaneDumper_2 dumper = new KartePaneDumper_2();
        KarteStyledDocument soaDoc = soaPane.getDocument();
        soaDoc.removeExtraCR();
        dumper.dump(soaDoc);

        // SOAモジュールを追加する
        ModuleModel[] soaModules = dumper.getModule();
        if (soaModules != null && soaModules.length > 0) {
            docModel.addModule(soaModules);
        }

        // ProgressCourseModule の ModuleInfo を保存しておく
        ModuleInfoBean progressInfo = null;
        ModuleInfoBean[] progressInfos = baseModel.getModuleInfo(MODULE_PROGRESS_COURSE);
        if (progressInfos != null && progressInfos.length > 0) {
            for (ModuleInfoBean info : progressInfos) {
                String stampRole = info.getStampRole();
                if (ROLE_SOA_SPEC.equals(stampRole)) {
                    progressInfo = info;
                }
            }
        }
        // 存在しない場合は新規に作成する
        if (progressInfo == null) {
            progressInfo = new ModuleInfoBean();
            progressInfo.setStampName(MODULE_PROGRESS_COURSE);
            progressInfo.setEntity(MODULE_PROGRESS_COURSE);
            progressInfo.setStampRole(ROLE_SOA_SPEC);
        }

        // ProgressCourse SOA を生成する
        ProgressCourse pc = new ProgressCourse();
        pc.setFreeText(dumper.getSpec());
        ModuleModel soaProgressModule = new ModuleModel();
        soaProgressModule.setModuleInfoBean(progressInfo);
        soaProgressModule.setModel(pc);
        docModel.addModule(soaProgressModule);

        // Moduleの永続化を設定する
        // soaSpecにもstampNumberが振られてしまうのだが…　いいのか？ TODO
        List<ModuleModel> mmList = docModel.getModules();
        if (mmList != null && !mmList.isEmpty()) {
            int size = mmList.size();
            for (int index = 0; index < size; ++index) {
                ModuleModel mm = mmList.get(index);
                moduleToPersist(docModel, mm, index);
            }
        }

        // Schema を追加する
        int maxImageWidth = ClientContext.getInt("image.max.width");
        int maxImageHeight = ClientContext.getInt("image.max.height");
        Dimension maxImageSize = new Dimension(maxImageWidth, maxImageHeight);

        SchemaModel[] schemas = dumper.getSchema();
        if (schemas != null && schemas.length > 0) {
            // 保存のため Icon を JPEG に変換する
            int size = schemas.length;
            for (int index = 0; index < size; ++index) {
                SchemaModel schema = schemas[index];
                schemaToPersist(docModel, schema, index, maxImageSize);
                docModel.addSchema(schema);
            }
        }

        // image があるかどうか
        boolean hasImage = (schemas != null && schemas.length > 0);
        docInfo.setHasImage(hasImage);
    }

    // DocumentModelに永続化P内容を構成する
    private void processPPane(DocumentModel docModel) {

        KartePane pPane = editor.getPPane();
        DocInfoModel docInfo = docModel.getDocInfoModel();

        // PPane をダンプし model に追加する
        KartePaneDumper_2 pdumper = new KartePaneDumper_2();
        KarteStyledDocument pDoc = pPane.getDocument();
        pDoc.removeExtraCR();
        pdumper.dump(pDoc);
        ModuleModel[] pModules = pdumper.getModule();

        // Pモジュールを追加する
        if (pModules != null && pModules.length > 0) {
            docModel.addModule(pModules);
        } else {
            docInfo.setSendClaim(false);
        }

        // ProgressCourseModule の ModuleInfo を保存しておく
        ModuleInfoBean progressInfo = null;
        ModuleInfoBean[] progressInfos = baseModel.getModuleInfo(MODULE_PROGRESS_COURSE);
        if (progressInfos != null && progressInfos.length > 0) {
            for (ModuleInfoBean info : progressInfos) {
                String stampRole = info.getStampRole();
                if (ROLE_P_SPEC.equals(stampRole)) {
                    progressInfo = info;
                }
            }
        }
        // 存在しない場合は新規に作成する
        if (progressInfo == null) {
            progressInfo = new ModuleInfoBean();
            progressInfo.setStampName(MODULE_PROGRESS_COURSE);
            progressInfo.setEntity(MODULE_PROGRESS_COURSE);
            progressInfo.setStampRole(ROLE_P_SPEC);
        }

        // ProgressCourse P を生成する
        ProgressCourse pProgressCourse = new ProgressCourse();
        pProgressCourse.setFreeText(pdumper.getSpec());
        ModuleModel pProgressModule = new ModuleModel();
        pProgressModule.setModuleInfoBean(progressInfo);
        pProgressModule.setModel(pProgressCourse);
        docModel.addModule(pProgressModule);

        // docInfoのFlagをリセット
        docInfo.setHasRp(false);
        docInfo.setHasTreatment(false);
        docInfo.setHasLaboTest(false);

        // Moduleの永続化を設定する
        // pSpecにもstampNumberが振られてしまうのだが…　いいのか？ TODO
        List<ModuleModel> mmList = docModel.getModules();
        if (mmList != null && !mmList.isEmpty()) {
            int size = mmList.size();
            for (int index = 0; index < size; ++index) {
                ModuleModel mm = mmList.get(index);
                moduleToPersist(docModel, mm, index);
            }
        }
    }

    private void moduleToPersist(DocumentModel docModel, ModuleModel mm, int index) {

        KarteBean karte = docModel.getKarteBean();
        DocInfoModel docInfo = docModel.getDocInfoModel();

        mm.setId(0L);                                       // unsaved-value
        mm.setKarteBean(karte);                             // Karte
        mm.setUserModel(Project.getUserModel());            // 記録者
        mm.setDocumentModel(docModel);                         // Document
        mm.setConfirmed(docInfo.getConfirmDate());          // 確定日
        mm.setFirstConfirmed(docInfo.getFirstConfirmDate());  // 適合開始日
        mm.setRecorded(docInfo.getConfirmDate());           // 記録日
        mm.setStatus(STATUS_FINAL);                         // status

        // 全角を Kill する
        if (mm.getModel() instanceof ClaimBundle) {
            ClaimBundle bundle = (ClaimBundle) mm.getModel();
            ClaimItem[] items = bundle.getClaimItem();
            if (items != null && items.length > 0) {
                for (ClaimItem item : items) {
                    String num = item.getNumber();
                    if (num != null) {
                        num = ZenkakuUtils.toHalfNumber(num);
                        item.setNumber(num);
                    }
                }
            }
            String bNum = bundle.getBundleNumber();
            if (bNum != null) {
                bNum = ZenkakuUtils.toHalfNumber(bNum);
                bundle.setBundleNumber(bNum);
            }
        }

        mm.setBeanBytes(BeanUtils.xmlEncode(mm.getModel()));

        // ModuleInfo を設定する。Name, Role, Entity は設定されている
        ModuleInfoBean mInfo = mm.getModuleInfoBean();
        mInfo.setStampNumber(index);

        // Flag設定
        String entity = mInfo.getEntity();
        if (entity != null) {
            switch (entity) {
                case ENTITY_MED_ORDER:
                    docInfo.setHasRp(true);
                    break;
                case ENTITY_TREATMENT:
                    docInfo.setHasTreatment(true);
                    break;
                case ENTITY_LABO_TEST:
                    docInfo.setHasLaboTest(true);
                    break;
            }
        }
    }

    private void schemaToPersist(DocumentModel docModel, SchemaModel schema, 
            int index, Dimension maxImageSize) {

        KarteBean karte = docModel.getKarteBean();
        DocInfoModel docInfo = docModel.getDocInfoModel();

        // 保存のため Icon を JPEG に変換する
        ImageIcon icon = schema.getIcon();
        icon = ImageTool.adjustImageSize(icon, maxImageSize);
        byte[] jpegByte = getJPEGByte(icon.getImage());
        schema.setJpegByte(jpegByte);
        schema.setIcon(null);

        // 画像との関係を設定する
        schema.setId(0L);                                         // unsaved
        schema.setKarteBean(karte);                               // Karte
        schema.setUserModel(Project.getUserModel());              // Creator
        schema.setDocumentModel(docModel);                        // Document
        schema.setConfirmed(docInfo.getConfirmDate());            // 確定日
        schema.setFirstConfirmed(docInfo.getFirstConfirmDate());  // 適合開始日
        schema.setRecorded(docInfo.getConfirmDate());             // 記録日
        schema.setStatus(STATUS_FINAL);                           // Status
        schema.setImageNumber(index);

        ExtRefModel ref = schema.getExtRefModel();
        StringBuilder sb = new StringBuilder();
        sb.append(docInfo.getDocId());
        sb.append("-");
        sb.append(index);
        sb.append(".jpg");
        ref.setHref(sb.toString());
    }

    private byte[] getJPEGByte(Image image) {
        try {
            return ImageTool.getJpegBytes(image);
        } catch (IOException ex) {
        }
        return null;
    }

}
