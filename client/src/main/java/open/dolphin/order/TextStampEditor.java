package open.dolphin.order;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import open.dolphin.client.GUIConst;
import open.dolphin.infomodel.*;

/**
 * TextStampEditor based on LTextStampEditor.java
 * 
 * @author pns
 * @author modified by masuda, Masuda Naika
 */
public class TextStampEditor extends AbstractStampEditor {

    private static final String EDITOR_NAME = "テキスト";
    private static final String lineSeparator = System.getProperty("line.separator");

    private JTextPane textPane;
    private JTextField titleField;
    private JPanel editorPanel;

    /**
     * Creates new TextStampEditor
     */
    public TextStampEditor(String entity) {
        this(entity, true);
    }

    public TextStampEditor(String entity, boolean mode) {
        super(entity, mode);
    }
    /**
     * Componentを初期化する。
     */
    @Override
    protected final void initComponents() {
        
        editorPanel = new JPanel();
        editorPanel.setPreferredSize(new Dimension(700, 600));
        textPane = new JTextPane();
        textPane.setMargin(new Insets(7,7,7,7));
        textPane.getDocument().addDocumentListener(new StateListener());
        titleField = new JTextField();
        titleField.getDocument().addDocumentListener(new StateListener());

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BorderLayout(10,0));
        JLabel label = new JLabel("スタンプ名：");
        titlePanel.add(label, BorderLayout.WEST);
        titlePanel.add(titleField, BorderLayout.CENTER);

        JScrollPane scroller = new JScrollPane(textPane);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        Border b = BorderFactory.createEtchedBorder();
        scroller.setBorder(BorderFactory.createTitledBorder(b, EDITOR_NAME));

        editorPanel.setLayout(new BorderLayout(0, GUIConst.DEFAULT_CMP_V_SPACE));
        editorPanel.add(titlePanel, BorderLayout.NORTH);
        editorPanel.add(scroller, BorderLayout.CENTER);
    }

    @Override
    public JPanel getView() {
        return editorPanel;
    }
    @Override
    protected String[] getColumnNames() {
        return null;
    }

    @Override
    protected String[] getColumnMethods() {
        return null;
    }

    @Override
    protected int[] getColumnWidth() {
        return null;
    }

    @Override
    protected String[] getSrColumnNames() {
        return null;
    }

    @Override
    protected String[] getSrColumnMethods() {
        return null;
    }

    @Override
    protected int[] getSrColumnWidth() {
        return null;
    }
    
    @Override
    protected void checkValidation() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                String title = titleField.getText();
                String text = textPane.getText();
                boolean setIsEmpty = (text == null || text.isEmpty());
                boolean setIsValid = title != null && !title.isEmpty() && !setIsEmpty;

                // 通知する
                if (boundSupport != null) {
                    boundSupport.firePropertyChange(EMPTY_DATA_PROP, !setIsEmpty, setIsEmpty);
                    boundSupport.firePropertyChange(VALIDA_DATA_PROP, !setIsValid, setIsValid);
                }
            }
        });
    }

    @Override
    protected void addSelectedTensu(TensuMaster tm) {
    }

    @Override
    protected void search(String text, boolean hitReturn) {
    }

    @Override
    protected void clear() {
        textPane.setText("");
        titleField.setText("");
     }

    /**
     * setValidModel で EditorSetPanel の propertyChange が fire される
     */
    private class StateListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            checkValidation();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            checkValidation();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            checkValidation();
        }
    }

    /**
     * 編集したテキストを返す
     * @return ModuleModel
     */
    @Override
    public TextStampModel getNewValue() {
        
        TextStampModel stamp = new TextStampModel();

        // Windows では改行コードを変換しないと位置がずれる!!
        stamp.setText(textPane.getText().replace(lineSeparator, "\n"));
        
        // 更新の場合はstampIdを引き継ぐ
        Object oldValue = getOldValue();
        if (oldValue instanceof TextStampModel) {
            TextStampModel old = (TextStampModel) oldValue;
            stamp.setStampId(old.getStampId());
        }
        // スタンプ名を設定する
        stamp.setStampName(titleField.getText().trim());
        
        return stamp;
    }

    /**
     * 編集するテキストを設定する
     * @param val ModuleModel
     */
    @Override
     public void setValue(Object objValue) {

        // 連続して編集される場合があるのでテーブル内容等をクリアする
        clear();
        setOldValue(objValue);
        if (!(objValue instanceof TextStampModel)) {
            return;
        }
        
        TextStampModel textStamp = (TextStampModel) objValue;
        setOldValue(textStamp);
        
        textPane.setText(textStamp.getText());
        titleField.setText(textStamp.getStampName());

        textPane.requestFocusInWindow();
    }
}