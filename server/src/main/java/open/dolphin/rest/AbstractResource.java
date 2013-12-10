package open.dolphin.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.IRestConstants;
import open.dolphin.common.util.JsonConverter;

/**
 * AbstractResource
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class AbstractResource implements IRestConstants {

    protected static final boolean DEBUG = false;
    
    private static final String CHARSET_UTF8 = "; charset=UTF-8";
    protected static final String MEDIATYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + CHARSET_UTF8;
    protected static final String MEDIATYPE_TEXT_UTF8 = MediaType.TEXT_PLAIN + CHARSET_UTF8;

    protected static final Logger logger = Logger.getLogger(AbstractResource.class.getName());

    @Context
    protected HttpServletRequest servletReq;
    

    protected Date parseDate(String source) {
        try {
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_DF_FORMAT);
            return frmt.parse(source);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    protected void debug(String msg) {
        logger.info(msg);
    }

    protected String getRemoteFacility() {
        return (String) servletReq.getAttribute(IInfoModel.FID);
    }
    
    protected String toJson(Object obj) {
        return getConverter().toJson(obj);
    }
    
    protected String toJson(Object obj, TypeReference typeRef) {
        return getConverter().toJson(obj, typeRef);
    }
    
    protected StreamingOutput getJsonOutStream(final Object obj) {
        StreamingOutput so = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                getConverter().toJson(obj, os);
            }
        };
        return so;
    }
    
    protected StreamingOutput getJsonOutStream(final Object obj, final TypeReference typeRef) {
        StreamingOutput so = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                getConverter().toJson(obj, typeRef, os);
            }
        };
        return so;
    }
    
    protected StreamingOutput getGzipOutStream(final Object obj) {
        StreamingOutput so = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                try (GZIPOutputStream gos = new GZIPOutputStream(os)) {
                    getConverter().toJson(obj, gos);
                }
            }
        };
        return so;
    }
    
    protected StreamingOutput getGzipOutStream(final Object obj, final TypeReference typeRef) {
        StreamingOutput so = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                try (GZIPOutputStream gos = new GZIPOutputStream(os)) {
                    getConverter().toJson(obj, typeRef, gos);
                }
            }
        };
        return so;
    }
    
    protected JsonConverter getConverter() {
        return JsonConverter.getInstance();
    }
}
