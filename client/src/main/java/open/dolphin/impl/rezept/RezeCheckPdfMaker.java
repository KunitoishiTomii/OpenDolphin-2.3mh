package open.dolphin.impl.rezept;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Desktop;
import java.io.File;
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
 * レセ点、印刷
 * 
 * @author masuda, Masuda Naika
 */
public class RezeCheckPdfMaker {

    private static final int TOP_MARGIN = 25;
    private static final int LEFT_MARGIN = 20;
    private static final int BOTTOM_MARGIN = 30;
    private static final int RIGHT_MARGIN = 20;
    private static final int BODY_FONT_SIZE = 10;
    private static final int TITLE_FONT_SIZE = 12;
    private static final String USER_MINCHO_FONT = "msmincho.ttc,1";    // MS-PMicho
    private static final String HEISEI_MIN_W3 = "HeiseiMin-W3";
    private static final String UNIJIS_UCS2_HW_H = "UniJIS-UCS2-HW-H";
    private static final String DOC_FOOTER = "OpenDolphin, Japanese open source EHR. (c)Life Sciences Computing Corp.";

    private final int marginLeft = LEFT_MARGIN;
    private final int marginRight = RIGHT_MARGIN;
    private final int marginTop = TOP_MARGIN;
    private final int marginBottom = BOTTOM_MARGIN;

    private BaseFont baseFont;
    private Font titleFont;
    private Font bodyFont;
    private final int titleFontSize = TITLE_FONT_SIZE;
    private final int bodyFontSize = BODY_FONT_SIZE;

    private List<RE_Model> reModelList;
    private String drName;
    private PdfWriter pdfWriter;
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

                boolean b = makePDF(fileName);
                if (b) {
                    openDocumentFile(fileName);
                }
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
        sb.append(".pdf");
        String fileName = sb.toString();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setDialogTitle("レセ点PDF出力");
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

    private boolean makePDF(String filePath) {

        boolean result = false;

        // 用紙サイズを設定
        Document document = new Document(PageSize.A4, marginLeft, marginRight, marginTop, marginBottom);

        try {
            // Open Document
            Path path = Paths.get(filePath);
            pdfWriter = PdfWriter.getInstance(document, Files.newOutputStream(path));
            document.open();

            // Font
            baseFont = getMinchoFont();
            titleFont = new Font(baseFont, titleFontSize);
            bodyFont = new Font(baseFont, bodyFontSize);

            // フッターに宣伝 do not remove!
            document.setFooter(getDolphinFooter());
            document.open();

            // タイトル
            StringBuilder sb = new StringBuilder();
            sb.append("レセ点");
            if (drName != null) {
                sb.append("  担当医：").append(drName);
            }
            Paragraph title = new Paragraph(sb.toString(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(2);
            document.add(title);

            // コンテンツ
            addCheckTable(document);

            document.close();

            result = true;

        } catch (IOException | DocumentException ex) {
        } finally {
            if (document.isOpen()) {
                document.close();
            }
            if (pdfWriter != null) {
                pdfWriter.close();
            }
        }
        return result;

    }

    // PDFビューアーで開く
    private void openDocumentFile(String filePath) throws IOException {

        File file = new File(filePath);
        if (file.exists()) {
            Desktop.getDesktop().open(file);
        }
    }

    private void addCheckTable(Document document) throws DocumentException {

        for (RE_Model reModel : reModelList) {

            PdfPTable table = createCheckTable();

            // 患者
            StringBuilder sb = new StringBuilder();
            sb.append(reModel.getName()).append("  ");
            sb.append(reModel.getAge()).append("歳  ");
            sb.append(reModel.getPatientId()).append("  ");
            sb.append(RezeUtil.getInstance().getYMStr(reModel.getBillDate())).append("  ");
            sb.append(reModel.getIrModel().getShinsaKikanStr());
            PdfPCell cell1 = new PdfPCell(new Paragraph(sb.toString(), bodyFont));
            cell1.setColspan(2);
            table.addCell(cell1);

            // 傷病名
            table.addCell(createDiagTable(reModel));

            // インフォ
            table.addCell(createInfoTable(reModel));

            // 症状詳記
            if (reModel.getSJModelList() != null && !reModel.getSJModelList().isEmpty()) {
                StringBuilder sb1 = new StringBuilder();
                for (SJ_Model sjModel : reModel.getSJModelList()) {
                    sb1.append("<").append(sjModel.getKbn()).append(">\n");
                    sb1.append(sjModel.getData()).append("\n");
                }
                PdfPCell cell2 = new PdfPCell(new Paragraph(sb1.toString(), bodyFont));
                cell2.setColspan(2);
                table.addCell(cell2);
            }

            document.add(table);
        }
    }

    private PdfPTable createCheckTable() throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        int widths[] = {4, 6};
        table.setWidths(widths);
        table.setSplitLate(false);
        table.setSpacingAfter(2);
        return table;
    }

    private PdfPTable createDiagTable(RE_Model reModel) throws DocumentException {

        PdfPTable table = new PdfPTable(4);
        int widths[] = {10, 5, 3, 5};
        table.setWidths(widths);
        table.setSplitLate(false);

        for (SY_Model syModel : reModel.getSYModelList()) {
            table.addCell(createTableCell(syModel.getDiagName()));
            table.addCell(createTableCell(syModel.getStartDateStr()));
            table.addCell(createTableCell(syModel.getOutcomeStr()));
            table.addCell(createTableCell(syModel.getByoKanrenKbnStr()));
        }

        return table;
    }

    private PdfPCell createTableCell(String str) {
        PdfPCell cell = new PdfPCell(new Paragraph(str, bodyFont));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPTable createInfoTable(RE_Model reModel) throws DocumentException {

        PdfPTable table = new PdfPTable(3);
        int widths[] = {1, 3, 15};
        table.setWidths(widths);
        table.setSplitLate(false);

        for (CheckResult result : reModel.getCheckResults()) {
            int status = result.getResult();
            switch (status) {
                case CheckResult.CHECK_INFO:
                    table.addCell(createTableCell("ｉ"));
                    break;
                case CheckResult.CHECK_WARNING:
                    table.addCell(createTableCell("△"));
                    break;
                case CheckResult.CHECK_ERROR:
                    table.addCell(createTableCell("×"));
                    break;
                default:
                    table.addCell(new PdfPCell());
                    break;
            }
            table.addCell(createTableCell(result.getFilterName()));
            table.addCell(createTableCell(result.getMsg()));
        }

        return table;
    }

    private BaseFont getMinchoFont() throws DocumentException, IOException {

        boolean win = ClientContext.isWin();

        if (win) {
            // Windowsの場合はMS-PMinchoを使う。埋め込んじゃう
            String fontName = getWindowsFontPath(USER_MINCHO_FONT);
            return BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } else {
            return BaseFont.createFont(HEISEI_MIN_W3, UNIJIS_UCS2_HW_H, false);
        }
    }

    private String getWindowsFontPath(String fontName) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.getenv("windir")).append(File.separator);
        sb.append("Fonts").append(File.separator);
        sb.append(fontName);
        return sb.toString();
    }

    private HeaderFooter getDolphinFooter() {
        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC);
        HeaderFooter footer = new HeaderFooter(new Paragraph(DOC_FOOTER, footerFont), false);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setBorder(Rectangle.NO_BORDER);
        return footer;
    }
}
