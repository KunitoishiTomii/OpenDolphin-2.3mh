package open.dolphin.impl.rezept;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * AllIndicationView
 * 
 * @author masuda, Masua Naika
 */
public class AllIndicationView extends JPanel {
    
    private JTextField srycdFld;
    private JTextField nameFld;
    private JTable modelTable;
    private JButton orcaBtn;
    private JButton pmdaBtn;
    private JButton copyBtn;
    private JTable indicationTable;
    private JCheckBox notCheck;
    private JTextField keywordFld;
    private JTextField commentFld;
    private JButton deleteBtn;
    private JButton clearBtn;
    private JButton addBtn;
    private JButton closeBtn;
    private JButton saveBtn;
    private JCheckBox inclusiveChk;
    private JCheckBox admissionChk;
    private JCheckBox outPatientChk;
    
    public AllIndicationView() {
        initComponents();
    }
    
    private void initComponents() {
        
        setLayout(new BorderLayout());
        
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
        north.add(new JLabel("コード"));
        srycdFld = createTextField(10);
        north.add(srycdFld);
        north.add(new JLabel("名称"));
        nameFld = createTextField(20);
        north.add(nameFld);
        orcaBtn = new JButton("ORCA");
        orcaBtn.setToolTipText("ORCAの適応病名を取り込みます");
        north.add(orcaBtn);
        pmdaBtn = new JButton("PMDA");
        pmdaBtn.setToolTipText("医療用医薬品の添付文書情報検索サイトを開きます");
        north.add(pmdaBtn);
        copyBtn = new JButton("COPY");
        copyBtn.setToolTipText("名称をクリップボードにコピーします");
        north.add(copyBtn);
        add(north, BorderLayout.NORTH);
        
        JPanel center = new JPanel();
        center.setLayout(new GridLayout(1, 2));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        center.add(left);
        modelTable = new JTable();
        modelTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrl1 = new JScrollPane(modelTable);
        scrl1.setBorder(BorderFactory.createTitledBorder("項目"));
        left.add(scrl1);
        
        JPanel right = new JPanel();
        center.add(right);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        indicationTable = new JTable();
        indicationTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrl2 = new JScrollPane(indicationTable);
        scrl2.setBorder(BorderFactory.createTitledBorder("キーワード"));
        right.add(scrl2);
        JPanel commentPanel = new JPanel();
        commentPanel.setLayout(new BoxLayout(commentPanel, BoxLayout.X_AXIS));
        commentPanel.add(new JLabel("81コメント"));
        commentFld = new JTextField(20);
        commentPanel.add(commentFld);
        right.add(commentPanel);
        JPanel shinsa = new JPanel();
        shinsa.setLayout(new BoxLayout(shinsa, BoxLayout.Y_AXIS));
        JPanel shinsa1 = new JPanel();
        shinsa1.setLayout(new BoxLayout(shinsa1, BoxLayout.X_AXIS));
        shinsa1.add(new JLabel("審査："));
        admissionChk = new JCheckBox("入院");
        shinsa1.add(admissionChk);
        outPatientChk = new JCheckBox("入院外");
        shinsa1.add(outPatientChk);
        shinsa1.add(Box.createHorizontalGlue());
        inclusiveChk = new JCheckBox("検査包括10項目以上は対象外");
        shinsa1.add(inclusiveChk);
        shinsa.add(shinsa1);
        right.add(shinsa);
        JPanel kwPanel = new JPanel();
        kwPanel.setLayout(new BoxLayout(kwPanel, BoxLayout.X_AXIS));
        notCheck = new JCheckBox("禁忌");
        notCheck.setToolTipText("診療行為の禁忌病名を登録する場合にチェックします");
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
        btnPanel.add(Box.createHorizontalStrut(10));
        saveBtn = new JButton("登録");
        btnPanel.add(saveBtn);
        btnPanel.add(Box.createHorizontalStrut(10));
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
    public JTable getModelTable() {
        return modelTable;
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
    public JCheckBox getInclusiveChk() {
        return inclusiveChk;
    }
    public JTextField getCommentFld() {
        return commentFld;
    }
    public JButton getSaveButton() {
        return saveBtn;
    }
}
