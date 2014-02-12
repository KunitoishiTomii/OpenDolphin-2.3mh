package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.hibernate.annotations.BatchSize;

/**
 * 適応症モデル
 * 
 * @author masuda, Masuda Naika
 */
@Entity
@Table(name = "msd_indication")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class IndicationModel implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    
    @Column(nullable = false)
    private String fid;         // 医療機関ID

    @Column(nullable = false, unique = true)
    private String srycd;
    
    private Boolean outPatient; // 外来で有効
    
    private Boolean admission;  // 入院で有効
    
    private Boolean inclusive;  // 10項目以上なら審査対象外
    
    @JsonManagedReference   // bi-directional references
    @JsonDeserialize(contentAs = IndicationItem.class)
    @BatchSize(size = 20)
    @OneToMany(mappedBy = "indication", cascade = CascadeType.ALL, orphanRemoval = true)
    @XmlElement(name = "items")
    private List<IndicationItem> items;
    
    @XmlTransient
    private Boolean isLock;   // てぬき排他処理

    public IndicationModel() {
        outPatient = true;
        admission = true;
    }
    
    @XmlID
    public String getStringId() {
        return String.valueOf(id);
    }
    
    public void setId(long id) {
        this.id = id;
    }
    public void setFacilityId(String fid) {
        this.fid = fid;
    }
    public void setSrycd(String srycd) {
        this.srycd = srycd;
    }
    public void setOutPatient(boolean outPatient) {
        this.outPatient = outPatient;
    }
    public void setAdmission(boolean admission) {
        this.admission = admission;
    }
    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }
    public void setIndicationItems(List<IndicationItem> items) {
        this.items = items;
    }
    public void setLock(boolean isLock) {
        this.isLock = isLock;
    }
    
    public long getId() {
        return id;
    }
    public String getFacilityId() {
        return fid;
    }
    public String getSrycd() {
        return srycd;
    }
    public Boolean isOutPatient() {
        return outPatient == null ? false : outPatient;
    }
    public Boolean isAdmission() {
        return admission == null ? false : admission;
    }
    public Boolean isInclusive() {
        return inclusive == null ? false : inclusive;
    }
    public List<IndicationItem> getIndicationItems() {
        return items;
    }
    public Boolean isLock() {
        return isLock == null ? false : isLock;
    }
}
