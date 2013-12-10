package open.dolphin.message;

/**
 * IMessageHelper
 * 
 * @author masuda, Mausda Naika
 */
public interface IMessageHelper {

    public String getConfirmDate();
    
    public String getCreatorId();

    public String getCreatorName();

    public String getFacilityName();

    public String getJmariCode();

    public boolean isUseDefaultDept();

    public String getCreatorDeptDesc();

    public String getCreatorDept();

    public String getCreatorLicense();

    public String getPatientId();
    
    public String getGenerationPurpose();
    
    public String getDocId();
    
    public String getGroupId();
}
