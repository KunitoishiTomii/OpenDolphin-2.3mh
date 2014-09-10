package open.dolphin.impl.rezept.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import open.dolphin.impl.rezept.RezeUtil;
import open.dolphin.impl.rezept.filter.CheckResult;

/**
 * レセプト共通レコード
 * 
 * @author masuda, Masuda Naika
 */
public class RE_Model implements IRezeModel {
    
    public static final String ID = "RE";
    
    private int rezeType;       // レセプト種別
    private Date billDate;      // 診療年月
    private String name;        // 氏名
    private String sex;         // 性別
    private Date birthday;      // 生年月日
    private Date admitDate;     // 入院年月日
    private String patientId;   // 患者番号
    private int checkFlag;      // チェック
    
    private String nyugaikbn;   // 1:入院、2:入院外
    
    private HO_Model hoModel;
    private final List<KO_Model> koModelList;
    private KH_Model khModel;
    private final List<SY_Model> syModelList;
    private List<IRezeItem> itemList;
    private final List<SJ_Model> sjModelList;
    
    private List<CheckResult> checkResults;
    
    private String kanaName;
    
    private IR_Model irModel;
    
    public RE_Model() {
        koModelList = new ArrayList<>();
        syModelList = new ArrayList<>();
        itemList = new ArrayList<>();
        sjModelList = new ArrayList<>();
    }
    
    public IR_Model getIrModel() {
        return irModel;
    }
    public void setIrModel(IR_Model irModel) {
        this.irModel = irModel;
    }
    
    public String getRezeType(int num) {
        return RezeUtil.getInstance().getRezeTypeDesc(num, rezeType);
    }
    public Date getBillDate() {
        return billDate;
    }
    public String getName() {
        return name;
    }
    public String getSex() {
        return sex;
    }
    public Date getBirthday() {
        return birthday;
    }
    public Date getAdmitDate() {
        return admitDate;
    }
    public String getPatientId() {
        return patientId;
    }
    public void setCheckFlag(int flag) {
        checkFlag = flag;
    }
    public int getCheckFlag() {
        return checkFlag;
    }
    public String getNyugaikbn() {
        return nyugaikbn;
    }
    public void setNyugaikbn(String nyugaikbn) {
        this.nyugaikbn = nyugaikbn;
    }

    public int getAge() {
        GregorianCalendar gc = new GregorianCalendar();
        int d = gc.get(GregorianCalendar.DATE);
        int ym = gc.get(GregorianCalendar.YEAR) * 12 + gc.get(GregorianCalendar.MONTH) + 1;
        gc.setTime(birthday);
        int bd = gc.get(GregorianCalendar.DATE);
        int bym = gc.get(GregorianCalendar.YEAR) * 12 + gc.get(GregorianCalendar.MONTH) + 1;
        int age = (ym - bym) / 12;
        if (bd > d) {
            age--;
        }
        return age;
    }
    
    public List<IRezeItem> getItemList() {
        return itemList;
    }
    public void addItem(IRezeItem item) {
        itemList.add(item);
    }
    public void setItemList(List<IRezeItem> itemList) {
        this.itemList = itemList;
    }
    public List<SJ_Model> getSJModelList() {
        return sjModelList;
    }
    public void addSJModel(SJ_Model sjModel) {
        sjModelList.add(sjModel);
    }
    public void setHOModel(HO_Model model) {
        hoModel = model;
    }
    public HO_Model getHOModel() {
        return hoModel;
    }
    public void addKOModel(KO_Model model) {
        koModelList.add(model);
    }
    public List<KO_Model> getKOModelList() {
        return koModelList;
    }
    public void setKHModel(KH_Model model) {
        khModel = model;
    }
    public KH_Model getKHModel() {
        return khModel;
    }
    public List<SY_Model> getSYModelList() {
        return syModelList;
    }
    public void addSYModel(SY_Model model) {
        syModelList.add(model);
    }
    public String getKanaName() {
        return kanaName;
    }
    public void setKanaName(String kanaName) {
        this.kanaName = kanaName;
    }
    
    @Override
    public void parseLine(String csv) {
        String[] tokens = TokenSplitter.split(csv);
        rezeType = Integer.parseInt(tokens[2]);
        billDate = RezeUtil.getInstance().fromYearMonth(tokens[3]);
        name = tokens[4].trim();
        sex = RezeUtil.getInstance().getSexDesc(tokens[5]);
        birthday = RezeUtil.getInstance().fromYearMonthDate(tokens[6]);
        admitDate = RezeUtil.getInstance().fromYearMonthDate(tokens[8]);
        patientId = tokens[13].trim();
    }
    
    public void setCheckResults(List<CheckResult> results) {
        this.checkResults = results;
    }
    public List<CheckResult> getCheckResults() {
        return checkResults;
    }
    public void addCheckResult(CheckResult result) {
        if (result != null) {
            if (checkResults == null) {
                checkResults = new ArrayList<>();
            }
            checkResults.add(result);
            checkFlag = Math.max(checkFlag, result.getResult());
        }
    }
    public void addCheckResults(List<CheckResult> results) {
        for (CheckResult result : results) {
            addCheckResult(result);
        }
    }
    
    public void initCheckResult() {
        
        if (checkResults!= null) {
            checkResults.clear();
        }
        if (syModelList != null) {
            for (SY_Model syModel : syModelList) {
                syModel.setHitCount(0);
                syModel.setPass(true);
            }
        }
        if (itemList != null) {
            for (IRezeItem item : itemList) {
                item.setHitCount(0);
                item.setPass(true);
            }
        }
        checkFlag = 0;
        
    }
}
