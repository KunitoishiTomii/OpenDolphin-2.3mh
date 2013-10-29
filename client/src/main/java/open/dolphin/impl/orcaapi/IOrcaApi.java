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
    
    public static final String BASE_DATE = "Base_Date";
}
