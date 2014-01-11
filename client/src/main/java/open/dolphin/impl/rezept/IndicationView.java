package open.dolphin.impl.rezept;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * IndicationView
 * 
 * @author masuda, Masuda Naika
 */
public class IndicationView extends JPanel {
    
    private JTextField srycdFld;
    private JTextField nameFld;
    private JTable diagTable;
    private JButton orcaBtn;
    private JButton pmdaBtn;
    private JButton copyBtn;
    private JTable indicationTable;
    private JCheckBox notCheck;
    private JTextField keywordFld;
    private JButton deleteBtn;
    private JButton clearBtn;
    private JButton addBtn;
    private JButton closeBtn;
    private JCheckBox admissionChk;
    private JCheckBox outPatientChk;
    
    public IndicationView() {
        initComponents();
    }
    
    private void initComponents() {
        
        setLayout(new BorderLayout());
        
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
        north.add(new JLabel("コード"));
        srycdFld = createTextField(10);
        srycdFld.setEditable(false);
        north.add(srycdFld);
        north.add(new JLabel("名称"));
        nameFld = createTextField(20);
        nameFld.setEditable(false);
        north.add(nameFld);
        orcaBtn = new JButton("ORCA");
        north.add(orcaBtn);
        pmdaBtn = new JButton("PMDA");
        north.add(pmdaBtn);
        copyBtn = new JButton("COPY");
        north.add(copyBtn);
        add(north, BorderLayout.NORTH);
        
        JPanel center = new JPanel();
        center.setLayout(new GridLayout(1, 2));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        center.add(left);
        diagTable = new JTable();
        diagTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrl1 = new JScrollPane(diagTable);
        scrl1.setBorder(BorderFactory.createTitledBorder("傷病名"));
        left.add(scrl1);
        JPanel shinsa = new JPanel();
        shinsa.setLayout(new BoxLayout(shinsa, BoxLayout.X_AXIS));
        shinsa.add(new JLabel("審査："));
        admissionChk = new JCheckBox("入院");
        shinsa.add(admissionChk);
        outPatientChk = new JCheckBox("入院外");
        shinsa.add(outPatientChk);
        left.add(shinsa);
        
        JPanel right = new JPanel();
        center.add(right);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        indicationTable = new JTable();
        indicationTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrl2 = new JScrollPane(indicationTable);
        scrl2.setBorder(BorderFactory.createTitledBorder("キーワード"));
        right.add(scrl2);
        JPanel kwPanel = new JPanel();
        kwPanel.setLayout(new BoxLayout(kwPanel, BoxLayout.X_AXIS));
        notCheck = new JCheckBox("NOT");
        kwPanel.add(notCheck);
        keywordFld = new JTextField(20);
        int height = keywordFld.getPreferredSize().height;
        keywordFld.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        kwPanel.add(keywordFld);
        right.add(kwPanel);
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
        deleteBtn = new JButton("削除");
        btnPanel.add(deleteBtn);
        clearBtn = new JButton("クリア");
        btnPanel.add(clearBtn);
        addBtn = new JButton("追加");
        btnPanel.add(addBtn);
        closeBtn = new JButton("閉じる");
        btnPanel.add(closeBtn);
        right.add(btnPanel);
        add(center, BorderLayout.CENTER);
    }
    
    private JTextField createTextField(int len) {
        JTextField jf = new JTextField(len);
        jf.setEditable(false);
        jf.setFocusable(false);
        return jf;
    }
    
    public JTextField getSrycdFld() {
        return srycdFld;
    }
    public JTextField getNameFld() {
        return nameFld;
    }
    public JTable getDiagTable() {
        return diagTable;
    }
    public JButton getOrcaButton() {
        return orcaBtn;
    }
    public JButton getPmdaButton() {
        return pmdaBtn;
    }
    public JButton getCopyButton() {
        return copyBtn;
    }
    public JTable getIndicationTable() {
        return indicationTable;
    }
    public JTextField getKeywordFld() {
        return keywordFld;
    }
    public JCheckBox getNotCheck() {
        return notCheck;
    }
    public JButton getDeleteButton() {
        return deleteBtn;
    }
    public JButton getClearButton() {
        return clearBtn;
    }
    public JButton getAddButton() {
        return addBtn;
    }
    public JButton getCloseButton() {
        return closeBtn;
    }
    public JCheckBox getAdmissionChk() {
        return admissionChk;
    }
    public JCheckBox getOutPatientChk() {
        return outPatientChk;
    }
}
