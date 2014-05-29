package open.dolphin.impl.pacsviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * 計測線を表示するパネル
 *
 * @author masuda, Masuda Naika
 */
public class DicomMeasurePanel extends JPanel {

    private final DicomViewerRootPane parent;
    private final List<PointPair> measureList;

    private double pixelSpacingX;
    private double pixelSpacingY;

    private static final BasicStroke STROKE_DOTTED
            = new BasicStroke(1,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    10.0f,
                    new float[]{5f},
                    0.0f);
    private static final BasicStroke STROKE_PLAIN = new BasicStroke();
    private static final DecimalFormat frmt = new DecimalFormat("0.0");

    public DicomMeasurePanel(DicomViewerRootPane parent) {
        this.parent = parent;
        measureList = new ArrayList();
        setOpaque(false);
    }

    public List<PointPair> getMeasureList() {
        return measureList;
    }

    public void setPixelSpacing(double pSpacingX, double pSpacingY) {
        pixelSpacingX = pSpacingX;
        pixelSpacingY = pSpacingY;
    }

    @Override
    protected void paintComponent(Graphics g) {

        if (measureList != null && !measureList.isEmpty()) {
            Graphics2D g2D = (Graphics2D) g;
            double scale = parent.getCurrentScale();
            g2D.setColor(Color.MAGENTA);
            for (PointPair pair : measureList) {
                int sx = (int) (pair.getStartPoint().x * scale);
                int sy = (int) (pair.getStartPoint().y * scale);
                int ex = (int) (pair.getEndPoint().x * scale);
                int ey = (int) (pair.getEndPoint().y * scale);
                // ２点間に波線を描画
                g2D.setStroke(STROKE_DOTTED);
                g2D.drawLine(sx, sy, ex, ey);
                // 両端に十字を描画
                final int len = 3;
                g2D.setStroke(STROKE_PLAIN);
                g2D.drawLine(sx - len, sy, sx + len, sy);
                g2D.drawLine(sx, sy - len, sx, sy + len);
                g2D.drawLine(ex - len, ey, ex + len, ey);
                g2D.drawLine(ex, ey - len, ex, ey + len);
                // 中心付近に距離を描画
                g2D.drawString(getDistance(pair),
                        (sx + ex) / 2, (sy + ey) / 2 - 4);
            }
        }
    }

    private String getDistance(PointPair pair) {

        Point start = pair.getStartPoint();
        Point end = pair.getEndPoint();

        if (pixelSpacingX == 0 || pixelSpacingY == 0) {
            Double d = Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
            return frmt.format(d) + "px";
        }
        Double d = Math.sqrt(Math.pow((start.x - end.x) * pixelSpacingX, 2) + Math.pow((start.y - end.y) * pixelSpacingY, 2));
        if (d > 10) {
            return frmt.format(d / 10) + "cm";
        } else {
            return frmt.format(d) + "mm";
        }
    }
}
