package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * 適応症モデル子さん
 * 
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_indication_item")
public class IndicationItem implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    private Boolean disableFlg; // 無効か有効か
    private Boolean notCondition; // not条件かどうか
    
    private String keyword;
    
    @JsonBackReference // bi-directional references
    @ManyToOne
    @JoinColumn(name="indication_id", nullable=false)
    private IndicationModel indication;
    
    @Transient
    private boolean validFlg;
    
    public IndicationItem() {
        disableFlg = false;
        notCondition = false;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    public void setDisable(boolean disableFlg) {
        this.disableFlg = disableFlg;
    }
    public void setNotCondition(boolean notCondition) {
        this.notCondition = notCondition;
    }
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    public void setValid(boolean validFlg) {
        this.validFlg = validFlg;
    }
    public void setIndicationModel(IndicationModel indication) {
        this.indication = indication;
    }
    
    public long getId() {
        return id;
    }
    public Boolean isDisabled() {
        return disableFlg;
    }
    public Boolean isNotCondition() {
        return notCondition;
    }
    public String getKeyword() {
        return keyword;
    }
    public boolean isValid() {
        return validFlg;
    }
    public IndicationModel getIndicationModel() {
        return indication;
    }
}
