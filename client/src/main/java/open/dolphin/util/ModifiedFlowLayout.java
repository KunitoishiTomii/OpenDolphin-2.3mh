package open.dolphin.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;


/**
 * A modified version of FlowLayout that allows containers using this Layout to
 * behave in a reasonable manner when placed inside a JScrollPane
 * 
 * http://www.javakb.com/Uwe/Forum.aspx/java-gui/1904/Flowlayout-JPanel-and-JScrollPane-Scrolling-vertically-impossible
 * @author Babu Kalakrishnan
 * @author masuda, Mauda Naika
 */
public class ModifiedFlowLayout extends FlowLayout {

    public ModifiedFlowLayout() {
        super();
    }

    public ModifiedFlowLayout(int align) {
        super(align);
    }

    public ModifiedFlowLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return computeSize(target, false);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return computeSize(target, true);
    }

    private Dimension computeSize(Container target, boolean minimum) {
        synchronized (target.getTreeLock()) {
            int hgap = getHgap();
            int vgap = getVgap();
            int w = target.getWidth();

            // Let this behave like a regular FlowLayout (single row)
            // if the container hasn't been assigned any size yet    
            if (w == 0) {
                w = Integer.MAX_VALUE;
            }

            Insets insets = target.getInsets();
            if (insets == null) {
                insets = new Insets(0, 0, 0, 0);
            }
            
            Dimension reqdSize = new Dimension();
            int maxwidth = w - (insets.left + insets.right + hgap * 2);
            int n = target.getComponentCount();
            int x = 0;
            int rowHeight = 0;

            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                if (c.isVisible()) {
                    Dimension d = minimum ? c.getMinimumSize() : c.getPreferredSize();
                    if (x == 0 || x + d.width <= maxwidth) {
                        if (x > 0) {
                            x += hgap;
                        }
                        x += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        x = d.width;
                        reqdSize.height += vgap + rowHeight;
                        rowHeight = d.height;
                    }
                    reqdSize.width = Math.max(reqdSize.width, x);
                }
            }
            reqdSize.height += rowHeight;

            reqdSize.width += insets.left + insets.right + hgap * 2;
            reqdSize.height += insets.top + insets.bottom + vgap * 2;

            return reqdSize;
        }
    }
}
