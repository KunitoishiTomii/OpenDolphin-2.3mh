package open.dolphin.letter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.StyleConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.client.ClientContext;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.common.util.StampRenderingHints;
import open.dolphin.common.util.XmlUtils;
import open.dolphin.util.AgeCalculator;


/**
 * PDFKarteMaker.java
 * カルテをPDFにエクスポートする。KarteRenderer2から主なコード拝借
 *
 * @author masuda, Masuda Naika
 */
public class KartePDFMaker extends AbstractPDFMaker {

    private static final String STAMP_HOLDER = "stampHolder";
    private static final String SCHEMA_HOLDER = "schemaHolder";
    private static final String COMPONENT_NAME = StyleConstants.ComponentElementName;   // "component";
    //private static final String SECTION_NAME = AbstractDocument.SectionElementName;
    private static final String CONTENT_NAME = AbstractDocument.ContentElementName;
    private static final String PARAGRAPH_NAME = AbstractDocument.ParagraphElementName;
    private static final String TEXT_NAME ="text";
    
    private static final String NAME_NAME = StyleConstants.NameAttribute.toString();
    private static final String ALIGNMENT_NAME = StyleConstants.Alignment.toString();   // "Alignment" not "alignment"
    private static final String FOREGROUND_NAME = StyleConstants.Foreground.toString();
    private static final String SIZE_NAME = StyleConstants.Size.toString();
    private static final String BOLD_NAME = StyleConstants.Bold.toString();
    private static final String ITALIC_NAME = StyleConstants.Italic.toString();
    private static final String UNDERLINE_NAME = StyleConstants.Underline.toString();
    private static final String VALUE_ONE = "1";

    private static final int KARTE_FONT_SIZE = 9;
    private static final int STAMP_FONT_SIZE = 8;
    private static final int PERCENTAGE_IMAGE_WIDTH = 25;   // ページ幅の1/4
    private static final Color STAMP_TITLE_BACKGROUND = new Color(200, 200, 200);    // グレー
    private static final String UNDER_TMP_SAVE = " - 仮保存中";
    
    private static final String DOC_TITLE = "カルテ";

    private List<DocumentModel> docList;
    private boolean ascending;
    private int bookmarkNumber;   // しおりの内部番号
    
    
    @Override
    protected final String getPatientName() {
        return (context != null) ? context.getPatient().getFullName() : null;
    }

    @Override
    protected final String getPatientId() {
        return (context != null) ? context.getPatient().getPatientId() : null;
    }
    
    private String getPatientBirthday() {
        return (context != null) ? context.getPatient().getBirthday() : null;
    }
    
    // 文書名を返す
    @Override
    protected String getTitle() {
        return DOC_TITLE.replace(" ", "").replace("　", "");
    }
    
