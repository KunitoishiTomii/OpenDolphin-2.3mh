package open.dolphin.impl.rezept;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import open.dolphin.client.ClientContext;
import open.dolphin.project.Project;

/**
 * RezeptView
 * 
 * @author masuda, Masuda Naika
 */
public class RezeptView extends JPanel {
    
    private static final String ICON_FORWARD = "icon_arrow_right_small";
    private static final String ICON_BACK = "icon_arrow_left_small";
    private static final ImageIcon PERV_ICON = ClientContext.getImageIconAlias(ICON_BACK);
    private static final ImageIcon NEXT_ICON = ClientContext.getImageIconAlias(ICON_FORWARD);
    private static final ImageIcon REFRESH_ICON = ClientContext.getImageIconAlias("icon_refresh_small");
    
    private JSplitPane splitPane;
    private JTabbedPane tabbedPane;
    private JButton importBtn;
    private JButton checkBtn;
    private JButton prevBtn;
    private JButton nextBtn;
    private JComboBox drCombo;
    private JButton printBtn;
    
    private JTextField insTypeField;
    private JTextField pubIns1Field;
    private JTextField pubIns1NumField;
    private JTextField pubIns2Field;
    private JTextField pubIns2NumField;
    private JTextField pubIns3Field;
    private JTextField pubIns3NumField;
    private JTextField pubIns4Field;
    private JTextField pubIns4NumField;
    private JTextField insField;
    private JTextField insSymbolField;
    private JTextField insNumberField;
    private JButton refreshBtn;
    
    private JTextField ptIdField;
    private JTextField billYmField;
    private JTextField ptNameField;
    private JTextField ptBirthdayField;
    private JTextField ptSexField;
    private JTextField ptAgeField;
    
    private JSplitPane centerSplit;
    private JTable diagTable;
    private JTextField diagCountField;
    private JTable itemTable;
    private JTextField numDayField;
    private JTextField tenField;
    
    private JTextArea commentArea;
    private JTable infoTable;
    
    private static final int PT_PANEL_WIDTH = 230;
    private static final int DIAG_PANEL_WIDTH = 400;
    private static final String PROP_PT_PANEL_WIDTH = "rezeptViewPtPanelWidth";
    private static final String PROP_DIAG_PANEL_WIDTH = "rezeptViewDiagPanelWidth";
    private static final int DIVIDER_SIZE = 2;
    
    
    public RezeptView() {
        initComponents();
    }

