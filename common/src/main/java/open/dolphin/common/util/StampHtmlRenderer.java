package open.dolphin.common.util;

import open.dolphin.infomodel.BundleDolphin;
import open.dolphin.infomodel.BundleMed;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleModel;

/**
 * StAXでStampHolderのHTML作成
 * 
 * @author masuda, Masuda Naika
 */
public class StampHtmlRenderer {
    
    private static final String TAG_HTML = "HTML";
    private static final String TAG_BODY = "BODY";
    private static final String TAG_TT = "TT";
    private static final String TAG_FONT = "FONT";
    private static final String TAG_TABLE = "TABLE";
    private static final String TAG_TR = "TR";
    private static final String TAG_TD = "TD";
    
    private static final String ATTR_SIZE = "SIZE";
    private static final String ATTR_COLOR = "COLOR";
    private static final String ATTR_BGCOLOR = "BGCOLOR";
    private static final String ATTR_BORDER = "BORDER";
    private static final String ATTR_CELLSPACING = "CELLSPACING";
    private static final String ATTR_CELLPADDING = "CELLPADDING";
    private static final String ATTR_NOWRAP = "NOWRAP";
    private static final String ATTR_COLSPAN = "COLSPAN";
    private static final String ATTR_ALIGN = "ALIGN";
    private static final String VALUE_RIGHT = "RIGHT";
    //private static final String VALUE_NOWRAP = "NOWRAP";
    private static final String VALUE_NOWRAP = null;
    private static final String VALUE_ONE = "1";
    private static final String VALUE_TWO = "2";
    private static final String VALUE_THREE = "3";
    
    private final ModuleModel moduleModel;
    private final StampRenderingHints hints;
    private final String stampName;
    private boolean includeHtmlBody;
    
    private SimpleXmlWriter writer;
    
    
    public StampHtmlRenderer(ModuleModel moduleModel, StampRenderingHints hints) {
        this.moduleModel = moduleModel;
        this.hints = hints;
        stampName = moduleModel.getModuleInfoBean().getStampName();
    }
    
    public String getStampHtml(boolean includeHtmlBody) {
        
        this.includeHtmlBody = includeHtmlBody;
        
        writer = new SimpleXmlWriter();
        writer.setRepcaceXmlChar(false);
        writer.setReplaceZenkaku(true);

        // entityを取得
        String entity = moduleModel.getModuleInfoBean().getEntity();
        switch (entity) {
            case IInfoModel.ENTITY_MED_ORDER:
                buildMedStampHtml();
                break;
            case IInfoModel.ENTITY_LABO_TEST:
                buildDolphinStampHtml(hints.isLaboFold());
                break;
            default:
                buildDolphinStampHtml(false);
                break;
        }

        String html = writer.getProduct();
        
        return html;
    }
    
    private void buildMedStampHtml() {
        
        BundleMed model = (BundleMed) moduleModel.getModel();
        
        // HTML開始
        writeStartTable();
        
        // タイトル
        String orderName = (hints.isNewStamp(stampName)) ? "RP) " : "RP) " + stampName;
        String classCode = hints.getMedTypeAndCode(model);
        writeTableTitle(orderName, classCode);
        
        // 項目
        for (ClaimItem item : model.getClaimItem()) {
            writeMedItem(item);
        }
        
        // 用法
        writeAdminUsage(model);
        
        // メモ
        writeMemo(model.getAdminMemo());
        
        // HTML終了
        writer.writeEndDocument();
    }
    
    private void buildDolphinStampHtml(boolean foldItem) {
        
        BundleDolphin model = (BundleDolphin) moduleModel.getModel();
        
        // HTML開始
        writeStartTable();
        
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
        
        // 終わり
        writer.writeEndDocument();
    }
    
    // テーブル開始を書き出す
    private void writeStartTable() {
        
        if (includeHtmlBody) {
            writer.writeStartElement(TAG_HTML)
                    .writeStartElement(TAG_BODY)
                    .writeStartElement(TAG_TT)
                    .writeStartElement(TAG_FONT)
                    .writeAttribute(ATTR_SIZE, String.valueOf(hints.getFontSize()))
                    .writeAttribute(ATTR_COLOR, hints.getBackgroundAs16String());
        }
        
        writer.writeStartElement(TAG_TABLE)
                .writeAttribute(ATTR_BORDER, String.valueOf(hints.getBorder()))
                .writeAttribute(ATTR_CELLSPACING, String.valueOf(hints.getCellSpacing()))
                .writeAttribute(ATTR_CELLPADDING, String.valueOf(hints.getCellPadding()));
    }
    