    // PDFに出力する
    @Override
    protected boolean makePDF(String filePath) {

        boolean result = false;
        marginLeft = 20;
        marginRight = 20;
        marginTop = 20;
        marginBottom = 30;
        titleFontSize = 10;        

        // 昇順・降順にソート
        if (ascending) {
            Collections.sort(docList);
        } else {
            Collections.sort(docList, Collections.reverseOrder());
        }

        // 用紙サイズを設定
        Document document = new Document(PageSize.A4, marginLeft, marginRight, marginTop, marginBottom);

        try {
            // Font
            baseFont = getGothicFont();
            Font font = new Font(baseFont, KARTE_FONT_SIZE);

            // PdfWriterの設定
//minagawa^ mac jdk7            
//            writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
            Path path = Paths.get(filePath);
            writer = PdfWriter.getInstance(document, Files.newOutputStream(path));
//minagawa$
            writer.setStrictImageSequence(true);
            writer.setViewerPreferences(PdfWriter.PageModeUseOutlines);

            // フッターに名前とIDを入れる
            SimpleDateFormat sdf = new SimpleDateFormat(FRMT_DATE_WITH_TIME);
            StringBuilder sb = new StringBuilder();
            sb.append(getPatientId()).append(" ");
            sb.append(getPatientName()).append(" 様 ");
            sb.append(sdf.format(new Date()));
            sb.append("  Page ");
            HeaderFooter footer = new HeaderFooter(new Phrase(sb.toString(), font), true);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setBorder(Rectangle.NO_BORDER);
            document.setFooter(footer);

            // 製作者と文書タイトルを設定
            String author = Project.getUserModel().getFacilityModel().getFacilityName();
            document.addAuthor(author);
            document.addTitle(getPatientName() + " 様カルテ");

            document.open();

            for (DocumentModel docModel : docList) {

                // DocumentModelからschema, moduleを取り出す
                List<SchemaModel> schemas = docModel.getSchema();
                List<ModuleModel> modules = docModel.getModules();
                List<ModuleModel> soaModules = new ArrayList<>();
                List<ModuleModel> pModules = new ArrayList<>();
                String soaSpec = null;
                String pSpec = null;

                for (ModuleModel bean : modules) {
                    String role = bean.getModuleInfoBean().getStampRole();
                    if (role == null) {
                        continue;
                    }
                    switch (role) {
                        case IInfoModel.ROLE_SOA:
                            soaModules.add(bean);
                            break;
                        case IInfoModel.ROLE_SOA_SPEC:
                            soaSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                            break;
                        case IInfoModel.ROLE_P:
                            pModules.add(bean);
                            break;
                        case IInfoModel.ROLE_P_SPEC:
                            pSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                            break;
                    }
                }

                // 念のためソート
                Collections.sort(soaModules);
                Collections.sort(pModules);
                if (schemas != null) {
                    Collections.sort(schemas);
                }

                // テーブルを作成する
                KarteTable table;
                DocInfoModel docInfo = docModel.getDocInfoModel();

                if (docInfo != null && 
                        (IInfoModel.DOCTYPE_S_KARTE.equals(docInfo.getDocType())
                        || IInfoModel.DOCTYPE_SUMMARY.equals(docInfo.getDocType()))) {
                    table = createTable(docModel, 1);
                    PdfPCell cell = new PdfCellRenderer(baseFont)
                            .render(soaSpec, soaModules, schemas, table);
                    cell.setColspan(2);
                    table.addCell(cell);
                } else {
                    table = createTable(docModel, 2);
                    PdfPCell cell = new PdfCellRenderer(baseFont)
                            .render(soaSpec, soaModules, schemas, table);
                    table.addCell(cell);
                    cell = new PdfCellRenderer(baseFont)
                            .render(pSpec, pModules, schemas, table);
                    table.addCell(cell);
                }

                // PdfDocumentに追加する
                document.add(table);
            }

            result = true;

        } catch (IOException ex) {
            ClientContext.getBootLogger().warn(ex);
            throw new RuntimeException(ERROR_IO);
        } catch (DocumentException ex) {
            ClientContext.getBootLogger().warn(ex);
            throw new RuntimeException(ERROR_PDF);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
        return result;
    }

    public void setDocumentList(List<DocumentModel> docList) {
        this.docList = docList;
    }

    public void setAscending(boolean b) {
        this.ascending = b;
    }
    
    private KarteTable createTable(DocumentModel model, int col) {

        // タイトルのフォント
        Font font = new Font(baseFont, titleFontSize);
        String title = createTitle(model);
        // しおりのタイトルは日付とDocInfo.tilte
        SimpleDateFormat sdf = new SimpleDateFormat(FRMT_DATE_WITH_TIME);
        StringBuilder sb = new StringBuilder();
        sb.append(sdf.format(model.getDocInfoModel().getFirstConfirmDate()));
        sb.append("\n");
        sb.append(model.getDocInfoModel().getTitle());
        String bookmark = sb.toString();

        // タイトルにしおりを登録する
        String mark = String.valueOf(++bookmarkNumber);
        Chunk chunk = new Chunk(title, font).setLocalDestination(mark);
        PdfOutline root = writer.getDirectContent().getRootOutline();
        new PdfOutline(root, PdfAction.gotoLocalPage(mark, false), bookmark);

        // テーブルを作成する
        PdfPCell titleCell = new PdfPCell(new Paragraph(chunk));
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        KarteTable table = new KarteTable(col);
        titleCell.setColspan(col);

        table.addCell(titleCell);
        table.setWidthPercentage(100);
        table.setSpacingAfter(2);
        // ヘッダー行を指定
        //table.setHeaderRows(1);
        // 改頁で表の分割を許可
        table.setSplitLate(false);

        return table;
    }

    // カルテのタイトルを作成する
    private String createTitle(DocumentModel model) {

        DocInfoModel docInfo = model.getDocInfoModel();
        String status = docInfo.getStatus();
        StringBuilder sb = new StringBuilder();
        
        if (status != null) {
            switch (docInfo.getStatus()) {
                case IInfoModel.STATUS_DELETE:
                    sb.append("削除済／");
                    break;
                case IInfoModel.STATUS_MODIFIED:
                    sb.append("修正:");
                    sb.append(docInfo.getVersionNumber().replace(".0", ""));
                    sb.append("／");
                    break;
            }
        }

        // 確定日を分かりやすい表現に変える
        sb.append(ModelUtils.getDateAsFormatString(
                docInfo.getFirstConfirmDate(), IInfoModel.KARTE_DATE_FORMAT));

        // 当時の年齢を表示する
        String mmlBirthday = getPatientBirthday();
        String mmlDate = ModelUtils.getDateAsString(docInfo.getFirstConfirmDate());
        if (mmlBirthday != null) {
            sb.append("[").append(AgeCalculator.getAge2(mmlBirthday, mmlDate)).append("歳]");
        }

        if (docInfo.getStatus().equals(IInfoModel.STATUS_TMP)) {
            sb.append(UNDER_TMP_SAVE);
        }
        
        // 入院の場合は病室・入院科を表示する
        AdmissionModel admission = docInfo.getAdmissionModel();
        if (admission != null) {
            sb.append("<");
            sb.append(admission.getRoom()).append("号室:");
            sb.append(admission.getDepartment());
            sb.append(">");
        }
        
        // 保険　公費が見えるのは気分良くないだろうから、表示しない
        // コロン区切りの保険者名称・公費のフォーマットである 
        // 旧カルテはSPC区切りの保険者番号・SPC・保険者名称・公費のフォーマット
        String ins = docInfo.getHealthInsuranceDesc().trim();
        if (ins != null && !ins.isEmpty()) {
            if (ins.contains(":")) {
                String items[] = docInfo.getHealthInsuranceDesc().split(":");
                sb.append("／");
                sb.append(items[0]);
            } else if (ins.contains(" ")) {
                String items[] = docInfo.getHealthInsuranceDesc().split(" ");
                if (items.length > 2) {
                    sb.append("／");
                    sb.append(items[2]);
                } else {
                    sb.append("／");
                    sb.append(ins);
                }
            } else {
                sb.append("／");
                sb.append(ins);
            }
        }
        
        // KarteViewerで日付の右Dr名を表示する
        sb.append("／");
        sb.append(model.getUserModel().getCommonName());
        
        // pdfには最終編集日を記載する
        if (docInfo.getParentId() != null) {
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_DF_FORMAT);
            sb.append("／[編集]");
            String cconfirmDate = frmt.format(docInfo.getConfirmDate());
            sb.append(cconfirmDate);
        }

        return sb.toString();
    }
    
    
    private static class KarteTable extends PdfPTable {

