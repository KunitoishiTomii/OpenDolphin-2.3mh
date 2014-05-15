package open.dolphin.mbean;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import open.dolphin.server.orca.OrcaService;
import open.dolphin.server.pvt.PvtServletServer;
import open.dolphin.session.MasudaServiceBean;

/**
 * DolphinServletListener
 * ゴニョゴニョするときのJava EE標準の定番初期化ポイントらしい
 * @author masuda, Masuda Naika
 */
@WebListener
public class DolphinServletListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        
        MasudaServiceBean masudaService = (MasudaServiceBean) JndiUtil.getJndiResource(MasudaServiceBean.class);
        if (masudaService != null && masudaService.usePvtServletServer()) {
            PvtServletServer.getInstance().start();
        }

        OrcaService.getInstance().start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        MasudaServiceBean masudaService = (MasudaServiceBean) JndiUtil.getJndiResource(MasudaServiceBean.class);
        if (masudaService != null && masudaService.usePvtServletServer()) {
            PvtServletServer.getInstance().dispose();
        }

        OrcaService.getInstance().dispose();
    }

}
