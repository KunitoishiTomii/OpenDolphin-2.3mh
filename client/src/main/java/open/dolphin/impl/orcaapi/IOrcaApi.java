package open.dolphin.impl.orcaapi;

import javax.ws.rs.core.MediaType;

/**
 * ORCA APIで使う定数群
 * 
 * @author masuda, Masuda Naika
 */
public interface IOrcaApi {
    
    public static final String UTF8 = "UTF-8";
    public static final MediaType MEDIATYPE_XML_UTF8
            = MediaType.APPLICATION_JSON_TYPE.withCharset(UTF8);
    public static final int API_PORT = 8000;
    
    public static final String API_RESULT = "Api_Result";
    public static final String API_RESULT_MESSAGE = "Api_Result_Message";
    public static final String API_NO_ERROR = "00";
    
    public static final String DATA = "data";
    public static final String RECORD = "record";
    public static final String ARRAY = "array";
    public static final String NAME = "name";
    public static final String STRING = "string";
    public static final String CLASS = "class";
    public static final String TYPE = "type";
    public static final String _CHILD = "_child";
    
    public static final String BASE_DATE = "Base_Date";
    
    public static final String MEDICAL_REQ = "medicalreq";
    public static final String PATIENT_ID = "Patient_ID";
    public static final String PERFORM_DATE = "Perform_Date";
    public static final String PERFORM_TIME = "Perform_Time";
    public static final String MEDICAL_UID = "Medical_Uid";
    
    public static final String MEDICA_GET_REQ = "medicalgetreq";
    public static final String SEQUENTIAL_NUMBER = "Sequential_Number";
    public static final String INSURANCE_COMBINATION_NUMBER = "Insurance_Combination_Number";
    public static final String DEPARTMENT_CODE = "Department_Code";
    public static final String PERFORM_CALENDAR = "Perform_Calendar";
    public static final String PATIENT_INFORMATION = "Patient_Information";
    public static final String WHOLE_NAME = "WholeName";
    
    public static final String DIAGNOSIS_INFORMATION = "Diagnosis_Information";
    public static final String PHYSICIAN_CODE = "Physician_Code";
    
    public static final String HEALTH_INSURANCE_INFORMATION = "HealthInsurance_Information";
    public static final String INSURANCE_PROVIDER_CLASS = "InsuranceProvider_Class";
    public static final String INSURANCE_PROVIDER_NUMBER = "InsuranceProvider_Number";
    public static final String INSURANCE_PROVIDER_WHOLENAME = "InsuranceProvider_WholeName";
    public static final String HEALTH_INSURED_PERSON_SYMBOL = "HealthInsuredPerson_Symbol";
    public static final String HEALTH_INSURED_PERSON_NUMBER = "HealthInsuredPerson_Number";
    public static final String HEALTH_INSURED_PERSON_CONTINUATION = "HealthInsuredPerson_Continuation";
    public static final String HEALTH_INSURED_PERSON_ASSISTANCE = "HealthInsuredPerson_Assistance";
    public static final String RELATION_TO_INSURED_PERSON = "RelationToInsuredPerson";
    public static final String HEALTH_INSURED_PERSON_WHOLE_NAME = "HealthInsuredPerson_WholeName";
    public static final String CERTIFICATE_START_DATE = "Certificate_StartDate";
    public static final String CERTIFICATE_ISSURED_DATE = "Certificate_IssuedDate";
    public static final String CERTIFICATE_EXPIRED_DATE = "Certificate_ExpiredDate";
    public static final String PUBLIC_INSURANCE_INFORMATION = "PublicInsurance_Information";
    public static final String PUBLIC_INSURANCE_INFORMATION_CHILD = PUBLIC_INSURANCE_INFORMATION + _CHILD;
    public static final String PUBLIC_INSURANCE_CLASS = "PublicInsurance_Class";
    public static final String PUBLIC_INSURANCE_NAME = "PublicInsurance_Name";
    public static final String PUBLIC_INSURER_NUMBER = "PublicInsurer_Number";
    public static final String PUBLIC_INSURED_PERSON_NUMBER = "PublicInsuredPerson_Number";
    
    public static final String MEDICAL_INFORMATION = "Medical_Information";
    public static final String MEDICAL_INFORMATION_CHILD = MEDICAL_INFORMATION + _CHILD;
    public static final String MEDICAL_LIST_INFORMATION = "Medical_List_Information";
    public static final String MEDICAL_LIST_INFORMATION_CHILD = MEDICAL_LIST_INFORMATION + _CHILD;
    public static final String MEDICAL_CLASS = "Medical_Class";
    public static final String MEDICAL_CLASS_NAME = "Medical_Class_Name";
    public static final String MEDICAL_CLASS_NUMBER = "Medical_Class_Number";
    public static final String MEDICATION_INFO = "Medication_Info";
    public static final String MEDICATION_INFO_CHILD = MEDICATION_INFO + _CHILD;
    public static final String MEDICATION_CODE = "Medication_Code";
    public static final String MEDICATION_NAME = "Medication_Name";
    public static final String MEDICATION_NUMBER = "Medication_Number";
    
    public static final String DISEASE_INFORMATION = "Disease_Information";
    public static final String DISEASE_INFORMATION_CHILD = DISEASE_INFORMATION + _CHILD;
    public static final String DISEASE_CODE = "Disease_Code";
    public static final String DISEASE_NAME = "Disease_Name";
    public static final String DISEASE_SUSPECTED_FLAG = "Disease_SuspectedFlag";
    public static final String DISEASE_START_DATE = "Disease_StartDate";
    public static final String DISEASE_END_DATE = "Disease_EndDate";
    public static final String DISEASE_OUTCOME = "Disease_OutCome";
    public static final String DISEASE_IN_OUT = "Disease_InOut";
    public static final String DISEASE_SINGLE = "Disease_Single";
    public static final String DISEASE_SINGLE_CHILD = DISEASE_SINGLE + _CHILD;
    public static final String DISEASE_SINGLE_CODE = "Disease_Single_Code";
    public static final String DISEASE_SINGLE_NAME = "Disease_Single_Name";
    
    public static final String OUTCOME_DIED = "D";
    public static final String OUTCOME_FULLY_RECOVERED = "F";
    public static final String OUTCOME_NOT_RECOVERING = "N";
    public static final String OUTCOME_RECOVERING = "R";
    public static final String OUTCOME_SEQUELAE = "S";
    public static final String OUTCOME_UNKNOWN = "U";
    public static final String OUTCOME_WORSENING = "W";
    public static final String OUTCOME_OMIT = "O";
    
    public static final String PRIMARY_DISEASE = "PD";
    public static final String SUSPECTED_FLAG = "S";
    
}
