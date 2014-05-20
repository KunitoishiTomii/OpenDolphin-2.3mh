package open.dolphin.server.pvt;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import open.dolphin.infomodel.PatientVisitModel;
import open.dolphin.mbean.JndiUtil;
import open.dolphin.pvtclaim.PVTBuilder;
import open.dolphin.session.MasudaServiceBean;
import open.dolphin.session.PVTServiceBean;

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

    // PvtPostTask
    private ExecutorService exec;

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

            exec = Executors.newSingleThreadExecutor();

        } catch (IOException ex) {
            String msg = "IOException while creating the ServerSocket: " + ex.toString();
            logger.warning(msg);
        }
    }

    public void dispose() {

        // ServerThreadを中止させる
        if (serverThread != null) {
            serverThread.stop();
            // ServerSocketのThread破棄する
            sThread.interrupt();
        }

        // shutdown PvtPostTask
        if (exec != null) {
            exec.shutdown();
            try {
                if (!exec.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    exec.shutdownNow();
                }
            } catch (InterruptedException ex) {
                exec.shutdownNow();
            }
        }

        logger.info("PVT Server stopped.");
    }

    // PvtClaimIOHanlderから呼ばれる
    public void postPvt(String pvtXml) {
    }
}
