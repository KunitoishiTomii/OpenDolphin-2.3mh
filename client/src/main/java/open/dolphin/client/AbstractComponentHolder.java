package open.dolphin.client;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Position;
import open.dolphin.tr.DolphinTransferHandler;

/**
 * ComponentHolder
 *
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public abstract class AbstractComponentHolder extends JLabel 
        implements MouseListener, DragGestureListener, ComponentHolder {
    
    private static final Color SELECTED_BORDER = new Color(255, 0, 153);
    private static final Color NON_SELECTED_BORDER = new Color(0, 0, 0, 0); // 透明
    protected static final Border nonSelectedBorder = 
            BorderFactory.createLineBorder(NON_SELECTED_BORDER);
    protected static final Border selectedBorder = 
            BorderFactory.createLineBorder(SELECTED_BORDER);
    
    private Position startPosition;
    private boolean selected;
    protected final KartePane kartePane;

    
    public AbstractComponentHolder(KartePane kartePane) {
        
        this.kartePane = kartePane;
        setDoubleBuffered(true);
        setOpaque(false);
        setBackground(null);
        setBorder(nonSelectedBorder);
        
        setFocusable(true);
        addMouseListener(AbstractComponentHolder.this);
        addMouseListener(new PopupListner());
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        
        ActionMap map = this.getActionMap();
        map.put(TransferHandler.getCutAction().getValue(Action.NAME), TransferHandler.getCutAction());
        map.put(TransferHandler.getCopyAction().getValue(Action.NAME), TransferHandler.getCopyAction());
        map.put(TransferHandler.getPasteAction().getValue(Action.NAME), TransferHandler.getPasteAction());

        // DragGestureを使ってみる
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(AbstractComponentHolder.this, DnDConstants.ACTION_COPY_OR_MOVE, AbstractComponentHolder.this);
    }
    
    /**
     * このスタンプホルダのKartePaneを返す。
     */
    @Override
    public KartePane getKartePane() {
        return kartePane;
    }
    
    @Override
    public int getStartOffset() {
        return startPosition.getOffset();
    }
    
    @Override
    public void setStartPosition(Position start) {
        startPosition = start;
    }
    
    /**
     * 選択されているかどうかを返す。
     * @return 選択されている時 true
     */
    @Override
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * 選択属性を設定する。
     * @param selected 選択の時 true
     */
    @Override
    public void setSelected(boolean selected) {
        if (selected) {
            setBorder(selectedBorder);
            this.selected = true;
        } else {
            setBorder(nonSelectedBorder);
            this.selected = false;
        }
    }
    
    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {

        int action = dge.getDragAction();
        InputEvent event = dge.getTriggerEvent();
        JComponent comp = (JComponent) dge.getComponent();
        TransferHandler handler = comp.getTransferHandler();
        if (handler != null) {
            handler.exportAsDrag(comp, event, action);
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        
        // TransferHandlerにmodifiersExを記録しておく
        int modifiersEx = e.getModifiersEx();
        DolphinTransferHandler.setModifiersEx(modifiersEx);

        // StampEditor から戻った後に動作しないため
        boolean focus = requestFocusInWindow();

        if (!focus) {
            requestFocusInWindow();
        }

        if (e.getClickCount() == 2 && (!e.isPopupTrigger())) {
            edit();
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
    
    public abstract void mabeShowPopup(MouseEvent e);
    
    private class PopupListner extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getClickCount() != 2) {
                mabeShowPopup(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getClickCount() != 2) {
                mabeShowPopup(e);
            }
        }
    }
}