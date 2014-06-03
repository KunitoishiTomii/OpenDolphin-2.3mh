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
    private boolean inverted;
    private boolean monochrome;
    
    // ウソGSDF
    private static final int[] GRAY_LUT = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1,
        2, 2, 2, 3, 3, 4, 5, 5, 6, 7, 8, 9, 10, 11, 12, 14,
        15, 16, 17, 19, 20, 21, 23, 24, 25, 27, 28, 29, 31, 32, 34, 36,
        37, 39, 40, 42, 43, 45, 46, 48, 49, 51, 52, 54, 55, 57, 58, 60,
        62, 63, 65, 66, 68, 69, 71, 72, 73, 75, 76, 78, 79, 81, 82, 84,
        85, 87, 88, 90, 91, 93, 94, 95, 97, 98, 99, 101, 102, 103, 105, 106,
        108, 109, 111, 112, 113, 115, 116, 117, 119, 120, 121, 123, 124, 125, 126, 128,
        129, 131, 132, 133, 134, 136, 137, 138, 139, 140, 142, 143, 144, 145, 146, 148,
        149, 150, 151, 153, 154, 155, 156, 157, 158, 159, 161, 162, 163, 164, 165, 166,
        168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 184,
        185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 195, 197, 198, 198, 200,
        201, 202, 203, 204, 205, 206, 206, 208, 208, 209, 211, 211, 212, 213, 214, 215,
        216, 217, 218, 218, 220, 220, 221, 222, 223, 224, 225, 225, 227, 227, 228, 229,
        230, 231, 232, 233, 234, 234, 235, 236, 237, 238, 238, 239, 240, 241, 242, 242,
        243, 244, 245, 246, 247, 247, 248, 249, 250, 250, 251, 252, 252, 253, 254, 255
    };

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
    
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }
    
    public boolean isInverted() {
        return inverted;
    }
    
    public void setMonochrome(boolean monochrome) {
        this.monochrome = monochrome;
    }
    
    public boolean isMonochrome() {
        return monochrome;
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

        int factor = imageDepth / DEPTH;
        double ww = windowWidth;
        double wc = windowCenter;
        double y;

        for (int i = 0; i < DEPTH; ++i) {
            
            double x;
            if (monochrome) {
                x = !inverted ? GRAY_LUT[i] : DEPTH - 1 - GRAY_LUT[i];
            } else {
                x = !inverted ? i : DEPTH - 1 - i;
            }
            x *= factor;
            
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

        lookupOp = new LookupOp(new ByteLookupTable(0, lut), null);
    }

}
