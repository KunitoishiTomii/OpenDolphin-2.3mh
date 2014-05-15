package open.dolphin.mbean;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * JndiUtil
 * 
 * @author masuda, Masuda Naika
 */
public class JndiUtil {

    private static final String warName = "OpenDolphin-server-2.3m_WF8";
    
    public static Object getJndiResource(Class cls) {

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("java:global/");
            sb.append(warName).append("/");
            sb.append(cls.getSimpleName());
            InitialContext ic = new InitialContext();
            Object obj = ic.lookup(sb.toString());
            return obj;
        } catch (NamingException ex) {
            Logger.getLogger(JndiUtil.class.getSimpleName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
