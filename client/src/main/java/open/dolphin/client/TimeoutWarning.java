package open.dolphin.client;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 *
 * @author Kazushi Minagawa.
 */
public class TimeoutWarning {

    private final JComponent parent;
    private String title;
    private final String message;

    public TimeoutWarning(JComponent parent, String title, String message) {
        this.parent = parent;
        this.message = message;
    }

    public void start() {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message);
        }
        sb.append(ClientContext.getString("task.timeoutMsg1"));
        sb.append("\n");
        sb.append(ClientContext.getString("task.timeoutMsg2"));
        JOptionPane.showMessageDialog(parent,
                sb.toString(),
                ClientContext.getFrameTitle(title),
                JOptionPane.WARNING_MESSAGE);
    }
}
