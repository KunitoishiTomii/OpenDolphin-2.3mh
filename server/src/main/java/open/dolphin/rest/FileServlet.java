package open.dolphin.rest;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * FileServlet
 *
 * @author masuda, Masuda Naika
 */
@WebServlet(urlPatterns = {"/files/*"}, asyncSupported = true)
public class FileServlet extends HttpServlet {

    private static final String ALGORITHM = "MD5";
    private static final String JAR_NAME = "jar.name";
    private static final String ROOT_PATH = "root.path";
    private static final String CURR_HASH = "curr.hash";
    private static final Properties prop;
    private static final List<String> fileNames;
    private static final Logger logger = Logger.getLogger(FileServlet.class.getSimpleName());

    static {
        prop = new Properties();
        fileNames = new ArrayList<>();
    }

    @Override
    public void init() throws ServletException {

        final String jbossConfigDir = System.getProperty("jboss.server.config.dir");

        try {
            Path propPath = Paths.get(jbossConfigDir, "fileservlet.properties");
            if (Files.exists(propPath)) {
                URL config = propPath.toUri().toURL();
                prop.load(config.openStream());
            }
            // プログラムのハッシュ値を求める
            String rootPath = prop.getProperty(ROOT_PATH);
            String jarName = prop.getProperty(JAR_NAME);
            if (rootPath != null && jarName != null) {
                Path path = Paths.get(rootPath, jarName);
                if (Files.exists(path)) {
                    String currHash = getFileHash(path, ALGORITHM);
                    prop.setProperty(CURR_HASH, currHash);
                    logger.log(Level.INFO, "{0} hash = {1}", new Object[]{jarName, currHash});
                }
            }
            // ファイル名リストを設定する
            Path root = Paths.get(rootPath);
            List<Path> filePaths = getFilePaths(root);
            for (Path path : filePaths) {
                fileNames.add(root.relativize(path).toString().replace("\\", "/"));
            }

        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception on loading fileservlet.properties");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // パスが設定されていないならば404
        String rootPath = prop.getProperty(ROOT_PATH);
        String pathInfo = req.getPathInfo();
        if (rootPath == null || pathInfo == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // index.html
        if ("/index.html".equals(pathInfo)) {
            serveIndexHtml(resp);
            return;
        }
        
        // launcher.properties
        if ("/launcher.properties".equals(pathInfo)) {
            serveProperties(req, resp);
            return;
        }

        // 指定されたファイルを返す
        Path path = Paths.get(rootPath, pathInfo);
        serveFile(path, req, resp);
    }
    
    // launcher.propertiesを作成して返す
    private void serveProperties(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
        String serverIp = req.getLocalAddr();
        String jarName = prop.getProperty(JAR_NAME);
        StringBuilder sb = new StringBuilder();
        sb.append("jar.name=").append(jarName).append("\n");
        sb.append("server.ip=").append(serverIp).append("\n");
        
        resp.setContentType("text/x-java-properties");
        try (PrintWriter writer = resp.getWriter()) {
            writer.write(sb.toString());
        }
    }

    // 指定されたファイルを返す
    private void serveFile(Path path, HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // ファイルがないなら404
        if (Files.isDirectory(path) || !Files.exists(path)) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // クライアントに送信する
        logger.log(Level.INFO, "Send {0} to {1}", new Object[]{path.getFileName(), req.getRemoteAddr()});
        resp.setContentType("application/java-archive");
        try (FileChannel fileCh = new FileInputStream(path.toFile()).getChannel();
                WritableByteChannel respCh = Channels.newChannel(resp.getOutputStream())) {
            fileCh.transferTo(0, fileCh.size(), respCh);
        }
    }

    // index.htmlページを作成して返す
    private void serveIndexHtml(HttpServletResponse resp) throws IOException {

        String title = prop.getProperty(JAR_NAME).replace(".jar", "");
        String hash = prop.getProperty(CURR_HASH);
        
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<title>").append(title).append("</title>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<h1>").append(title).append("</h1>\n");
        sb.append("<br>\n");
        sb.append("<p>Download DolphinLauncher and launcher.properties to client's folder.</p>\n");
        sb.append("<p>e.g. C:\\User Program Files\\OpenDolphin\\</p>\n");
        sb.append("<br>\n");
        sb.append("<p><a href=\"DolphinLauncher.jar\">DolphinLauncher</a></p>\n");
        sb.append("<p><a href=\"launcher.properties\">設定ファイル</a></p>\n");
        sb.append("<br>\n");
        sb.append("<p>").append("HASH:").append(hash).append("</p>\n");
        for (String name : fileNames) {
            sb.append("<p>").append("FILE:").append(name).append("</p>\n");
        }
        sb.append("</body>\n");
        sb.append("</html>\n");

        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.write(sb.toString());
        }
    }

    // フォルダ内のファイルを列挙する
    private List<Path> getFilePaths(Path root) {

        List<Path> filePaths = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(root)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    filePaths.addAll(getFilePaths(path));
                } else {
                    filePaths.add(path);
                }
            }
        } catch (IOException ex) {
        }
        return filePaths;
    }

    // byte[]を２桁のHex文字列に変換
    private String bytesToHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toHexString((b & 0xF0) >> 4));
            sb.append(Integer.toHexString(b & 0xF));

        }
        return sb.toString();
    }

    // ファイルのHashを計算する
    private String getFileHash(Path path, String algorithm) throws Exception {

        MessageDigest md = MessageDigest.getInstance(algorithm);
        try (FileInputStream fis = new FileInputStream(path.toFile());
                BufferedInputStream bis = new BufferedInputStream(fis);
                DigestInputStream dis = new DigestInputStream(bis, md)) {
            while (dis.read() != -1) {
            }
        }

        byte[] digest = md.digest();
        return bytesToHex(digest);
    }
}
