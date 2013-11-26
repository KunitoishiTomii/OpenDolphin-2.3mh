package open.dolphin.common.util;

import java.awt.Color;
import open.dolphin.infomodel.BundleDolphin;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.IInfoModel;

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
    private boolean laboFold = true;

    private static final StampRenderingHints instance;
    
    static {
        instance = new StampRenderingHints();
    }

    private StampRenderingHints() {
    }

    public static StampRenderingHints getInstance() {
        return instance;
    }
    
    public void setLaboFold(boolean laboFold) {
        this.laboFold = laboFold;
    }
    public boolean isLaboFold() {
        return laboFold;
    }

    // velocityから使う↓
    public boolean isNewStamp(String stampName) {
        return "新規スタンプ".equals(stampName) 
                || IInfoModel.TITLE_FROM_EDITOR.equals(stampName) 
                || "チェックシート".equals(stampName);
    }

    public boolean isCommentCode(String code) {
        return code.matches(ClaimConst.REGEXP_COMMENT_MED);
    }
    
    public boolean is84Code(String code) {
        if (code != null) {
            return code.startsWith("84") || code.startsWith("0084");
        }
        return false;
    }
    
    public String build84Name(ClaimItem item) {
        try {
            char[] chars = item.getName().toCharArray();
            String[] numTokens = item.getNumber().split("-");
            StringBuilder sb = new StringBuilder();
            boolean skip = false;
            int index = 0;

            for (char c : chars) {
                if (c == ' ' || c == '　') {
                    if (!skip) {
                        sb.append(' ').append(numTokens[index]);
                        index++;
                    }
                    skip = true;
                } else {
                    sb.append(c);
                    skip = false;
                }
            }
            return sb.toString();
        } catch (Exception ex) {
        }
        
        return item.getName() + " " + item.getNumber();
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
