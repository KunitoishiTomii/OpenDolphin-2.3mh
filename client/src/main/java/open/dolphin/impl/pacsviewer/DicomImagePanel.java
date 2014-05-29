package open.dolphin.impl.pacsviewer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import javax.swing.JPanel;

/**
 * 画像を表示するパネル
 *
 * @author masuda, Masuda Naika
 */
public class DicomImagePanel extends JPanel {
    
    private static final int MAX_DEPTH = 256;
    private static final int MAX_WW = MAX_DEPTH - 1;
    private static final int MIN_WW = 1;
    private static final int MAX_WL = MAX_DEPTH - 1;
    private static final int MIN_WL = -MAX_WL;
    public static final int DEFALUT_WW = MAX_DEPTH - 1;
    public static final int DEFAULT_WL = MAX_DEPTH / 2 - 1;
    private int windowWidth = DEFALUT_WW;
    private int windowLevel = DEFAULT_WL;
    
    private final DicomViewerRootPane parent;
    private final byte[] lut;
    
    private BufferedImage image;
    private LookupOp lookupOp;
    private double gamma;
    
    public DicomImagePanel(DicomViewerRootPane parent) {
        this.parent = parent;
        lut = new byte[MAX_DEPTH];
    }
    
    @Override
    public void paintComponent(Graphics g) {
        
        AffineTransform af = new AffineTransform();
        double scale = parent.getCurrentScale();
        af.scale(scale, scale);
        
        if (image != null && lookupOp != null) {
            Graphics2D g2D = (Graphics2D) g;
            g2D.drawImage(lookupOp.filter(image, null), af, null);
        }
    }
    
    public void setImage(BufferedImage image) {
        this.image = image;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public void setGamma(double gamma) {
        this.gamma = gamma;
        setLUT();
        repaint();
    }
    
    public double getGamma() {
        return gamma;
    }
    
    public int getWindowLevel() {
        return windowLevel;
    }
    
    public int getWindowWidth() {
        return windowWidth;
    }
    
    public void setWindowLevel(int level, int width) {
        
        if (level > MAX_WL) {
            level = MAX_WL;
        } else if (level < MIN_WL) {
            level = MIN_WL;
        }
        windowLevel = level;
        if (width > MAX_WW) {
            width = MAX_WW;
        } else if (width < MIN_WW) {
            width = MIN_WW;
        }
        windowWidth = width;
        setLUT();
        repaint();
    }

    // Window Width/Levelとガンマ値に応じたLUTを作成する。
    public void setLUT() {
        
        for (int i = 0; i < MAX_DEPTH; ++i) {
            double d1;
            if (i <= windowLevel - 0.5 - (windowWidth - 1) / 2) {
                d1 = MIN_WW;
            } else if (i > windowLevel - 0.5 + (windowWidth - 1) / 2) {
                d1 = MAX_WW;
            } else {
                d1 = ((i - (windowLevel - 0.5)) / (windowWidth - 1) + 0.5) * (MAX_WW - MIN_WW) + MIN_WW;
            }
            double d2 = (MAX_DEPTH - 1) * Math.pow(d1 / (MAX_DEPTH - 1), 1 / gamma);
            lut[i] = (byte) d2;
        }
        lookupOp = new LookupOp(new ByteLookupTable(0, lut), null);
    }
    
}
