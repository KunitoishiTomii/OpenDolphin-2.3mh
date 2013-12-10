package open.dolphin.message;

import java.util.List;
import open.dolphin.infomodel.IInfoModel;

/**
 * DiseaseHelper
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public final class DiseaseHelper implements IMessageHelper {
        
    private String patientId;
    private String confirmDate;
    private String groupId;
    private String department;
    private String departmentDesc;
    private String creatorName;
    private String creatorId;
    private String creatorLicense;
    private String facilityName;
    private String jmariCode;
    private List<DiagnosisModuleItem> diagnosisModuleItems;
    
//masuda^
    private boolean useDefaultDept;
    
    public void setUseDefalutDept(boolean b) {
        useDefaultDept = b;
    }
    @Override
    public boolean isUseDefaultDept() {
        return useDefaultDept;
    }
//masuda$
    
    @Override
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    @Override
    public String getConfirmDate() {
        return confirmDate;
    }

    public void setConfirmDate(String confirmDate) {
        this.confirmDate = confirmDate;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDepartmentDesc() {
        return departmentDesc;
    }

    public void setDepartmentDesc(String departmentDesc) {
        this.departmentDesc = departmentDesc;
    }

    @Override
    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    @Override
    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    @Override
    public String getCreatorLicense() {
        return creatorLicense;
    }

    public void setCreatorLicense(String creatorLicense) {
        this.creatorLicense = creatorLicense;
    }

    @Override
    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    @Override
    public String getJmariCode() {
        return jmariCode;
    }

    public void setJmariCode(String jmariCode) {
        this.jmariCode = jmariCode;
    }

    public List<DiagnosisModuleItem> getDiagnosisModuleItems() {
        return diagnosisModuleItems;
    }

    public void setDiagnosisModuleItems(List<DiagnosisModuleItem> diagnosisModuleItems) {
        this.diagnosisModuleItems = diagnosisModuleItems;
    }

    @Override
    public String getCreatorDeptDesc() {
        return getDepartmentDesc();
    }

    @Override
    public String getCreatorDept() {
        return getDepartment();
    }

    public void setCreatorDeptDesc(String departmentDesc) {
        setDepartmentDesc(departmentDesc);
    }

    public void setCreatorDept(String department) {
        setDepartment(department);
    }

    @Override
    public String getGenerationPurpose() {
        return IInfoModel.PURPOSE_RECORD;   // 手抜き
    }

    @Override
    public String getDocId() {
        return null;
    }
}
