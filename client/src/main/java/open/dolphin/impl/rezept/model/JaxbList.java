package open.dolphin.impl.rezept.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import open.dolphin.infomodel.IndicationModel;

/**
 *
 * @author masuda, Masuda Naika
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({IndicationModel.class})
public class JaxbList<E> {
    
    @XmlAnyElement(lax = true)
    private List<E> list;
    
    public JaxbList() {    
    }
    
    public JaxbList(List<E> list) {
        this.list = list;
    }
    
    public void setList(List<E> list) {
        this.list = list;
    }
    
    public List<E> getList() {
        return list;
    }
}
