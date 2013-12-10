package open.dolphin.message;

import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.PVTHealthInsuranceModel;

/**
 * ClaimHelper
 *
 * @author Minagawa,Kazushi
 * @author modified by masuda, Masuda Naika
 */
public final class ClaimHelper implements IMessageHelper {
    
    /** 確定日 */
    private String confirmDate;
    
    /** Creator ID */
    private String creatorId;
    
    /** Creator 名 */
    private String creatorName;
    
    /** 診療科コード */
    private String creatorDept;
    
    /** 診療科名 */
    private String creatorDeptDesc;
    
    /** 医療資格 */
    private String creatorLicense;
    
    /** 患者ID */
    private String patientId;
    
    /** 生成目的 */
    private String generationPurpose;
    
    /** 文書ID */
    private String docId;
    
    /** 健康保険 GUID */
    private String healthInsuranceGUID;
    
    /** 健康保険コード値 */
    private String healthInsuranceClassCode;
    
    /** 健康保険説明 */
    private String healthInsuranceDesc;
    
    /** ClaimBundle 配列 */
    private ClaimBundle[] claimBundle;

    /** 診療科名 */
    private String deptName;

    /** 診療科コード */
    private String deptCode;

    /** 担当医名 */
    private String doctorName;

    /** 担当医コード(ORCA) */
    private String doctorId;

    /** JMARIコード*/
    private String jmariCode;

    /** 施設名*/
    private String facilityName;

    /** 選択された健康保険 2010-11-10 */
    private PVTHealthInsuranceModel selectedInsurance;
    
//masuda^   入院CLAIMフラグ
    private boolean admitFlag = false;
    
    public void setAdmitFlag(boolean b) {
        admitFlag = b;
    }
    public boolean getAdmitFlag() {
        return admitFlag;
    }
    public String getAdmitFlagStr() {
        return String.valueOf(admitFlag);
    }

    private boolean useDefaultDept;
    
    public void setUseDefalutDept(boolean b) {
        useDefaultDept = b;
    }
    @Override
    public boolean isUseDefaultDept() {
        return useDefaultDept;
    }
//masuda$
    
    
    public void setConfirmDate(String confirmDate) {
        this.confirmDate = confirmDate;
    }
    
    @Override
    public String getConfirmDate() {
        return confirmDate;
    }
    
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
    
    @Override
    public String getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    @Override
    public String getCreatorName() {
        return creatorName;
    }
    
    public void setCreatorLicense(String creatorLicense) {
        this.creatorLicense = creatorLicense;
    }
    
    @Override
    public String getCreatorLicense() {
        return creatorLicense;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    @Override
    public String getPatientId() {
        return patientId;
    }
    
    public void setGenerationPurpose(String generationPurpose) {
        this.generationPurpose = generationPurpose;
    }
    
    @Override
    public String getGenerationPurpose() {
        return generationPurpose;
    }
    
    public void setDocId(String docId) {
        this.docId = docId;
    }
    
    @Override
    public String getDocId() {
        return docId;
    }
    
    public void setHealthInsuranceGUID(String healthInsuranceGUID) {
        this.healthInsuranceGUID = healthInsuranceGUID;
    }
    
    public String getHealthInsuranceGUID() {
        // 2010-10-11 保険情報を送り返す場合は null
        return selectedInsurance != null ? null : healthInsuranceGUID;
    }
    
    public void setHealthInsuranceClassCode(String healthInsuranceClassCode) {
        this.healthInsuranceClassCode = healthInsuranceClassCode;
    }
    
    public String getHealthInsuranceClassCode() {
        // 2010-10-11 保険情報を送り返す場合
        return selectedInsurance != null ? selectedInsurance.getInsuranceClassCode() : healthInsuranceClassCode;
    }
    
    public void setHealthInsuranceDesc(String healthInsuranceDesc) {
        this.healthInsuranceDesc = healthInsuranceDesc;
    }
    
    public String getHealthInsuranceDesc() {
        // 2010-10-11 保険情報を送り返す場合
        return selectedInsurance != null ? selectedInsurance.getInsuranceClass() : healthInsuranceDesc;
    }
    
    public void setClaimBundle(ClaimBundle[] claimBundle) {
        this.claimBundle = claimBundle;
    }
    
    public ClaimBundle[] getClaimBundle() {
        return claimBundle;
    }
    
    public void addClaimBundle(ClaimBundle val) {
        
        if (val == null) {
            return;
        }

        if (claimBundle == null) {
            claimBundle = new ClaimBundle[1];
            claimBundle[0] = val;
            return;
        }
        int len = claimBundle.length;
        ClaimBundle[] dest = new ClaimBundle[len + 1];
        System.arraycopy(claimBundle, 0, dest, 0, len);
        claimBundle = dest;
        claimBundle[len] = val;
    }

    @Override
    public String getCreatorDept() {
        return creatorDept;
    }

    public void setCreatorDept(String creatorDept) {
        this.creatorDept = creatorDept;
    }

    @Override
    public String getCreatorDeptDesc() {
        return creatorDeptDesc;
    }

    public void setCreatorDeptDesc(String creatorDeptDesc) {
        this.creatorDeptDesc = creatorDeptDesc;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    @Override
    public String getJmariCode() {
        return jmariCode;
    }

    public void setJmariCode(String jmariCode) {
        this.jmariCode = jmariCode;
    }

    @Override
    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    //------------------------------------
    // 健康保険情報を送信する 2010-11-10
    //------------------------------------
    public PVTHealthInsuranceModel getSelectedInsurance() {
        return selectedInsurance;
    }

    public void setSelectedInsurance(PVTHealthInsuranceModel selectedInsurance) {
        this.selectedInsurance = selectedInsurance;
    }

    @Override
    public String getGroupId() {
        return getDocId();
    }
    
}
