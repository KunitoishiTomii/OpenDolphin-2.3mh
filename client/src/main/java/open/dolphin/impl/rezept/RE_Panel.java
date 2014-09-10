package open.dolphin.impl.rezept;

import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * JTabbedPaneに入れる、審査機関ごとの患者テーブルパネル
 * 
 * @author masuda, Masuda Naika
 */
public class RE_Panel extends JPanel {
    
    private JTable reTable;
    private JTextField countField;
    private JTextField tenField;
    
    public RE_Panel() {
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.add(new JLabel("件数"));
        countField = new JTextField(3);
        south.add(countField);
        south.add(new JLabel("総点数"));
        tenField = new JTextField(6);
        south.add(tenField);
        add(south, BorderLayout.SOUTH);
        reTable = new JTable();
        JScrollPane scrl = new JScrollPane(reTable);
        add(scrl, BorderLayout.CENTER);
    }
    
    public JTable getReTable() {
        return reTable;
    }
    public JTextField getCountField() {
        return countField;
    }
    public JTextField getTenField() {
        return tenField;
    }
}
