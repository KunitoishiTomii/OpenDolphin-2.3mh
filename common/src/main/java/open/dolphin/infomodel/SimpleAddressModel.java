package open.dolphin.infomodel;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * SimpleAddressModel
 *
 * @author kazm
 *
 */
@Embeddable
public class SimpleAddressModel implements Serializable {
    
    private String zipCode;
    
    private String address;
    
    
    public SimpleAddressModel() {
    }
    
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
}
