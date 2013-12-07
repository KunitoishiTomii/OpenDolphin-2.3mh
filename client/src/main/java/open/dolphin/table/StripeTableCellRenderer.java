package open.dolphin.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import open.dolphin.client.ClientContext;

/**
 * ストライプテーブルのセルレンダラ
 *
 * @author masuda, Masuda Naika
 */

public class StripeTableCellRenderer extends DefaultTableCellRenderer {

    private static final Border emptyBorder = BorderFactory.createEmptyBorder();
    private static final Border rtPadding = BorderFactory.createEmptyBorder(0, 0, 0, 8);
    private static final Color DEFAULT_ODD_COLOR = ClientContext.getColor("color.odd");
    //private static final Color DEFAULT_EVEN_COLOR = ClientContext.getColor("color.even");
    private static final Color DEFAULT_EVEN_COLOR = ClientContext.getZebraColor();
    private static final Color[] ROW_COLORS = {DEFAULT_EVEN_COLOR, DEFAULT_ODD_COLOR};
    private static final int ROW_HEIGHT = 18;

    private JTable table;
    private TableCellRenderer booleanRenderer;

    
    public StripeTableCellRenderer() {
        super();
    }
    public StripeTableCellRenderer(JTable table) {
        super();
        setTable(table);
    }

    public final void setTable(JTable table) {
        this.table = table;
        table.setRowHeight(ROW_HEIGHT);
        table.setFillsViewportHeight(true);   // viewportは広げておく
        //table.setShowVerticalLines(false);
        //table.setShowHorizontalLines(false);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setUI(new StripeTableUI());
        // JTable.BooleanRendererを保存
        booleanRenderer = table.getDefaultRenderer(Boolean.class);
    }

    // このレンダラでレンダリングするクラスを指定する
    public void setDefaultRenderer() {
        table.setDefaultRenderer(String.class, this);
        table.setDefaultRenderer(Boolean.class, this);
        table.setDefaultRenderer(Number.class, this);
    }

    // 選択・非選択の色分けはここでする。特に指定したいときは後で上書き
    // ストライプはStripeTableUIが描画する
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        // オブジェクトに応じたComponentを取得する
        JComponent jc;
        setOpaque(true);
        if (value instanceof Boolean) {
            jc = (JComponent) booleanRenderer.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            //setHorizontalAlignment(CEHNTER);
            setBorder(emptyBorder);
        } else if (value instanceof Number) {
            jc = (JComponent) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(RIGHT);
            setBorder(rtPadding);
        } else {
            jc = (JComponent) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(LEFT);
            setBorder(emptyBorder);
        }
        
        // 選択・非選択に応じて色分けを設定する
        if (isSelected) {
            jc.setForeground(table.getSelectionForeground());
            jc.setBackground(table.getSelectionBackground());
            //jc.setOpaque(true);
        } else {
            jc.setForeground(table.getForeground());
            jc.setBackground(table.getBackground());
            //jc.setOpaque(false);
        }
        
        return jc;
    }
    
    // テーブルにストライプの背景を描く
    // http://explodingpixels.wordpress.com/2008/10/05/making-a-jtable-fill-the-view-without-extension/
    // を改変。popupやtooltip表示後乱れるのを修正
    private static class StripeTableUI extends BasicTableUI {

        @Override
        public void paint(Graphics g, JComponent c) {
            
            final Rectangle clipBounds = g.getClipBounds();
            final int rowHeight = table.getRowHeight();
            final int endY = clipBounds.y + clipBounds.height;

            int topY = clipBounds.y;
            int currentRow = topY / rowHeight;
            int height = rowHeight - topY % rowHeight;
            
            while (topY < endY) {
                int bottomY = topY + height;
                g.setColor(ROW_COLORS[currentRow & 1]);
                g.fillRect(clipBounds.x, topY, clipBounds.width, Math.min(bottomY, endY));
                topY = bottomY;
                height = rowHeight;
                currentRow++;
            }
            super.paint(g, c);
        }
    }
}