        private final int col;    // カラム数

        private KarteTable(int col) {
            super(col);
            this.col = col;
        }

        private int getColumnCount() {
            return col;
        }
    }

    private static class PdfCellRenderer {

        private PdfPCell cell;
        private Paragraph theParagraph;
        private List<ModuleModel> modules;
        private List<SchemaModel> schemas;
        private KarteTable karteTable;
        
        private String foreground;
        private String size;
        private String bold;
        private String italic;
        private String underline;
        
        private final StampRenderingHints hints;
        private final BaseFont baseFont;
        
        private PdfCellRenderer(BaseFont baseFont) {
            hints = StampRenderingHints.getInstance();
            this.baseFont = baseFont;
        }

        private PdfPCell render(String xml, List<ModuleModel> modules, List<SchemaModel> schemas, KarteTable karteTable) {

            this.modules = modules;
            this.schemas = schemas;
            this.karteTable = karteTable;

            // SoaPane, Ppaneが収まるセル
            cell = new PdfPCell();
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = null;

            try (StringReader stream = new StringReader(xml)) {
                
                reader = factory.createXMLStreamReader(stream);

                while (reader.hasNext()) {
                    int eventType = reader.next();
                    switch (eventType) {
                        case XMLStreamReader.START_ELEMENT:
                            startElement(reader);
                            break;
                    }
                }

            } catch (XMLStreamException ex) {
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (XMLStreamException ex) {
                }
            }
            
            return cell;
        }
        
