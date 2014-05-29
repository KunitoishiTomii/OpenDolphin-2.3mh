package open.dolphin.impl.pacsviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JPanel;

/**
 * 領域選択パネル
 * 
 * @author masuda, Masuda Naika
 */
public class DicomSelectionPanel extends JPanel {
    
    private static final BasicStroke STROKE_DOTTED
            = new BasicStroke(1,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10.0f,
                    new float[]{5f},
                    0.0f);
    
    private final DicomViewerRootPane parent;
    private Point start;
    private Point end;

    
    public DicomSelectionPanel(DicomViewerRootPane parent) {
        this.parent = parent;
        setOpaque(false);
    }
    
    public void setStartPoint(Point p) {
        start = p;
    }
    
    public Point getStartPoint() {
        return start;
    }
    
    public void setEndPoint(Point p) {
        end = p;
    }
    
    public Point getEndPoint() {
        return end;
    }
    
    public void clearPoints() {
        start = null;
        end = null;
    }

    @Override
    protected void paintComponent(Graphics g) {

        if (start == null || end == null) {
            return;
        }

        Graphics2D g2D = (Graphics2D) g;
        g2D.setColor(Color.MAGENTA);
        g2D.setStroke(STROKE_DOTTED);
        Rectangle r = getSelectedRectangle();
        g2D.drawRect(r.x, r.y, r.width, r.height);

    }

    public Rectangle getSelectedRectangle() {
        
        double scale = parent.getCurrentScale();
        int x = (int) (Math.min(start.x, end.x) * scale);
        int y = (int) (Math.min(start.y, end.y) * scale);
        int width = (int) (Math.abs(start.x - end.x) * scale);
        int height = (int) (Math.abs(start.y - end.y) * scale);
        Rectangle r = new Rectangle(x, y, width, height);
        
        return r;
    }
}
