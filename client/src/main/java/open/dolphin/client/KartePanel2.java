package open.dolphin.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * ２号カルテパネル
 * @author masuda, Masuda Naika
 */
public final class KartePanel2 extends KartePanel {

    private JTextPane pTextPane;
    private JTextPane soaTextPane;

    public KartePanel2(boolean editor) {
        super();
        initComponents(editor);
    }

    @Override
    protected void initComponents(boolean editor) {

        soaTextPane = createTextPane();
        pTextPane = createTextPane();

        if (editor) {
            ScrollableJPanel panel = new ScrollableJPanel();
            panel.setFixedWidth(true);
            panel.setLayout(new GridLayout(rows, cols, hgap, vgap));
            panel.add(soaTextPane);
            panel.add(pTextPane);
            JScrollPane scroll = new JScrollPane(panel);
            add(scroll, BorderLayout.CENTER);
/*
            JScrollPane soaScroll = new JScrollPane(soaTextPane);
            soaScroll.setBorder(null);
            panel.add(soaScroll);
            JScrollPane pScroll = new JScrollPane(pTextPane);
            pScroll.setBorder(null);
            panel.add(pScroll);
*/
        } else {
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(rows, cols, hgap, vgap));
            panel.add(soaTextPane);
            panel.add(pTextPane);
            add(panel, BorderLayout.CENTER);
        }
    }
    
    @Override
    public JTextPane getSoaTextPane() {
        return soaTextPane;
    }

    @Override
    public JTextPane getPTextPane() {
        return pTextPane;
    }
    
    @Override
    public boolean isSinglePane() {
        return false;
    }
/*
    // KarteDocumentViewerのBoxLayoutがうまくやってくれるように
    @Override
    public Dimension getPreferredSize() {

        int w = getContainerWidth();
        int h = getTimeStampPanel().getPreferredSize().height;
        h -= 15;    // some adjustment

        int hsoa = soaTextPane.getPreferredSize().height;
        int hp = pTextPane != null
                ? pTextPane.getPreferredSize().height : 0;
        h += Math.max(hp, hsoa);

        return new Dimension(w, h);
    }
*/
}
