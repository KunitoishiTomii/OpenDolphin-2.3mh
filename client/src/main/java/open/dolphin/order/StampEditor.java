package open.dolphin.order;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import open.dolphin.client.Chart;
import open.dolphin.client.ClientContext;
import open.dolphin.helper.ComponentMemory;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;


/**
 * Stamp 編集用の外枠を提供する Dialog.
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class StampEditor implements PropertyChangeListener {

    private AbstractStampEditor editor;
    private JDialog dialog;


    public void editStamp(ModuleModel[] stamps, final PropertyChangeListener listener, Chart chart) {
        
        if (stamps.length == 0) {
            return;
        }

        String entity = stamps[0].getModuleInfoBean().getEntity();
        
        if (entity != null) {
            switch (entity) {
                case IInfoModel.ENTITY_MED_ORDER:
                    // RP
                    editor = new RpEditor(entity);
                    break;
                case IInfoModel.ENTITY_RADIOLOGY_ORDER:
                    // Injection
                    editor = new RadEditor(entity);
                    break;
                case IInfoModel.ENTITY_INJECTION_ORDER:
                    // Rad
                    editor = new InjectionEditor(entity);
                    break;
                case IInfoModel.ENTITY_INSTRACTION_CHARGE_ORDER:
                    // 指導
                    editor = new InstractionEditor(entity);
                    break;
                default:
                    // others, ex. physiology. most simple and basic editor
                    editor = new BaseEditor(entity);
                    break;
            }
        } else {
            // others, ex. physiology. most simple and basic editor
            //editor = new BaseEditor(entity);
        }
        
        editor.addPropertyChangeListener(AbstractStampEditor.VALUE_PROP, listener);
        editor.addPropertyChangeListener(AbstractStampEditor.EDIT_END_PROP, StampEditor.this);
        editor.setValue(stamps);
        
        // editorにChartを設定する
        editor.setContext(chart);
        
        dialog = new JDialog(chart.getFrame(), true);
        // アイコン設定
        ClientContext.setDolphinIcon(dialog);
        
        dialog.setTitle(editor.getOrderName());
        dialog.getContentPane().add(editor.getView(), BorderLayout.CENTER);
        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                editor.setFocusOnSearchTextFld();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                dialog.setVisible(false);
            }
        });

        dialog.pack();
        
        // エディタごとにウィンドウサイズを記憶させる
        ComponentMemory cm = new ComponentMemory(dialog, new Point(200, 100), dialog.getPreferredSize(), editor);

        cm.setToPreferenceBounds();

        dialog.setVisible(true);
    }

    public void editDiagnosis(RegisteredDiagnosisModel rd, PropertyChangeListener listener, Window lock) {
        
        editor = new DiseaseEditor(IInfoModel.ENTITY_DIAGNOSIS);
        editor.addPropertyChangeListener(AbstractStampEditor.VALUE_PROP, listener);
        editor.addPropertyChangeListener(AbstractStampEditor.EDIT_END_PROP, StampEditor.this);

        dialog = new JDialog((Frame) lock, true);
        // アイコン設定
        ClientContext.setDolphinIcon(dialog);

        dialog.setTitle(editor.getOrderName());
        dialog.getContentPane().add(editor.getView(), BorderLayout.CENTER);
        editor.setValue(rd);     // 編集する病名をセット
        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                editor.setFocusOnSearchTextFld();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                dialog.dispose();
                dialog.setVisible(false);
            }
        });

        dialog.pack();
        
        // エディタごとにウィンドウサイズを記憶させる
        ComponentMemory cm = new ComponentMemory(dialog, new Point(200, 100), dialog.getPreferredSize(), editor);
        cm.setToPreferenceBounds();

        dialog.setVisible(true);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (evt.getPropertyName().equals(AbstractStampEditor.EDIT_END_PROP)) {
            Boolean b = (Boolean) evt.getNewValue();
            if (b.booleanValue()) {
                dialog.dispose();
                dialog.setVisible(false);
            }
        }
    }
}