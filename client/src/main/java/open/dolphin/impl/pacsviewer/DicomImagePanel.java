package open.dolphin.impl.pacsviewer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.io.IOException;
import javax.swing.JPanel;
import open.dolphin.util.ImageTool;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * 画像を表示するパネル
 *
 * @author masuda, Masuda Naika
 */
public class DicomImagePanel extends JPanel {
    
    private static final int DEPTH = 256;
    private static final int Y_MAX = DEPTH - 1;
    private static final int Y_MIN = 0;
    
    private int imageDepth;
    private int defaultWindowWidth;
    private int defaultWindowCenter;
    
    private int windowWidth;
    private int windowCenter;
    
    private int maxWidth;
    private final int minWidth = 1;
    private int maxCenter;
    private int minCenter;

    private final DicomViewerRootPane parent;
    private final byte[] lut;
    
    private BufferedImage image;
    private LookupOp lookupOp;
    private double gamma;
    
    public DicomImagePanel(DicomViewerRootPane parent) {
        this.parent = parent;
        lut = new byte[DEPTH];
        setOpaque(false);
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
    
    public void setDicomObject(DicomObject object) throws IOException {
        
        image = ImageTool.getDicomImage(object);
        int bitsStored = object.getInt(Tag.BitsStored);
        imageDepth = 1 << bitsStored;
        maxWidth = imageDepth - 1;
        maxCenter = imageDepth + imageDepth / 2 - 1;
        minCenter = -imageDepth / 2 + 1;

        String wl = object.getString(Tag.WindowCenter);
        String ww = object.getString(Tag.WindowWidth);
        try {
            windowCenter = Integer.parseInt(wl);
            windowWidth = Integer.parseInt(ww);
        } catch (NullPointerException | NumberFormatException ex) {
            windowCenter = maxWidth / 2;
            windowWidth = maxWidth;
        }
        defaultWindowCenter = windowCenter;
        defaultWindowWidth = windowWidth;
    }
    
    public void restoreDefault() {
        windowCenter = defaultWindowCenter;
        windowWidth = defaultWindowWidth;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public void setGamma(double gamma) {
        this.gamma = gamma;
        setLUT();
    }
    
    public double getGamma() {
        return gamma;
    }

    public int getWindowCenter() {
        return windowCenter;
    }

    public int getWindowWidth() {
        return windowWidth;
    }
    
    public void setWindowWidthAndCenter(int wWidth, int wCenter) {
        
        if (wCenter > maxCenter) {
            wCenter = maxCenter;
        } else if (wCenter < minCenter) {
            wCenter = minCenter;
        }
        windowCenter = wCenter;
        if (wWidth > maxWidth) {
            wWidth = maxWidth;
        } else if (wWidth < minWidth) {
            wWidth = minWidth;
        }
        windowWidth = wWidth;
        setLUT();
    }

    // Window Width/Levelとガンマ値に応じたLUTを作成する。
    public void setLUT() {
        
        if (imageDepth != 0) {
            
            int factor = imageDepth / DEPTH;
            double ww = windowWidth;
            double wc = windowCenter;
            double y;
            
            for (int i = 0; i < DEPTH; ++i) {
                double x = i * factor;
                if (x <= wc - 0.5 - (ww - 1) / 2) {
                    y = Y_MIN;
                } else if (x > wc - 0.5 + (ww - 1) / 2) {
                    y = Y_MAX;
                } else {
                    y = ((x - (wc - 0.5)) / (ww - 1) + 0.5) * (Y_MAX - Y_MIN) + Y_MIN;
                }
                double yg = (DEPTH - 1) * Math.pow(y / (DEPTH - 1), 1 / gamma);
                lut[i] = (byte) yg;
            }
        }
        lookupOp = new LookupOp(new ByteLookupTable(0, lut), null);
    }

}
