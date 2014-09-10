package open.dolphin.infomodel;

import java.util.List;

/**
 * 担当医と患者IDのモデル
 * 
 * @author masuda, Masuda Naika
 */
public class DrPatientIdModel {
    
    private UserModel user;
    private List<String> patientIdList;
    
    public void setUserModel(UserModel user) {
        this.user = user;
    }
    public UserModel getUserModel() {
        return user;
    }
    public void setPatientIdList(List<String> list) {
        patientIdList = list;
    }
    public List<String> getPatientIdList() {
        return patientIdList;
    }

    @Override
    public String toString() {
        return user.getCommonName();
    }
}
