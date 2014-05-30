package open.dolphin.impl.pacsviewer;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import org.dcm4che2.data.DicomObject;

/**
 * 画像情報を表示するラベル
 *
 * @author masuda, Masuda Naika
 */
public class DicomInfoLabel extends JLabel {

    private static final String SPC = " ";
    private static final String BR = "<br>";

    private DicomImageInfo info;
    private int windowWidth;
    private int windowLevel;

    public DicomInfoLabel() {
        setForeground(Color.LIGHT_GRAY);
        setFont(new Font("Dialog", Font.PLAIN, 20));
        setOpaque(false);
    }
    
    public void setDicomObject(DicomObject object) {
        info = new DicomImageInfo(object);
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public void setWindowLevel(int windowLevel) {
        this.windowLevel = windowLevel;
    }

    public void updateText() {

        if (info == null) {
            setText("");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");

        sb.append(info.getInstitutionName()).append(BR);
        sb.append(info.getPatientID()).append(SPC);
        sb.append(info.getPatientName()).append(SPC);
        sb.append(info.getPatientAgeSex()).append(BR);

        sb.append(info.getStudyDate()).append(BR);
        sb.append("Se:").append(info.getSeriesNumber());
        sb.append(" Im:").append(info.getInstanceNumber()).append(BR);

        sb.append("W:").append(String.valueOf(windowWidth));
        sb.append(" L:").append(String.valueOf(windowLevel));
        sb.append("</body></html>");

        setText(sb.toString());

        setSize(getPreferredSize());
    }
}
