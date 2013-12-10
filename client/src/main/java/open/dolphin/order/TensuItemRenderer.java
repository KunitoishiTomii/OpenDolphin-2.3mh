package open.dolphin.order;

import java.awt.Color;
import java.awt.Component;
import java.util.regex.Pattern;
import javax.swing.JTable;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.table.ListTableSorter;
import open.dolphin.table.StripeTableCellRenderer;

/**
 * TensuItemRenderer
 * 
 * @author Kazushi Minagawa.
 * @author modified by masuda, Masuda Naika
 */
public final class TensuItemRenderer extends StripeTableCellRenderer {

    private static final Color THEC_COLOR = new Color(204, 255, 102);
    private static final Color MEDICINE_COLOR = new Color(255, 204, 0);
    private static final Color MATERIAL_COLOR = new Color(153, 204, 255);
    private static final Color OTHER_COLOR = new Color(255, 255, 255);
    private final Pattern passPattern;
    private final Pattern shinkuPattern;

    public TensuItemRenderer(Pattern passPattern, Pattern shinkuPattern) {
        super();
        this.passPattern = passPattern;
        this.shinkuPattern = shinkuPattern;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        //ListTableModel<TensuMaster> tm = (ListTableModel<TensuMaster>) table.getModel();
        //TensuMaster item = tm.getObject(row);
        ListTableSorter<TensuMaster> sorter = (ListTableSorter<TensuMaster>) table.getModel();
        TensuMaster item = sorter.getObject(row);

        if (item != null) {

            String slot = item.getSlot();

            if (passPattern != null && passPattern.matcher(slot).find()) {

                String srycd = item.getSrycd();

                if (srycd.startsWith(ClaimConst.SYUGI_CODE_START)
                        && shinkuPattern != null
                        && shinkuPattern.matcher(item.getSrysyukbn()).find()) {
                    setBackground(THEC_COLOR);

                } else if (srycd.startsWith(ClaimConst.YAKUZAI_CODE_START)) {
                    //内用1、外用6、注射薬4
                    String ykzkbn = item.getYkzkbn();
                    if (ykzkbn != null) {
                        switch (ykzkbn) {
                            case ClaimConst.YKZ_KBN_NAIYO:
                                setBackground(MEDICINE_COLOR);
                                break;
                            case ClaimConst.YKZ_KBN_INJECTION:
                                setBackground(MEDICINE_COLOR);
                                break;
                            case ClaimConst.YKZ_KBN_GAIYO:
                                setBackground(MEDICINE_COLOR);
                                break;
                            default:
                                setBackground(OTHER_COLOR);
                                break;
                        }
                    } else {
                        setBackground(OTHER_COLOR);
                    }

                } else if (srycd.startsWith(ClaimConst.ZAIRYO_CODE_START)) {
                    setBackground(MATERIAL_COLOR);

                } else if (srycd.startsWith(ClaimConst.ADMIN_CODE_START)) {
                    setBackground(OTHER_COLOR);

                } else if (srycd.startsWith(ClaimConst.RBUI_CODE_START)) {
                    setBackground(THEC_COLOR);

                } else {
                    setBackground(OTHER_COLOR);
                }

            } else {
                setBackground(OTHER_COLOR);
            }

        } else {
            setBackground(OTHER_COLOR);
        }

        return this;
    }
}
