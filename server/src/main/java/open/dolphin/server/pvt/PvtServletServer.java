package open.dolphin.server.pvt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * PvtServerThread, server
 *
 * @author masuda, Masuda Naika
 */
public class PvtServletServer {

    private static final Logger logger = Logger.getLogger(PvtServletServer.class.getSimpleName());
    
    private static final int DEFAULT_PORT = 5002;
    private final int port = DEFAULT_PORT;
    
    // ServerSocketのスレッド nio!
    private Thread sThread;
    private PvtServerThread serverThread;
    
    // PVT登録処理
    private Thread pThread;
    private PvtPostThread pvtPostThread;

    private static final PvtServletServer instance;
    
    static {
        instance = new PvtServletServer();
    }
    
    public static PvtServletServer getInstance() {
        return instance;
    }

    private PvtServletServer() {
    }

    public void start() {

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), port);
            String msg = "PVT Server is binded " + address;
            logger.info(msg);

            serverThread = new PvtServerThread(address);
            sThread = new Thread(serverThread, "PVT server socket");
            sThread.start();
            
            pvtPostThread = new PvtPostThread();
            pThread = new Thread(pvtPostThread, "PVT post thread");
            pThread.start();

        } catch (IOException ex) {
            String msg = "IOException while creating the ServerSocket: " + ex.toString();
            logger.warning(msg);
        }
    }

    public void dispose() {

        // ServerThreadを中止させる
        serverThread.stop();

        // ServerSocketのThread破棄する
        sThread.interrupt();
        sThread = null;

        // PvtPostThreadを中止させる
        pThread.interrupt();
        
        logger.info("PVT Server stopped.");
    }

    // PvtClaimIOHanlderから呼ばれる
    public void postPvt(String pvtXml) {
        pvtPostThread.offerPvt(pvtXml);
    }
}
