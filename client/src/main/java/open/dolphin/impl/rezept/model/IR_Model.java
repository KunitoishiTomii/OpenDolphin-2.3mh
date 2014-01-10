package open.dolphin.impl.rezept.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import open.dolphin.impl.rezept.RezeUtil;

/**
 * 医療機関情報レコード
 *
 * @author masuda, Masuda Naika
 */
public class IR_Model implements IRezeModel {

    public static final String ID = "IR";

    private int shinsaKikanNumber;      // 1:社保 2:国保
    private int prefectureCode;         // 都道府県
    private int tenTable;               // 点数表
    private String facilityCode;        // 医療機関コード
    private String facilityName;        // 医療機関名称
    private Date billDate;              // 請求年月
    private String telephone;           // 電話番号
    
    private String nyugaikbn;      // 1:入院、2:入院外
    
    private LinkedList<RE_Model> reModelList;
    private GO_Model goModel;
    
    public String getNyugaikbn() {
        return nyugaikbn;
    }
    public void setNyugaikbn(String nyugaikbn) {
        this.nyugaikbn = nyugaikbn;
    }
    
    public int getShinsaKikanNumber() {
        return shinsaKikanNumber;
    }
    public String getShinsaKikanStr() {
        return RezeUtil.getInstance().getShinsaKikanStr(shinsaKikanNumber);
    }
    public void setShinsaKikan(int shinsaKikanNumber) {
        this.shinsaKikanNumber = shinsaKikanNumber;
    }
    public void setTenTable(int tenTable) {
        this.tenTable = tenTable;
    }
    public void setBillDate(String ym) {
        billDate = RezeUtil.getInstance().fromYearMonth(ym);
    }
    
    public String getPrefecture() {
        return RezeUtil.getInstance().getPrefecture(prefectureCode);
    }

    public String getTenTable() {
        return RezeUtil.getInstance().getTenTable(tenTable);
    }

    public String getFacilityCode() {
        return facilityCode;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public Date getBillDate() {
        return billDate;
    }

    public String getTelephone() {
        return telephone;
    }
    
    public List<RE_Model> getReModelList() {
        return reModelList;
    }
    public RE_Model getCurrentREModel() {
        return reModelList.getLast();
    }
    public void addReModel(RE_Model model) {
        if (reModelList == null) {
            reModelList = new LinkedList<>();
        }
        reModelList.add(model);
    }
    public void setGOModel(GO_Model model) {
        goModel = model;
    }
    public GO_Model getGOModel() {
        return goModel;
    }

    @Override
    public void parseLine(String csv) {
        String[] tokens = csv.split(CAMMA);
        shinsaKikanNumber = Integer.parseInt(tokens[1]);
        prefectureCode = Integer.parseInt(tokens[2]);
        tenTable = Integer.parseInt(tokens[3]);
        facilityCode = tokens[4].trim();
        facilityName = tokens[6].trim();
        billDate = RezeUtil.getInstance().fromYearMonth(tokens[7]);
        telephone = tokens[9].trim();
    }
}
