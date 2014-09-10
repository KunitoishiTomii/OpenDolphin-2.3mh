package open.dolphin.client;

import java.awt.Font;
import javax.swing.UIManager;
import open.dolphin.common.util.StampRenderingHints;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;

/**
 *
 * @author masuda, Masuda Naika
 */
public class FontManager {
    
    private static final int DEFAULT_FONT_SIZE = 13;
    private static final int[] FONT_SIZES = {10, 11, 12, DEFAULT_FONT_SIZE, 14, 15, 18, 20};
    private static final int[] FONT_SIZE_INCREMENTS = {-3, 0, 2, 5, 7, 9, 23};
    
    // フォント設定
    public static void initFonts() {
        
        String fontName = Project.getString(MiscSettingPanel.UI_FONT_NAME, MiscSettingPanel.DEFAULT_UI_FONT_NAME);
        int fontStyle = Project.getInt(MiscSettingPanel.UI_FONT_STYLE, MiscSettingPanel.DEFAULT_UI_FONT_STYLE);

        Font font11 = new Font(fontName, fontStyle, 11);
        Font font12 = new Font(fontName, fontStyle, 12);
        Font font13 = new Font(fontName, fontStyle, 13);

        UIManager.put("TextField.font", font13);
        UIManager.put("PasswordField.font", font13);
        UIManager.put("TextArea.font", font13);
        UIManager.put("OptionPane.font", font13);
        UIManager.put("OptionPane.messageFont", font13.deriveFont(Font.BOLD));
        UIManager.put("Label.font", font12);
        UIManager.put("Button.font", font12);
        UIManager.put("ToggleButton.font", font12);
        UIManager.put("Menu.font", font12);
        UIManager.put("MenuItem.font", font12);
        UIManager.put("CheckBox.font", font12);
        UIManager.put("CheckBoxMenuItem.font", font12);
        UIManager.put("RadioButton.font", font12);
        UIManager.put("RadioButtonMenuItem.font", font12);
        UIManager.put("ToolBar.font", font12);
        UIManager.put("ComboBox.font", font12);
        UIManager.put("TabbedPane.font", font12);
        UIManager.put("TitledBorder.font", font12);
        UIManager.put("List.font", font12);
        UIManager.put("Table.font", font12);
        UIManager.put("TabbedPane.font", font12);
        UIManager.put("TableHeader.font", font11);    // 小さ目

        updateFonts();
    }
    
    public static void updateFonts() {
        
        String fontName = Project.getString(MiscSettingPanel.UI_FONT_NAME, MiscSettingPanel.DEFAULT_UI_FONT_NAME);
        int fontSize = Project.getInt(MiscSettingPanel.UI_FONT_SIZE, MiscSettingPanel.DEFAULT_UI_FONT_SIZE);
        int fontStyle = Project.getInt(MiscSettingPanel.UI_FONT_STYLE, MiscSettingPanel.DEFAULT_UI_FONT_STYLE);


        Font fontTp = new Font(fontName, fontStyle, fontSize);

        UIManager.put("TextPane.font", fontTp);

        // スタンプホルダフォント
        String stampFontName = Project.getString(MiscSettingPanel.STAMP_FONT_NAME, MiscSettingPanel.DEFAULT_STAMP_FONT_NAME);
        int stampFontSize = Project.getInt(MiscSettingPanel.STAMP_FONT_SIZE, MiscSettingPanel.DEFAULT_STAMP_FONT_SIZE);
        int stampFontStyle = Project.getInt(MiscSettingPanel.STAMP_FONT_STYLE, MiscSettingPanel.DEFAULT_STAMP_FONT_STYLE);
        StampRenderingHints.getInstance().setFont(stampFontName, stampFontStyle, stampFontSize);
    }
    
    public static int[] getFontSizes() {
        return FONT_SIZES;
    }

    public static int[] getFontSizeIncrements() {
        return FONT_SIZE_INCREMENTS;
    }
    
    public static int getFontSize(int index, int baseFontSize) {
        if (index >= 0 && index < FONT_SIZE_INCREMENTS.length) {
            return baseFontSize + FONT_SIZE_INCREMENTS[index];
        }
        return baseFontSize;
    }
    
    public static int toViewFontSize(int modelFontSize, int baseFontSize) {
        return baseFontSize + modelFontSize - DEFAULT_FONT_SIZE;

    }

    public static int toModelFontSize(int viewFontSize, int baseFontSize) {
        return DEFAULT_FONT_SIZE + viewFontSize - baseFontSize;
    }
}
