package open.dolphin.common.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Jackson関連
 * @author masuda, Masuda Naika
 */
public class JsonConverter {
    
    private static final String CAMMA = ",";
    private static final ObjectMapper objectMapper;
    private static final JsonConverter instance;
    private static final boolean debug = false;
    
    static {
        instance = new JsonConverter();
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        if (debug) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }
    
    private JsonConverter(){
    }
    
    public static JsonConverter getInstance() {
        return instance;
    }
    
    
    // Object to JSON
    public String toJson(Object obj) {
        try {
            String json =objectMapper.writeValueAsString(obj);
            debug(json);
            return json;
        } catch (JsonProcessingException ex) {
            processException(ex);
        }
        return null;
    }

    public void toJson(Object obj, OutputStream os) {
        try {
            objectMapper.writeValue(os, obj);
        } catch (IOException ex) {
            processException(ex);
        }
    }
    
    public String toJson(Object obj, TypeReference typeRef) {
        try {
            String json =objectMapper.writerWithType(typeRef).writeValueAsString(obj);
            debug(json);
            return json;
        } catch (JsonProcessingException ex) {
            processException(ex);
        }
        return null;
    }

    public void toJson(Object obj, TypeReference typeRef, OutputStream os) {
        try {
            objectMapper.writerWithType(typeRef).writeValue(os, obj);
        } catch (IOException ex) {
            processException(ex);
        }
    }
    
    
    // JSON to Object
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            debug(json);
            return objectMapper.readValue(json, clazz);
        } catch (IOException ex) {
            processException(ex);
        }
        
        return null;
    }
    
    public <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            debug(json);
            return objectMapper.readValue(json, typeRef);
        } catch (IOException ex) {
            processException(ex);
        }
        return null;
    }
    
    // JSON InputStream to Object
    public <T> T fromJson(InputStream is, Class<T> clazz) {
        try {
            T obj = objectMapper.readValue(is, clazz);
            return obj;
        } catch (IOException ex) {
            processException(ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }
    
    public <T> T fromJson(InputStream is, TypeReference<T> typeRef) {
        try {
            T obj = objectMapper.readValue(is, typeRef);
            return obj;
        } catch (IOException ex) {
            processException(ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }
    
    // GZipped JSON to Object
    public <T> T fromGzippedJson(byte[] bytes, Class<T> clazz) {
        
        InputStream is = new ByteArrayInputStream(bytes);
        return fromGzippedJson(is, clazz);
    }
    
    public <T> T fromGzippedJson(byte[] bytes, TypeReference<T> typeRef) {
        
        InputStream is = new ByteArrayInputStream(bytes);
        return fromGzippedJson(is, typeRef);
    }
    
    // GZipped JSON InputStream to Object
    public <T> T fromGzippedJson(InputStream is, Class<T> clazz) {

        try (GZIPInputStream gis = new GZIPInputStream(is)) {
            T obj = fromJson(gis, clazz);
            return obj;
        } catch (IOException ex) {
            processException(ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }

    public <T> T fromGzippedJson(InputStream is, TypeReference<T> typeRef) {

        try (GZIPInputStream gis = new GZIPInputStream(is)) {
            T obj = fromJson(gis, typeRef);
            return obj;
        } catch (IOException ex) {
            processException(ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }

    private void processException(Exception ex) {
        ex.printStackTrace(System.err);
    }
    
    private void debug(String msg) {
        if (debug) {
            System.out.print(msg);
        }
    }
    
    public List<Long> toLongList(String params) {
        String[] strArray  = params.split(CAMMA);
        List<Long> ret = new ArrayList<>();
        for (String s : strArray) {
            ret.add(Long.parseLong(s));
        }
        return ret;
    }
    
    public List<String> toStrList(String params) {
        String[] strArray  = params.split(CAMMA);
        return Arrays.asList(strArray);
    }
    
    public String fromList(Collection list) {
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Iterator itr = list.iterator(); itr.hasNext();) {
            if (!first) {
                sb.append(CAMMA);
            } else {
                first = false;
            }
            sb.append(String.valueOf(itr.next()));
        }
        return sb.toString();
    }
}
