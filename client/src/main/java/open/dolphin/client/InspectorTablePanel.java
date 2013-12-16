package open.dolphin.client;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * InspectorTablePanel
 *
 * @author masuda, Masuda Naika
 */
public class InspectorTablePanel extends JPanel {

    private final JScrollPane scroll;
    private final JTable table;

    public InspectorTablePanel() {
        
        table = new JTable();
        scroll = new JScrollPane(table);
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
    }

    public JTable getTable() {
        return table;
    }
}