        private void startElement(XMLStreamReader reader) throws XMLStreamException {
            
            String eName = reader.getName().getLocalPart();
            
            if (eName == null) {
                return;
            }

            switch (eName) {
                case PARAGRAPH_NAME:
                    String alignStr = reader.getAttributeValue(null, ALIGNMENT_NAME);
                    startParagraph(alignStr);
                    break;
                case CONTENT_NAME:
                    foreground = reader.getAttributeValue(null, FOREGROUND_NAME);
                    size = reader.getAttributeValue(null, SIZE_NAME);
                    bold = reader.getAttributeValue(null, BOLD_NAME);
                    italic = reader.getAttributeValue(null, ITALIC_NAME);
                    underline = reader.getAttributeValue(null, UNDERLINE_NAME);
                    break;
                case TEXT_NAME:
                    String text = reader.getElementText();
                    startContent(text);
                    break;
                case COMPONENT_NAME:
                    String name = reader.getAttributeValue(null, NAME_NAME);
                    String number = reader.getAttributeValue(null, COMPONENT_NAME);
                    startComponent(name, number);
                    break;
                //case SECTION_NAME:
                //    break;
                default:
                    break;
            }
        }

        private void startParagraph(String alignStr) {

            theParagraph = createNewParagraph();
            cell.addElement(theParagraph);

            if (alignStr != null) {
                switch (alignStr) {
                    case "0":
                        theParagraph.setAlignment(Element.ALIGN_LEFT);
                        break;
                    case "1":
                        theParagraph.setAlignment(Element.ALIGN_CENTER);
                        break;
                    case "2":
                        theParagraph.setAlignment(Element.ALIGN_RIGHT);
                        break;
                }
            }
        }

        private Paragraph createNewParagraph() {
            Paragraph p = new Paragraph();
            p.setFont(new Font(baseFont, KARTE_FONT_SIZE));
            //p.setLeading(KARTE_FONT_SIZE + 2);
            return p;
        }

        private void startContent(String text) {

            // 特殊文字を戻す
            text = XmlUtils.fromXml(text);

            Font font = theParagraph.getFont();
            // foreground 属性を設定する
            if (foreground != null) {
                String[] tokens = foreground.split(",");
                int r = Integer.parseInt(tokens[0]);
                int g = Integer.parseInt(tokens[1]);
                int b = Integer.parseInt(tokens[2]);
                theParagraph.getFont().setColor(new Color(r, g, b));
            }

            // size 属性を設定する
            if (size != null) {
                font.setSize(Float.valueOf(size));
            }

            // bold 属性を設定する
            if (bold != null) {
                font.setStyle(Font.BOLD);
            }

            // italic 属性を設定する
            if (italic != null) {
                font.setStyle(Font.ITALIC);
            }

            // underline 属性を設定する
            if (underline != null) {
                font.setStyle(Font.UNDERLINE);
            }

            // テキストを挿入する
            if (!text.trim().isEmpty()) {  // スタンプで改行されないために
                theParagraph.add(new Chunk(text));
            }
        }

