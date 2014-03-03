package open.dolphin.impl.rezept;

import java.io.BufferedWriter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import open.dolphin.client.ClientContext;
import open.dolphin.impl.rezept.filter.CheckResult;
import open.dolphin.impl.rezept.model.RE_Model;
import open.dolphin.impl.rezept.model.SJ_Model;
import open.dolphin.impl.rezept.model.SY_Model;

/**
 * レセ点、CSV出力
 * 
 * @author Katou, Hashimoto-clinic
 */
public class RezeCheckCsvMaker {

    private List<RE_Model> reModelList;
    private String drName;
    private RezeptView view;

    public void setReModelList(List<RE_Model> list) {
        reModelList = list;
    }

    public void setDrName(String drName) {
        this.drName = drName;
    }

    public void setParent(RezeptView parent) {
        view = parent;
    }

    public void create() {

        // 出力先を取得
        final String fileName = getFilePath();
        if (fileName == null) {
            return;
        }

        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {

                boolean b = makeCSV(fileName);
                return null;
            }
        };
        worker.execute();
    }

    private String getFilePath() {

        SimpleDateFormat frmt = new SimpleDateFormat("yyyyMMdd");
        StringBuilder sb = new StringBuilder();
        sb.append("RezeCheck_").append(frmt.format(new Date()));
        if (drName != null) {
            sb.append("_").append(drName.replace(" ", "").replace("　", ""));
        }
        sb.append(".csv");
        String fileName = sb.toString();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setDialogTitle("レセ点CSV出力");
        File current = fileChooser.getCurrentDirectory();
        fileChooser.setSelectedFile(new File(current.getPath(), fileName));
        int selected = fileChooser.showSaveDialog(view);

        if (selected == JFileChooser.APPROVE_OPTION) {
            final Path path = fileChooser.getSelectedFile().toPath();
            if (!Files.exists(path) || overwriteConfirmed(path)) {
                return path.toString();
            }
        }
        return null;
    }

    private boolean overwriteConfirmed(Path path) {
        String title = "上書き確認";
        String message = "既存のファイル " + path.getFileName().toString() + "\n"
                + "を上書きしようとしています。続けますか？";

        int confirm = JOptionPane.showConfirmDialog(
                view, message, title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

        return confirm == JOptionPane.OK_OPTION;
    }

    private boolean makeCSV(String filePath) throws IOException {

        boolean result = false;
        BufferedWriter bw;
        bw = null;

        try {
            // Open Document
            Path path = Paths.get(filePath);
	    bw = new BufferedWriter(new FileWriter(new File(path.toString())));

            // タイトルはつけない。点検結果のみの出力とする（後で変えるかも）
            //StringBuilder sb = new StringBuilder();
            // sb.append("レセ点,");
            // if (drName != null) {
                // sb.append("  担当医,").append(drName);
            // }
            // document.add(sb.toString());

            // 実際のレセ点結果を出力する
            createRezeCheckCsv(bw);

            result = true;

        } catch (IOException ex) {
        } finally {
            if (bw != null) {
                bw.close();
            }
        }
        return result;

    }

    // CSVに点検結果を追加する
    private void createRezeCheckCsv(BufferedWriter bw) throws IOException {

        // １行目にタイトル行を挿入
        StringBuilder sbFirst = new StringBuilder();
        sbFirst.append("名前,ID,入外区分,保険区分,指摘レベル,指摘箇所,内容");
        sbFirst.append(System.getProperty("line.separator"));
        bw.write(sbFirst.toString());
        
        for (RE_Model reModel : reModelList) {
            // インフォ
            List<CheckResult> results = reModel.getCheckResults();
            if (results == null) {
                // 書くべきことが無いので次の患者を処理
                // ID・患者名は記載した上で「指摘無し」と書いた方が使いやすい？
                continue;
            }
    
            for (CheckResult result : results) {
                // 患者
		// CSVのため、全列に患者ID・患者名を記載する
                StringBuilder sb = new StringBuilder();
                sb.append(reModel.getName()).append(",");
                sb.append(reModel.getPatientId()).append(",");
                switch (reModel.getIrModel().getNyugaikbn()) {
                    case "1":
                        sb.append("入院").append(",");
                        break;
                    case "2":
                        sb.append("外来").append(",");
                        break;
                    default:
                        sb.append("不明").append(",");
                        break;
                }
                switch (reModel.getIrModel().getShinsaKikanNumber()) {
                    case 1:
                        sb.append("社保").append(",");
                        break;
                    case 2:
                        sb.append("国保").append(",");
                        break;
                    case 6:
                        sb.append("後期高齢").append(",");
                        break;
                    default:
                        sb.append("その他").append(",");
                        break;
                }

                // PDFには傷病名があったが、CSV出力は患者IDとNG箇所のみの記載とし、
	        // 詳細は直接アクセスして確認してもらうのを是とする。
	        // 橋本医院は医師がPCの使用に抵抗がないのも一因。

                int status = result.getResult();
                switch (status) {
                    case CheckResult.CHECK_INFO:
                        sb.append("情報").append(",");
                        break;
                    case CheckResult.CHECK_WARNING:
                        sb.append("警告").append(",");
                        break;
                    case CheckResult.CHECK_ERROR:
                        sb.append("エラー").append(",");
                        break;
                    default:
                        break;
                }
		sb.append(result.getFilterName()).append(",");
		sb.append(result.getMsg());
                sb.append(System.getProperty("line.separator"));
		bw.write(sb.toString());
            }
	    // 検査結果出力
        }
        return;
    }
}
