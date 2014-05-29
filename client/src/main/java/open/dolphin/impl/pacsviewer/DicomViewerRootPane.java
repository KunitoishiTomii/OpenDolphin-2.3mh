package open.dolphin.impl.pacsviewer;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import open.dolphin.tr.ImageTransferable;
import open.dolphin.util.ImageTool;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * DICOM画像表示の基底ペイン
 *
 * @author masuda, Masauda Naika
 */
public class DicomViewerRootPane extends JLayeredPane {

    private final JPanel topPanel;
    private final DicomSelectionPanel selectionPanel;
    private final DicomInfoLabel infoLabel;
    private final DicomImagePanel imagePanel;
    private final DicomMeasurePanel measurePanel;
    private final JLayeredPane baseLayer;

    private final DicomViewer viewer;
    // ウィンドウサイズにフィットしたスケール
    private double initialScale;
    // 現在のスケール
    private double currentScale;
    // 現在の拡大インデックス
    private int pow;

    public DicomViewerRootPane(DicomViewer viewer) {

        this.viewer = viewer;
        topPanel = new JPanel();
        baseLayer = new JLayeredPane();
        infoLabel = new DicomInfoLabel();
        selectionPanel = new DicomSelectionPanel(this);
        imagePanel = new DicomImagePanel(this);
        measurePanel = new DicomMeasurePanel(this);

        // baseLayerにはimagePanel, infoLabel, measurePanel, selectionPanelを設置する
        baseLayer.add(imagePanel, DEFAULT_LAYER);
        baseLayer.add(infoLabel, PALETTE_LAYER);
        baseLayer.add(measurePanel, MODAL_LAYER);
        baseLayer.add(selectionPanel, POPUP_LAYER);

        // DicomViewerPaneにはbaseLayer, topPanelを設置する
        add(baseLayer, DEFAULT_LAYER);
        add(topPanel, DRAG_LAYER);

        // topPanelは内容なくマウス処理用。マウスリスナーをセットする
        MyMouseAdapter adapter = new MyMouseAdapter();
        topPanel.addMouseWheelListener(adapter);
        topPanel.addMouseListener(adapter);
        topPanel.addMouseMotionListener(adapter);

        // topPanelはDicomViewerPaneのサイズに同期させる
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension d = e.getComponent().getSize();
                topPanel.setSize(d);
            }
        });
    }

    public double getCurrentScale() {
        return currentScale;
    }
    
    // imagePanelにimageを設定する
    public void setDicomObject(DicomObject object) throws IOException {
        
        BufferedImage image = ImageTool.getDicomImage(object);
        setPixelSpacing(object.getDoubles(Tag.PixelSpacing));
        
        imagePanel.setImage(image);
        infoLabel.setWindowLevel(imagePanel.getWindowLevel());
        infoLabel.setWindowWidth(imagePanel.getWindowWidth());
        // 画像情報を設定する
        infoLabel.setDicomInfo(new DicomImageInfo(object));
        
        resetImage2();
    }

    // 画像のPixel spacingを設定する
    private void setPixelSpacing(double[] pixelSpacing) {

        double pixelSpacingX = 0;
        double pixelSpacingY = 0;

        if (pixelSpacing != null) {
            pixelSpacingX = pixelSpacing[0];
            pixelSpacingY = pixelSpacing[1];
        }
        measurePanel.setPixelSpacing(pixelSpacingX, pixelSpacingY);
    }

    // 移動・拡大を初期値に戻し、計測を消去。
    public void resetImage() {

        baseLayer.setLocation(0, 0);

        int wl = DicomImagePanel.DEFAULT_WL;
        int ww = DicomImagePanel.DEFALUT_WW;
        imagePanel.setWindowLevel(wl, ww);
        imagePanel.setLUT();
        zoomReset();

        measurePanel.getMeasureList().clear();
        
        selectionPanel.clearPoints();

        infoLabel.setWindowLevel(wl);
        infoLabel.setWindowWidth(ww);
        infoLabel.updateText();

        // image, measure, selection infoをリペイント
        baseLayer.repaint();
    }

    // 移動・拡大を初期値に戻し、計測を消去。Window width/levelは変更しない
    private void resetImage2() {

        baseLayer.setLocation(0, 0);

        imagePanel.setLUT();
        zoomReset();

        measurePanel.getMeasureList().clear();
        
        selectionPanel.clearPoints();
        
        // image, measure, selection infoをリペイント
        baseLayer.repaint();
    }

    // 画像情報を表示するかのフラグ
    public void setShowInfo(boolean b) {
        infoLabel.setVisible(b);
    }
    
    // ガンマ係数を設定する
    public void setGamma(double d) {
        imagePanel.setGamma(d);
        imagePanel.repaint();
    }
    
    // クリップボードにコピーする
    public void copyImage() {
        
        // コピーする領域を設定する。未指定時は全部
        Rectangle r;
        if (selectionPanel.getStartPoint() != null) {
            r = selectionPanel.getSelectedRectangle();
        } else {
            int width = (int) (imagePanel.getImage().getWidth() * currentScale);
            int height = (int) (imagePanel.getImage().getHeight() * currentScale);
            r = new Rectangle(0, 0, width, height);
        }

        // BufferedImageにimageとmeasureを書き込む
        AffineTransform af = new AffineTransform();
        af.translate(-r.x, -r.y);
        BufferedImage image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = image.createGraphics();
        g2d.setTransform(af);
        imagePanel.paint(g2d);
        measurePanel.paint(g2d);
        g2d.dispose();

        // クリップボードにコピー
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        ImageTransferable it = new ImageTransferable(image);
        clip.setContents(it, null);
    }

    private void zoomReset() {
        pow = 0;
        setScale();
        currentScale = initialScale;
    }

    private void zoom(Point zoomPoint) {

        // baseLayerを拡大中心点を基準にした位置に移動させる
        Point loc = baseLayer.getLocation();
        double oldScale = currentScale;
        currentScale = initialScale * Math.pow(4, (double) pow / 10);
        double zoomX = zoomPoint.getX();
        double zoomY = zoomPoint.getY();
        double zoomRatio = currentScale / oldScale;

        AffineTransform tmp = new AffineTransform();
        tmp.setToTranslation(zoomX, zoomY);
        tmp.scale(zoomRatio, zoomRatio);
        tmp.translate(-zoomX, -zoomY);
        tmp.transform(loc, loc);
        baseLayer.setLocation(loc);

        BufferedImage image = imagePanel.getImage();
        if (image != null) {
            Dimension d = new Dimension();
            d.width = (int) (image.getWidth() * currentScale);
            d.height = (int) (image.getHeight() * currentScale);
            resizeBaseLayer(d);
        }
    }

    private void resizeBaseLayer(Dimension d) {
        baseLayer.setSize(d);
        measurePanel.setSize(d);
        selectionPanel.setSize(d);
        imagePanel.setSize(d);
    }

    private void setScale() {

        final BufferedImage image = imagePanel.getImage();
        if (image == null) {
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                Dimension d = getSize();
                // スケール計算
                double sx = (double) d.width / image.getWidth();
                double sy = (double) d.height / image.getHeight();
                initialScale = Math.min(sx, sy);
                currentScale = initialScale;
                resizeBaseLayer(d);
            }
        });
    }

    // 座標変換
    private void toAbsoluteBasePoint(Point p) {
        p.x = (int) ((p.x - baseLayer.getLocation().x) / currentScale);
        p.y = (int) ((p.y - baseLayer.getLocation().y) / currentScale);
    }

    // ドラッグで移動、マウスホイールで画像選択・拡大縮小を実装するMouseAdapter
    private class MyMouseAdapter extends MouseAdapter {

        private int mouseButton;
        private Point startP;
        private Point oldBaseP;

        private int windowLevel;
        private int windowWidth;

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {

            int count = e.getWheelRotation();
            if (viewer.getZoomBtn().isSelected()) {
                // 拡大縮小
                Point zoomPoint = e.getPoint();
                if (count > 0) {
                    if (pow < 10) {
                        pow++;
                        zoom(zoomPoint);
                    }
                } else {
                    if (pow > -10) {
                        pow--;
                        zoom(zoomPoint);
                    }
                }
            } else {
                // 前後イメージに移動
                if (count > 0) {
                    viewer.nextImage();
                } else {
                    viewer.prevImage();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

            mouseButton = e.getButton();
            startP = e.getPoint();
            oldBaseP = new Point(baseLayer.getLocation());
            windowLevel = imagePanel.getWindowLevel();
            windowWidth = imagePanel.getWindowWidth();

            switch (mouseButton) {
                case MouseEvent.BUTTON1:
                    if (viewer.getMeasureBtn().isSelected()) {
                        // 計測
                        Point sp = new Point(startP);
                        toAbsoluteBasePoint(sp);
                        PointPair pair = new PointPair(sp, sp);
                        measurePanel.getMeasureList().add(0, pair);
                        //measurePanel.repaint();
                    } else if (viewer.getDragBtn().isSelected()){
                        // 画像移動
                        Cursor cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                        e.getComponent().setCursor(cursor);
                    } else if (viewer.getSelectBtn().isSelected()){
                        // 矩形選択
                        Point sp = new Point(startP);
                        toAbsoluteBasePoint(sp);
                        selectionPanel.setStartPoint(sp);
                        selectionPanel.setEndPoint(sp);
                        //selectionPanel.repaint();
                    }
                    break;
                case MouseEvent.BUTTON2:
                    // 画像選択と画像拡大の切り替え
                    boolean b = viewer.getZoomBtn().isSelected();
                    viewer.getZoomBtn().setSelected(!b);
                    viewer.getMoveBtn().setSelected(b);
                    break;
                case MouseEvent.BUTTON3:
                    // windowLevel, windowWidth変更
                    Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                    e.getComponent().setCursor(cursor);
                    break;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {

            switch (mouseButton) {
                case MouseEvent.BUTTON1:
                    if (viewer.getMeasureBtn().isSelected()) {
                        // 計測
                        Point endP = e.getPoint();
                        toAbsoluteBasePoint(endP);
                        PointPair pair = measurePanel.getMeasureList().get(0);
                        pair.setEndPoint(endP);
                        measurePanel.repaint();
                    } else if (viewer.getDragBtn().isSelected()){
                        // 画像移動
                        Point p = new Point(
                                e.getX() - startP.x + oldBaseP.x,
                                e.getY() - startP.y + oldBaseP.y);
                        baseLayer.setLocation(p);
                    } else if (viewer.getSelectBtn().isSelected()){
                        // 矩形選択
                        Point endP = e.getPoint();
                        toAbsoluteBasePoint(endP);
                        selectionPanel.setEndPoint(endP);
                        selectionPanel.repaint();
                    }
                    break;
                case MouseEvent.BUTTON3:
                    // windowLevel, windowWidth変更
                    Point endP = e.getPoint();
                    int deltaX = endP.x - startP.x;
                    int deltaY = endP.y - startP.y;
                    imagePanel.setWindowLevel(windowLevel + deltaX / 2, windowWidth + deltaY / 2);
                    imagePanel.repaint();
                    infoLabel.setWindowLevel(imagePanel.getWindowLevel());
                    infoLabel.setWindowWidth(imagePanel.getWindowWidth());
                    infoLabel.updateText();
                    infoLabel.repaint();
                    break;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Cursor cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
            e.getComponent().setCursor(cursor);
        }

    }
}
