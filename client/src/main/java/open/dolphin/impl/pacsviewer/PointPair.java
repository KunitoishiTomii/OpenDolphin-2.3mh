package open.dolphin.impl.pacsviewer;

import java.awt.Point;

/**
 * 計測２点を記憶するクラス
 *
 * @author masuda, Masuda Naika
 */
public class PointPair {

    private final Point start;
    private Point end;

    public PointPair(Point start, Point end) {
        this.start = start;
        this.end = end;
    }

    public void setEndPoint(Point ep) {
        end = ep;
    }

    public Point getStartPoint() {
        return start;
    }

    public Point getEndPoint() {
        return end;
    }
}
