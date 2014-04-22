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
    private static final Logger logger = Logger.getLogger(FileServlet.class.getSimpleName());

    static {
        prop = new Properties();
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
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception on loading fileservlet.properties");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // headerにhashが設定されていたら比較し、異なる場合はファイル名リストを返す
        String hash = req.getHeader("HASH");
        if (hash != null) {
            String currHash = prop.getProperty(CURR_HASH);
            logger.log(Level.INFO, "Current Hash = {0}", currHash);
            if (!hash.equals(currHash)) {
                serveFilePaths(resp);
            }
            return;
        }

        // 指定されたファイルを返す
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // パスが設定されていないならば404
        String rootPath = prop.getProperty(ROOT_PATH);
        if (rootPath == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ファイルがないなら404
        Path path = Paths.get(rootPath, pathInfo);
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

    // ファイル名のリストをレスポンスする
    private void serveFilePaths(HttpServletResponse resp) {

        String rootPath = prop.getProperty(ROOT_PATH);
        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            Path root = Paths.get(rootPath);
            List<Path> filePaths = getFilePaths(root);
            boolean first = true;
            for (Path p : filePaths) {
                if (!first) {
                    writer.append(',');
                } else {
                    first = false;
                }
                writer.append(root.relativize(p).toString().replace("\\", "/"));
            }
            writer.flush();
        } catch (IOException ex) {
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
