package open.dolphin.client;

import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * １号カルテパネル
 *
 * @author masuda, Masuda Naika
 */
public final class KartePanel1 extends KartePanel {

    private JTextPane soaTextPane;

    public KartePanel1(boolean editor) {
        super();
        initComponents(editor);
    }

    @Override
    protected void initComponents(boolean editor) {

        soaTextPane = createTextPane();
        
        if (editor) {
            JScrollPane scroll = new JScrollPane(soaTextPane);
            scroll.setBorder(null);
            add(scroll);
        } else {
            add(soaTextPane);
        }
    }

    @Override
    public JTextPane getSoaTextPane() {
        return soaTextPane;
    }

    @Override
    public JTextPane getPTextPane() {
        return null;
    }
    
    @Override
    public boolean isSinglePane() {
        return true;
    }
/*
    // KarteDocumentViewerのBoxLayoutがうまくやってくれるように
    @Override
    public Dimension getPreferredSize() {

        int w = getContainerWidth();
        int h = getTimeStampPanel().getPreferredSize().height;
        h -= 15;    // some adjustment
        h += soaTextPane.getPreferredSize().height;

        return new Dimension(w, h);
    }
*/
}
