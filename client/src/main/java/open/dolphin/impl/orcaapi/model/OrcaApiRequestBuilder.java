package open.dolphin.impl.orcaapi.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import open.dolphin.impl.orcaapi.IOrcaApi;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

/**
 * OrcaApiRequestBuilder
 * 
 * @author masuda, Masuda Naika
 */
public class OrcaApiRequestBuilder implements IOrcaApi {
    
    public static String createMedicalModModelXml(MedicalModModel model) {
        
        Element elem = new OrcaApiElement.MedicalMod(model);
        return elememtToXml(elem);
    }
    
    public static String createMedicalModModelXml2(MedicalModModel model) {
        
        Element elem = new OrcaApiElement2.MedicalMod(model);
        return elememtToXml(elem);
    }
    
    public static String createMedicalgetreqXml2(String patientId, String ymd, String deptCode) {

        Element elem = new Element(DATA)
                .addContent(new Element("medicalgetreq").setAttribute(TYPE, RECORD)
                        .addContent(new Element("Patient_ID").setAttribute(TYPE, STRING).addContent(patientId))
                        .addContent(new Element("Perform_Date").setAttribute(TYPE, STRING).addContent(ymd))
                        .addContent(createMedicalInfoElem2(deptCode))
                );

       return elememtToXml(elem);
    }

    private static Element createMedicalInfoElem2(String deptCode) {

        Element elem = new Element("Medical_Information").setAttribute(TYPE, RECORD)
                .addContent(new Element("Department_Code").setAttribute(TYPE, STRING).addContent(deptCode))
                .addContent(new Element("Sequential_Number").setAttribute(TYPE, STRING))
                .addContent(new Element("Insurance_Combination_Number").setAttribute(TYPE, STRING))
                .addContent(new Element("HealthInsurance_Information").setAttribute(TYPE, RECORD)
                        .addContent(new Element("InsuranceProvider_Class").setAttribute(TYPE, STRING))
                        .addContent(new Element("InsuranceProvider_WholeName").setAttribute(TYPE, STRING))
                        .addContent(new Element("InsuranceProvider_Number").setAttribute(TYPE, STRING))
                        .addContent(new Element("HealthInsuredPerson_Symbol").setAttribute(TYPE, STRING))
                        .addContent(new Element("HealthInsuredPerson_Number").setAttribute(TYPE, STRING))
                );

        return elem;
    }
    
    public static String createSystem01ManagereqXml() {
        
        final SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd");
        String ymd = frmt.format(new Date());
        
        Element elem = new Element(DATA)
                .addContent(new Element(RECORD)
                        .addContent(new Element(RECORD).setAttribute(NAME, "system01_managereq")
                                .addContent(new Element(STRING).setAttribute(NAME, "Base_Date")
                                        .addContent(ymd))));
        
        return elememtToXml(elem);
    }
    
    public static String createSystem01ManagereqXml2() {
        
        final SimpleDateFormat frmt = new SimpleDateFormat("yyyy-MM-dd");
        String ymd = frmt.format(new Date());
        
        Element elem = new Element(DATA)
                .addContent(new Element("system01_managereq").setAttribute(TYPE, RECORD)
                        .addContent(new Element(BASE_DATE).setAttribute(TYPE, STRING)
                                .addContent(ymd)));
        
        return elememtToXml(elem);
    }
    
    private static String elememtToXml(Element elem) {
        XMLOutputter outputter = new XMLOutputter();
        Document doc = new Document(elem);
        return outputter.outputString(doc);
    }
}
