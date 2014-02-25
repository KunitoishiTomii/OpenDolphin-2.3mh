package open.dolphin.impl.orcaapi.parser;

import java.io.IOException;
import org.jdom2.Document;
import org.jdom2.JDOMException;


/**
 * MedicalreqResParser
 * 
 * @author masuda, Masuda Naika
 */
public class MedicalreqResParser extends AbstractOrcaApiParser {
    
    public MedicalreqResParser(String xml) throws JDOMException, IOException {
        super(xml);
    }
    
    public MedicalreqResParser(Document doc) {
        super(doc);
    }
    
    public String getMedicalUid() {
        final String name = "Medical_Uid";
        return xml2 
                ? getElementText2(name) 
                : getElementText(name);
    }
}