    // テーブルタイトルを書き出す
    private void writeTableTitle(String oderName, String classCode) {
        
        writer.writeStartElement(TAG_TR)
                .writeAttribute(ATTR_BGCOLOR, hints.getLabelColorAs16String());
        
            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_NOWRAP, VALUE_NOWRAP)
                    .writeCharacters(oderName)
                    .writeEndElement();

            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_COLSPAN, VALUE_TWO)
                    .writeAttribute(ATTR_ALIGN, VALUE_RIGHT)
                    .writeCharacters(classCode)
                    .writeEndElement();
            
        writer.writeEndElement();
    }
    
    // Med ClaimItemを書き出す
    private void writeMedItem(ClaimItem item) {
        
        writer.writeStartElement(TAG_TR);

        // コメントコードなら"・"と"x"は表示しない
        if (hints.isCommentCode(item.getCode())) {
            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_COLSPAN, VALUE_THREE)
                    .writeCharacters(item.getName())
                    .writeEndElement();
        } else {
            writer.writeStartElement(TAG_TD)
                    .writeCharacters("・")
                    .writeCharacters(item.getName())
                    .writeEndElement();

            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_NOWRAP, VALUE_NOWRAP)
                    .writeAttribute(ATTR_ALIGN, VALUE_RIGHT)
                    .writeCharacters(" x ")
                    .writeCharacters(item.getNumber())
                    .writeEndElement();

            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_NOWRAP, VALUE_NOWRAP)
                    .writeCharacters(" ")
                    .writeCharacters(hints.getUnit(item.getUnit()))
                    .writeEndElement();
        }

        writer.writeEndElement();
    }
    
    // ClaimItemを書き出す
    private void writeClaimItem(ClaimItem item) {
        
        writer.writeStartElement(TAG_TR);
        
        // コメントコードなら"・"と"x"は表示しない
        String itemName = item.getName();
        if (!hints.isCommentCode(item.getCode())) {
            itemName = "・" + itemName;
        }

        if (item.getNumber() != null && !item.getNumber().isEmpty()) {
            writer.writeStartElement(TAG_TD)
                    .writeCharacters(itemName)
                    .writeEndElement();

            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_NOWRAP, VALUE_NOWRAP)
                    .writeAttribute(ATTR_ALIGN, VALUE_RIGHT)
                    .writeCharacters(" x ")
                    .writeCharacters(item.getNumber())
                    .writeEndElement();

            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_NOWRAP, VALUE_NOWRAP)
                    .writeCharacters(" ")
                    .writeCharacters(hints.getUnit(item.getUnit()))
                    .writeEndElement();
        } else {
            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_COLSPAN, VALUE_THREE)
                    .writeCharacters(itemName)
                    .writeEndElement();
        }

        writer.writeEndElement();
    }
    
    // ラボ項目を折りたたんで書き出す
    private void writeLaboFoldItem(BundleDolphin model) {
        
        writer.writeStartElement(TAG_TR);
            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_COLSPAN, VALUE_THREE)
                    .writeCharacters("・")
                    .writeCharacters(model.getItemNames())
                    .writeEndElement();
        writer.writeEndElement();
    }
    
    // 内服の用法を書き出す
    private void writeAdminUsage(BundleMed model) {
        writer.writeStartElement(TAG_TR);
            writer.writeStartElement(TAG_TD)
                    .writeAttribute(ATTR_COLSPAN, VALUE_THREE)
                    .writeCharacters(model.getAdminDisplayString())
                    .writeEndElement();
        writer.writeEndElement();
    }
    
    // メモ行をかきだす
    private void writeMemo(String memo) {
        
        if (memo != null && !memo.isEmpty()) {
            writer.writeStartElement(TAG_TR);
                writer.writeStartElement(TAG_TD)
                        .writeAttribute(ATTR_COLSPAN, VALUE_THREE)
                        .writeCharacters(memo)
                        .writeEndElement();
            writer.writeEndElement();
        }
    }
    
    // バンドル数量を書き出す
    private void writeBundleNumber(BundleDolphin model) {
        
        if (model.getBundleNumber().startsWith("*")) {
            writer.writeStartElement(TAG_TR);
                writer.writeStartElement(TAG_TD)
                        .writeAttribute(ATTR_COLSPAN, VALUE_THREE)
                        .writeCharacters("・")
                        .writeCharacters(hints.parseBundleNum(model))
                        .writeEndElement();
            writer.writeEndElement();
        } else if (!VALUE_ONE.equals(model.getBundleNumber())) {
            writer.writeStartElement(TAG_TR)
                    .writeStartElement(TAG_TD)
                    .writeCharacters("・回数")
                    .writeEndElement();
                
                writer.writeStartElement(TAG_TD)
                        .writeAttribute(ATTR_NOWRAP, VALUE_NOWRAP)
                        .writeAttribute(ATTR_ALIGN, VALUE_RIGHT)
                        .writeCharacters(" x ")
                        .writeCharacters(model.getBundleNumber())
                        .writeEndElement();
                
                writer.writeStartElement(TAG_TD)
                        .writeCharacters(" 回")
                        .writeEndElement();
            writer.writeEndElement();
        }
    }
}
