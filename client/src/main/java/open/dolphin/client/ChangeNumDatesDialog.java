package open.dolphin.client;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author Kazushi Minagawa. Digital Globe, Inc.
 */
public final class ChangeNumDatesDialog {

    private JButton changeBtn;
    private JButton cancelBtn;
    private ChangeNumDatesView view;
    private JDialog dialog;
    private PropertyChangeSupport boundSupport;
    private PropertyChangeListener listener;

    public ChangeNumDatesDialog(JFrame parent, PropertyChangeListener pcl) {

        // view
        view = new ChangeNumDatesView();
        String pattern = "^[1-9][0-9]*$";
        RegexConstrainedDocument numReg = new RegexConstrainedDocument(pattern);
        view.getNumDatesFld().setDocument(numReg);

        // OK button
        changeBtn = new JButton(new AbstractAction("変更"){

            @Override
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });
        changeBtn.setEnabled(false);

        // Cancel Button
        String buttonText =  (String)UIManager.get("OptionPane.cancelButtonText");
        cancelBtn = new JButton(new AbstractAction(buttonText){

            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        // Listener
        view.getNumDatesFld().getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent de) {
                checkInput();
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                checkInput();
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
            }
        });

        Object[] options = new Object[]{changeBtn, cancelBtn};

        JOptionPane jop = new JOptionPane(
                view,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                changeBtn);

        dialog = jop.createDialog(parent, ClientContext.getFrameTitle("処方日数変更"));
        dialog.addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowOpened(WindowEvent e) {
                view.getNumDatesFld().requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                doCancel();
            }
        });

        listener = pcl;
        boundSupport = new PropertyChangeSupport(this);
        boundSupport.addPropertyChangeListener(listener);
    }

    public void show() {
        dialog.setVisible(true);
    }

    private void doOk() {
        try {
            int number = Integer.parseInt(view.getNumDatesFld().getText().trim());
            boundSupport.firePropertyChange("newNumDates", -1, number);
            close();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    private void doCancel() {
        boundSupport.firePropertyChange("newNumDates", -1, 0);
        close();
    }

    private void close() {
        boundSupport.removePropertyChangeListener(listener);
        dialog.setVisible(false);
        dialog.dispose();
    }

    private void checkInput() {
        String test = view.getNumDatesFld().getText().trim();
        boolean ok = true;
        ok = ok && (!test.equals(""));
        changeBtn.setEnabled(ok);
    }
}
