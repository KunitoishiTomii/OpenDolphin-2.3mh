package open.dolphin.client;

import java.awt.Color;
import java.io.StringWriter;
import open.dolphin.infomodel.BundleDolphin;
import open.dolphin.infomodel.BundleMed;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.project.Project;
import open.dolphin.util.TemplateLoader;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

/**
 * StampRenderingHints
 *
 * @author Minagawa, Kazushi
 * @author modified by masuda, Masuda Naika
 */
public class StampRenderingHints {

    private int fontSize = 12;
    private Color foreground;
    private Color background = Color.WHITE;
    private Color labelColor;
    private int border = 0;
    private int cellSpacing = 1;    //masuda 0 -> 1 to avoid unexpected line wrap
    private int cellPadding = 0;    //masuda 3 -> 0 to make slim

    private static final String KEY_MODEL = "model";
    private static final String KEY_STAMP_NAME = "stampName";
    private static final String KEY_HINTS = "hints";
    private static final String DOT_VM = ".vm";
    
    private static final StampRenderingHints instance;
    
    private Template medTemplate;
    private Template dolphinTemplate;
    private Template laboTemplate;

    static {
        instance = new StampRenderingHints();
    }

    private StampRenderingHints() {
        int cp = Project.getInt("stampHolderCellPadding", 0);
        setCellPadding(cp);
        prepareTemplates();
    }

    public static StampRenderingHints getInstance() {
        return instance;
    }
    
    private void prepareTemplates() {
        TemplateLoader templateLoader = new TemplateLoader();
        medTemplate = templateLoader.newTemplate(BundleMed.class.getName() + DOT_VM);
        dolphinTemplate = templateLoader.newTemplate(BundleDolphin.class.getName() + DOT_VM);
        laboTemplate = templateLoader.newTemplate("labo.vm");
    }
    
    public String getStampHtml(StampHolder sh) {

        // entityを取得
        String entity = sh.getStamp().getModuleInfoBean().getEntity();
        
        // entityに応じてテンプレートを選択
        Template template;        
        switch (entity) {
            case IInfoModel.ENTITY_MED_ORDER:
                template = medTemplate;
                break;
            case IInfoModel.ENTITY_LABO_TEST:
                template = Project.getBoolean("laboFold", true)
                        ? laboTemplate : dolphinTemplate;
                break;
            default:
                template = dolphinTemplate;
                break;
        }
        
        VelocityContext context = new VelocityContext();
        context.put(KEY_HINTS, sh.getHints());
        context.put(KEY_MODEL, sh.getStamp().getModel());
        context.put(KEY_STAMP_NAME, sh.getStamp().getModuleInfoBean().getStampName());
            
        StringWriter sw = new StringWriter();
        template.merge(context, sw);

        String text = sw.toString();
        
        return text;
    }
    
    // velocityから使う↓
    public boolean isNewStamp(String stampName) {
        return "新規スタンプ".equals(stampName) 
                || "エディタから発行...".equals(stampName) 
                || "チェックシート".equals(stampName);
    }

    public boolean isCommentCode(String code) {
        return code.matches(ClaimConst.REGEXP_COMMENT_MED);
    }
    
    public String getMedTypeAndCode(BundleDolphin model) {
        StringBuilder sb = new StringBuilder();
        sb.append(model.getMemo().replace("処方", "")).append("/");
        sb.append(model.getClassCode());
        return sb.toString();
    }
    
    public String getUnit(String unit) {
        if (unit == null) {
            return null;
        }
        return unit.replace("カプセル", "Ｃ");
    }

    public String parseBundleNum(BundleDolphin model) {
        String str = model.getBundleNumber().substring(1);
        int len = str.length();
        int pos = str.indexOf("/");
        StringBuilder sb = new StringBuilder();
        sb.append("回数：");
        sb.append(str.substring(0, pos));
        sb.append("　実施日：");
        sb.append(str.substring(pos + 1, len));
        sb.append("日");
        return sb.toString();
    }
    // velocityから使う↑

    
    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public Color getForeground() {
        return foreground;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    public Color getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(Color labelColor) {
        this.labelColor = labelColor;
    }

    public int getBorder() {
        return border;
    }

    public void setBorder(int border) {
        this.border = border;
    }

    public int getCellPadding() {
        return cellPadding;
    }

    public final void setCellPadding(int cellPadding) {
        this.cellPadding = cellPadding;
    }

    public int getCellSpacing() {
        return cellSpacing;
    }

    public void setCellSpacing(int cellSpacing) {
        this.cellSpacing = cellSpacing;
    }

    public String getForegroundAs16String() {
        if (getForeground() == null) {
            return "#000C9C";
        } else {
            int r = getForeground().getRed();
            int g = getForeground().getGreen();
            int b = getForeground().getBlue();
            StringBuilder sb = new StringBuilder();
            sb.append("#");
            sb.append(Integer.toHexString(r));
            sb.append(Integer.toHexString(g));
            sb.append(Integer.toHexString(b));
            return sb.toString();
        }
    }

    public String getBackgroundAs16String() {
        if (getBackground() == null) {
            return "#FFFFFF";
        } else {
            int r = getBackground().getRed();
            int g = getBackground().getGreen();
            int b = getBackground().getBlue();
            StringBuilder sb = new StringBuilder();
            sb.append("#");
            sb.append(Integer.toHexString(r));
            sb.append(Integer.toHexString(g));
            sb.append(Integer.toHexString(b));
            return sb.toString();
        }
    }

    public String getLabelColorAs16String() {
        if (getLabelColor() == null) {
            return "#FFCED9";
        } else {
            int r = getLabelColor().getRed();
            int g = getLabelColor().getGreen();
            int b = getLabelColor().getBlue();
            StringBuilder sb = new StringBuilder();
            sb.append("#");
            sb.append(Integer.toHexString(r));
            sb.append(Integer.toHexString(g));
            sb.append(Integer.toHexString(b));
            return sb.toString();
        }
    }
}
