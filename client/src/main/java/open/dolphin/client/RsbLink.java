package open.dolphin.client;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;

/**
 * RS_Baseと連携？リンク方法が公開されていたので… 僕はRS_Base持ってないんで動作確認全くしておらずｗ
 *
 * @author masuda, Masuda Naika
 */
public class RsbLink {

    private static final String showRsbTopSpecialID = "999999999999999";

    // RS_Baseと電子カルテの画面連携
    public void rsbOpenKanjaGamenLink(String ptid) {

        String url = Project.getString(MiscSettingPanel.RSB_URL, MiscSettingPanel.DEFAULT_RSB_URL);
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        if (!url.endsWith("/")) {
            sb.append("/");
        }
        sb.append("2000.cgi?show=");
        sb.append(ptid);
 
        openRsb(sb.toString());
    }

    public void rsbOpenLaboLink(String ptid) {

        String url = Project.getString(MiscSettingPanel.RSB_URL, MiscSettingPanel.DEFAULT_RSB_URL);
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        if (!url.endsWith("/")) {
            sb.append("/");
        }
        sb.append("labo_ini.cgi?");
        sb.append(ptid);
        sb.append("=enlarge");

        openRsb(sb.toString());
    }

    private void openRsb(String url) {

        String rsbBrowserPath = Project.getString(MiscSettingPanel.RSB_BROWSER_PATH, 
                MiscSettingPanel.DEFAULT_RSB_BROWSER_PATH);
        
        try {
            if (rsbBrowserPath.trim().isEmpty()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(new URI(url));
            } else {
                String[] args = new String[]{rsbBrowserPath, url};
                new ProcessBuilder(args).start();
            }
        } catch (URISyntaxException | IOException ex) {
        }
    }

    // RS_Baseと電子カルテの画面自動連携
    public void showRsbTop() throws IOException {

        doAutoLink(showRsbTopSpecialID);
    }

    public void doAutoLink(String ptId) throws IOException {

        String rsnPath = Project.getString(MiscSettingPanel.RSB_RSN_PATH, MiscSettingPanel.DEFAULT_RSB_RSN_PATH);

        if (!rsnPath.endsWith(File.separator)) {
            rsnPath = rsnPath + File.separator;
        }

        Path tmpPath = Paths.get(rsnPath + "ID.da_");
        if (Files.exists(tmpPath)) {
            Files.delete(tmpPath);
        }
        Files.write(tmpPath, Collections.singletonList(ptId), Charset.forName("MS932"), StandardOpenOption.CREATE);

        Path datPath = Paths.get(rsnPath + "ID.dat");
        if (Files.exists(datPath)) {
            Files.delete(datPath);
        }

        Files.move(tmpPath, datPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