    private void initComponents() {
        
        setLayout(new BorderLayout());
        
        int ptPanelWidth = Project.getInt(PROP_PT_PANEL_WIDTH, PT_PANEL_WIDTH);
        splitPane = new JSplitPane();
        splitPane.setDividerSize(DIVIDER_SIZE);
        splitPane.setDividerLocation(ptPanelWidth);
        add(splitPane, BorderLayout.CENTER);
        
        // left panel
        JPanel leftPanel = createYBoxPanel();
        splitPane.setLeftComponent(leftPanel);
        
        JPanel btnPanel = createXBoxPanel();
        importBtn = new JButton("取込");
        importBtn.setToolTipText("ORCAからレセ電データを読み込みます");
        checkBtn = new JButton("点検");
        checkBtn.setToolTipText("レセプト点検します");
        btnPanel.add(importBtn);
        btnPanel.add(checkBtn);
        prevBtn = new JButton(PERV_ICON);
        nextBtn = new JButton(NEXT_ICON);
        btnPanel.add(prevBtn);
        btnPanel.add(nextBtn);
        leftPanel.add(btnPanel);
        JPanel filterPanel = createXBoxPanel();
        filterPanel.add(new JLabel("担当医："));
        drCombo = new JComboBox();
        filterPanel.add(drCombo);
        printBtn = new JButton("印刷");
        filterPanel.add(printBtn);
        leftPanel.add(filterPanel);
        leftPanel.add(Box.createVerticalStrut(5));
        
        tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
        leftPanel.add(tabbedPane);
        
        // rightPanel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        splitPane.setRightComponent(rightPanel);

        // north
        JPanel north = createXBoxPanel();
        north.setBorder(BorderFactory.createEtchedBorder());
        
        // pt info
        JPanel p1 = createYBoxPanel();
        JPanel p11 =createXBoxPanel();
        p11.add(new JLabel("ID"));
        ptIdField = createTextField(8);
        p11.add(ptIdField);
        p11.add(new JLabel("診療年月"));
        billYmField = createTextField(6);
        p11.add(billYmField);
        p1.add(p11);
        JPanel p12 =createXBoxPanel();
        p12.add(new JLabel("氏名"));
        ptNameField = createTextField(10);
        p12.add(ptNameField);
        p1.add(p12);
        JPanel p13 = createXBoxPanel();
        p13.add(new JLabel("性別"));
        ptSexField = createTextField(2);
        p13.add(ptSexField);
        p13.add(new JLabel("年齢"));
        ptAgeField = createTextField(2);
        p13.add(ptAgeField);
        p1.add(p13);
        JPanel p14 = createXBoxPanel();
        p14.add(new JLabel("生年月日"));
        ptBirthdayField = createTextField(8);
        p14.add(ptBirthdayField);
        p1.add(p14);
        north.add(p1);
        
        // insurance
        JPanel p2 = createYBoxPanel();
        JPanel p21 = createXBoxPanel();
        p21.add(new JLabel("公費1"));
        pubIns1Field = createTextField(8);
        p21.add(pubIns1Field);
        p2.add(p21);
        JPanel p22 = createXBoxPanel();
        p22.add(new JLabel("公費2"));
        pubIns2Field = createTextField(8);
        p22.add(pubIns2Field);
        p2.add(p22);
        JPanel p23 = createXBoxPanel();
        p23.add(new JLabel("公費3"));
        pubIns3Field = createTextField(8);
        p23.add(pubIns3Field);
        p2.add(p23);
        JPanel p24 = createXBoxPanel();
        p24.add(new JLabel("公費4"));
        pubIns4Field = createTextField(8);
        p24.add(pubIns4Field);
        p2.add(p24);
        north.add(p2);
        
        JPanel p3 = createYBoxPanel();
        JPanel p31 = createXBoxPanel();
        p31.add(new JLabel("受1"));
        pubIns1NumField = createTextField(8);
        p31.add(pubIns1NumField);
        p3.add(p31);
        JPanel p32 = createXBoxPanel();
        p32.add(new JLabel("受2"));
        pubIns2NumField = createTextField(8);
        p32.add(pubIns2NumField);
        p3.add(p32);
        JPanel p33 = createXBoxPanel();
        p33.add(new JLabel("受3"));
        pubIns3NumField = createTextField(8);
        p33.add(pubIns3NumField);
        p3.add(p33);
        JPanel p34 = createXBoxPanel();
        p34.add(new JLabel("受4"));
        pubIns4NumField = createTextField(8);
        p34.add(pubIns4NumField);
        p3.add(p34);
        north.add(p3);
        
        JPanel p4 = createYBoxPanel();
        JTextField facilityFld = createTextField(20);
        facilityFld.setText(Project.getUserModel().getFacilityModel().getFacilityName());
        p4.add(facilityFld);
        insTypeField= createTextField(20);
        p4.add(insTypeField);
        JPanel p42 = createXBoxPanel();
        p42.add(new JLabel("保険"));
        insField = createTextField(20);
        p42.add(insField);
        p4.add(p42);
        JPanel p43 = createXBoxPanel();
        insSymbolField = createTextField(10);
        p43.add(insSymbolField);
        insNumberField = createTextField(12);
        p43.add(insNumberField);
        refreshBtn = new JButton(REFRESH_ICON);
        refreshBtn.setToolTipText("レセを再評価します");
        p43.add(refreshBtn);
        p4.add(p43);
        north.add(p4);
        rightPanel.add(north, BorderLayout.NORTH);
        
        // center
        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setDividerSize(DIVIDER_SIZE);
        int diagPanelWidth = Project.getInt(PROP_DIAG_PANEL_WIDTH, DIAG_PANEL_WIDTH);
        centerSplit.setDividerLocation(diagPanelWidth);
        rightPanel.add(centerSplit, BorderLayout.CENTER);

        JPanel centerLeft = createYBoxPanel();
        diagTable = new JTable();
        diagTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane diagScrl = new JScrollPane(diagTable);
        diagScrl.setBorder(BorderFactory.createTitledBorder("傷病名"));
        centerLeft.add(diagScrl);
        commentArea = createTextArea();
        JScrollPane commentScrl = new JScrollPane(commentArea);
        commentScrl.setBorder(BorderFactory.createTitledBorder("コメント"));
        centerLeft.add(commentScrl);
        commentScrl.setPreferredSize(new Dimension(DIAG_PANEL_WIDTH, 200));
        JPanel countPanel = createXBoxPanel();
        countPanel.add(new JLabel("病名数"));
        diagCountField = new JTextField(3);
        countPanel.add(diagCountField);
        countPanel.add(new JLabel("診療実日数"));
        numDayField = createTextField(2);
        countPanel.add(numDayField);
        countPanel.add(new JLabel("点数"));
        tenField = createTextField(5);
        countPanel.add(tenField);
        int height = countPanel.getPreferredSize().height;
        countPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        centerLeft.add(countPanel);
        centerSplit.setLeftComponent(centerLeft);
        
        itemTable = new JTable();
        itemTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane itemScrl = new JScrollPane(itemTable);
        itemScrl.setBorder(BorderFactory.createTitledBorder("診療行為"));
        centerSplit.setRightComponent(itemScrl);

        // south
        JPanel south = createYBoxPanel();
        infoTable = new JTable();
        infoTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane infoPane = new JScrollPane(infoTable);
        infoPane.setBorder(BorderFactory.createTitledBorder("インフォ"));
        infoPane.setPreferredSize(new Dimension(400, 200));
        south.add(infoPane);
        rightPanel.add(south, BorderLayout.SOUTH);
    }