        // スタンプとシェーマを配置する
        private void startComponent(String name, String number) {

            int index = Integer.valueOf(number);
            PdfPTable pTable = null;
            
            if (name != null) {
                switch (name) {
                    case STAMP_HOLDER: {
                        ModuleModel stamp = modules.get(index);
                        StampTableMaker maker = new StampTableMaker();
                        pTable = maker.createTable(stamp);
                        break;
                    }
                    case SCHEMA_HOLDER: {
                        SchemaModel schema = schemas.get(index);
                        SchemaTableMaker maker = new SchemaTableMaker();
                        pTable = maker.createTable(schema);
                        break;
                    }
                }
            }

            // cellにスタンプを追加する
            if (pTable == null) {
                return;
            }
            cell.addElement(pTable);

            // スタンプを挿入した後はParagraphを作り直してcellに追加
            Paragraph p = createNewParagraph();
            // フォント・アライメントを引き継ぐ
            p.setFont(theParagraph.getFont());
            p.setAlignment(theParagraph.getAlignment());
            theParagraph = p;
            cell.addElement(theParagraph);
        }

        private class SchemaTableMaker {

            private PdfPTable createTable(SchemaModel schema) {

                try {
                    // Schemaはカラム数１のテーブル
                    PdfPTable table = new PdfPTable(1);
                    table.setSpacingBefore(1);
                    //table.setSpacingAfter(1);
                    table.setHorizontalAlignment(Element.ALIGN_LEFT);
                    // イメージのパーセントを設定
                    int percentage = Math.min(PERCENTAGE_IMAGE_WIDTH * karteTable.getColumnCount(), 100);
                    table.setWidthPercentage(percentage);
                    // SchemaModelからjpeg imageを取得
                    Image image = Image.getInstance(schema.getJpegByte());
                    // セルにimageを設定
                    PdfPCell pcell = new PdfPCell(image, true);
                    pcell.setBorder(Rectangle.NO_BORDER);
                    // テーブルに追加
                    table.addCell(pcell);

                    return table;

                } catch (BadElementException ex) {
                } catch (MalformedURLException ex) {
                } catch (IOException ex) {
                }

                return null;
            }
        }

        private class StampTableMaker {

            private PdfPTable table;
            private ModuleModel moduleModel;
            private String stampName;

            private PdfPTable createTable(ModuleModel stamp) {

                moduleModel = stamp;
                stampName = stamp.getModuleInfoBean().getStampName();

                try {
                    // スタンプのテーブルを作成、カラム数３
                    table = new PdfPTable(3);
                    table.setWidthPercentage(100);
                    table.setWidths(new int[]{7, 1, 2});    // てきとー
                    table.setHorizontalAlignment(Element.ALIGN_LEFT);
                    table.setSpacingAfter(5);
                    // スタンプの種類別に処理する
                    String entity = stamp.getModuleInfoBean().getEntity();
                    if (entity != null) {
                        switch (entity) {
                            case IInfoModel.ENTITY_MED_ORDER:
                                buildMedStamp();
                                break;
                            case IInfoModel.ENTITY_LABO_TEST:
                                buildDolphinStamp(hints.isLaboFold());
                                break;
                            default:
                                buildDolphinStamp(false);
                                break;
                        }
                    } else {
                        buildDolphinStamp(false);
                    }
                    return table;
                } catch (DocumentException ex) {
                }
                return null;
            }

