package open.dolphin.common.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Kazushi Minagawa.
 * @author modified by masuda, Masuda Naika
 */
public class BeanUtils {
    
    public static String beanToXml(Object bean)  {
        
        ByteArrayOutputStream os = new ByteArrayOutputStream(16384);
        try (XMLEncoder e = new XMLEncoder(os)) {
            e.writeObject(bean);
        }
        return os.toString();
    }
    
    public static Object xmlToBean(String beanXml) {
        
        byte[] bytes = beanXml.getBytes(StandardCharsets.UTF_8);
        return xmlDecode(bytes);
    }
    
    public static byte[] xmlEncode(Object bean)  {
        
        ByteArrayOutputStream os = new ByteArrayOutputStream(16384);
        try (XMLEncoder e = new XMLEncoder(os)) {
            e.writeObject(bean);
        }
        return os.toByteArray();
    }
    
    public static Object xmlDecode(byte[] bytes) {
        
        Object obj;
        InputStream is = new ByteArrayInputStream(bytes);
        try (XMLDecoder d = new XMLDecoder(is)) {
            obj = d.readObject();
        }
        return obj;
    }
    
    public static <T> T xmlDecode(byte[] bytes, Class<T> clazz) {
        return (T) xmlDecode(bytes);
    }
    
    public static <T> T deepCopy(T src) {
        byte[] bytes = xmlEncode(src);
        return (T) xmlDecode(bytes);
    }
    

//    public static Object deepCopySharedByteBuffer(Object src) {
//        
//        ByteBufferOutputStream os = new ByteBufferOutputStream();
//        XMLEncoder e = new XMLEncoder(os);
//        e.writeObject(src);
//        e.close();
//        
//        ByteBuffer buf = os.getBuffer();
//        buf.flip();
//        
//        ByteBufferInputStream is = new ByteBufferInputStream(buf);
//        XMLDecoder d = new XMLDecoder(is);
//        Object ret = d.readObject();
//        return ret;
//    }
//
////masuda^   http://forums.sun.com/thread.jspa?threadID=427879
//
//    public static byte[] xmlEncode(Object bean)  {
//        ByteArrayOutputStream bo = new ByteArrayOutputStream();
//        XMLEncoder e = new XMLEncoder(new BufferedOutputStream(bo));
//        
////masuda^   java.sql.Dateとjava.sql.TimestampがxmlEncodeで失敗する
//        DatePersistenceDelegate dpd = new DatePersistenceDelegate();
//        e.setPersistenceDelegate(java.sql.Date.class, dpd);
//        TimestampPersistenceDelegate tpd = new TimestampPersistenceDelegate();
//        e.setPersistenceDelegate(java.sql.Timestamp.class, tpd);
////masuda$
//
//        e.writeObject(bean);
//        e.close();
//        return bo.toByteArray();
//    }
//
//   private static class DatePersistenceDelegate extends PersistenceDelegate {
//
//       @Override
//       protected Expression instantiate(Object oldInstance, Encoder out) {
//           java.sql.Date date = (java.sql.Date) oldInstance;
//           long time = Long.valueOf(date.getTime());
//           return new Expression(date, date.getClass(), "new", new Object[]{time});
//       }
//   }
//
//   private static class TimestampPersistenceDelegate extends PersistenceDelegate {
//
//       @Override
//       protected Expression instantiate(Object oldInstance, Encoder out) {
//           java.sql.Timestamp date = (java.sql.Timestamp) oldInstance;
//           long time = Long.valueOf(date.getTime());
//           return new Expression(date, date.getClass(), "new", new Object[]{time});
//       }
//   }
////masuda$
}
