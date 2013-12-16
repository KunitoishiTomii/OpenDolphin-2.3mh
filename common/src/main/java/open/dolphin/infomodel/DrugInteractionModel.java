package open.dolphin.infomodel;

import java.io.Serializable;

/**
 * 薬剤相互作用のモデル
 *
 * @author masuda, Masuda Naika
 */
public class DrugInteractionModel implements Serializable {

    private final String srycd1;
    private final String srycd2;
    private final String sskijo;
    private final String syojyoucd;
    private final String brandName1;   // 対応先発品名
    private final String brandName2;

    public DrugInteractionModel(String srycd1, String srycd2, String sskijo, 
            String syojyoucd, String brandName1, String brandName2){
        this.srycd1 = srycd1;
        this.srycd2 = srycd2;
        this.sskijo = sskijo;
        this.syojyoucd = syojyoucd;
        this.brandName1 = brandName1;
        this.brandName2 = brandName2;
    }

    public String getSrycd1(){
        return srycd1;
    }
    public String getSrycd2(){
        return srycd2;
    }
    public String getSskijo(){
        return sskijo;
    }
    public String getSyojyoucd(){
        return syojyoucd;
    }
    public String getBrandname1() {
        return brandName1;
    }
    public String getBrandname2() {
        return brandName2;
    }
}