    private JPanel createYBoxPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
    private JPanel createXBoxPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        return panel;
    }
    
    private JTextField createTextField(int len) {
        JTextField jf = new JTextField(len);
        jf.setEditable(false);
        jf.setFocusable(false);
        return jf;
    }
    
    private JTextArea createTextArea() {
        JTextArea ja = new JTextArea();
        ja.setEditable(false);
        ja.setLineWrap(true);
        ja.setBorder(BorderFactory.createEtchedBorder());
        return ja;
    }
    
    public void saveDimension() {
        int ptPanelWidth = splitPane.getDividerLocation();
        Project.setInt(PROP_PT_PANEL_WIDTH, ptPanelWidth);
        int diagPanelWidth = centerSplit.getDividerLocation();
        Project.setInt(PROP_DIAG_PANEL_WIDTH, diagPanelWidth);
    }
    
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    public JButton getImportBtn() {
        return importBtn;
    }
    public JButton getCheckBtn() {
        return checkBtn;
    }
    public JButton getPrevBtn() {
        return prevBtn;
    }
    public JButton getNextBtn() {
        return nextBtn;
    }
    public JTextField getInsTypeField() {
        return insTypeField;
    }
    public JTextField getPubIns1Field() {
        return pubIns1Field;
    }
    public JTextField getPubIns1NumField() {
        return pubIns1NumField;
    }
    public JTextField getPubIns2Field() {
        return pubIns2Field;
    }
    public JTextField getPubIns2NumField() {
        return pubIns2NumField;
    }
    public JTextField getPubIns3Field() {
        return pubIns3Field;
    }
    public JTextField getPubIns3NumField() {
        return pubIns3NumField;
    }
    public JTextField getPubIns4Field() {
        return pubIns4Field;
    }
    public JTextField getPubIns4NumField() {
        return pubIns4NumField;
    }
    public JTextField getInsField() {
        return insField;
    }
    public JTextField getInsSymbolField() {
        return insSymbolField;
    }
    public JTextField getInsNumberField() {
        return insNumberField;
    }
    public JTextField getPtNameField() {
        return ptNameField;
    }
    public JTextField getPtBirthdayField() {
        return ptBirthdayField;
    }
    public JTextField getPtSexField() {
        return ptSexField;
    }
    public JTextField getPtAgeField() {
        return ptAgeField;
    }
    public JTable getDiagTable() {
        return diagTable;
    }
    public JTable getItemTable() {
        return itemTable;
    }
    public JTextArea getCommentArea() {
        return commentArea;
    }
    public JTable getInfoTable() {
        return infoTable;
    }
    public JTextField getNumDayField() {
        return numDayField;
    }
    public JTextField getTenField() {
        return tenField;
    }
    public JTextField getDiagCountField() {
        return diagCountField;
    }
    public JTextField getPtIdField() {
        return ptIdField;
    }
    public JTextField getBillYmField() {
        return billYmField;
    }
    public JButton getRefreshBtn() {
        return refreshBtn;
    }
    public JComboBox getDrCombo() {
        return drCombo;
    }
    public JButton getPrintBtn() {
        return printBtn;
    }
}