            private void buildMedStamp() {
                // 処方スタンプ
                BundleMed model = (BundleMed) moduleModel.getModel();
                // タイトル
                String orderName = (hints.isNewStamp(stampName)) ? "RP) " : "RP) " + stampName;
                String classCode = hints.getMedTypeAndCode(model);
                writeTableTitle(orderName, classCode);

                // 項目
                for (ClaimItem item : model.getClaimItem()) {
                    writeClaimItem(item);
                }
                // 用法
                writeAdminUsage(model);

                // メモ
                writeMemo(model.getAdminMemo());
            }

            private void buildDolphinStamp(boolean foldItem) {

                BundleDolphin model = (BundleDolphin) moduleModel.getModel();
                // タイトル
                String orderName = model.getOrderName();
                if (!hints.isNewStamp(stampName)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(orderName);
                    sb.append("(").append(stampName).append(")");
                    orderName = sb.toString();
                }
                String classCode = model.getClassCode();
                writeTableTitle(orderName, classCode);
                // 項目
                if (foldItem) {
                    writeLaboFoldItem(model);
                } else {
                    for (ClaimItem item : model.getClaimItem()) {
                        writeClaimItem(item);
                    }
                }

                // メモ
                writeMemo(model.getMemo());

                // バンドル数量
                writeBundleNumber(model);
            }

            // テーブルタイトルを書き出す
            private void writeTableTitle(String orderName, String classCode) {
                table.addCell(createStampCell(orderName, 1, true));
                table.addCell(createStampCell(classCode, 2, true));
            }

            // ClaimItemを書き出す
            private void writeClaimItem(ClaimItem item) {

                if (hints.is84Code(item.getCode())) {
                    // 84系コードの場合、空白部分に数量を埋め込む
                    table.addCell(createStampCell(hints.build84Name(item), 3, false));

                } else if (hints.isCommentCode(item.getCode())) {
                    // コメントコードなら"・"と"x"は表示しない
                    table.addCell(createStampCell(item.getName(), 3, false));
                } else if (item.getNumber() != null && !item.getNumber().isEmpty()) {

                    table.addCell(createStampCell("・" + item.getName(), 1, false));
                    table.addCell(createStampCell(" x " + item.getNumber(), 1, false));
                    table.addCell(createStampCell(" " + hints.getUnit(item.getUnit()), 1, false));
                } else {

                    table.addCell(createStampCell("・" + item.getName(), 3, false));
                }
            }

            // ラボ項目を折りたたんで書き出す
            private void writeLaboFoldItem(BundleDolphin model) {
                table.addCell(createStampCell("・" + model.getItemNames(), 3, false));
            }

            // 内服の用法を書き出す
            private void writeAdminUsage(BundleMed model) {
                table.addCell(createStampCell(model.getAdminDisplayString(), 3, false));
            }

            // メモ行をかきだす
            private void writeMemo(String memo) {
                if (memo != null && !memo.isEmpty()) {
                    table.addCell(createStampCell(memo, 3, false));
                }
            }

            // バンドル数量を書き出す
            private void writeBundleNumber(BundleDolphin model) {

                if (model.getBundleNumber().startsWith("*")) {
                    table.addCell(createStampCell("・" + hints.parseBundleNum(model), 3, false));
                } else if (!VALUE_ONE.equals(model.getBundleNumber())) {
                    table.addCell(createStampCell("・回数", 1, false));
                    table.addCell(createStampCell(" x " + model.getBundleNumber(), 1, false));
                    table.addCell(createStampCell(" 回", 1, false));
                }
            }

            // スタンプ表示に使うPdfPCellを作成する
            private PdfPCell createStampCell(String str, int colSpan, boolean setBackground) {

                if (str == null) {
                    str = "";
                }
                PdfPCell pcell = new PdfPCell();
                pcell.setColspan(colSpan);
                pcell.setPadding(0);
                pcell.setBorder(Rectangle.NO_BORDER);
                if (setBackground) {
                    pcell.setBackgroundColor(STAMP_TITLE_BACKGROUND);
                }
                pcell.addElement(new Chunk(str, new Font(baseFont, STAMP_FONT_SIZE)));
                return pcell;
            }
        }

    }
}
