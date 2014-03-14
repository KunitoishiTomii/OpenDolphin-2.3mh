package open.dolphin.client;

import open.dolphin.common.util.StampRenderingHints;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.SwingUtilities;
import open.dolphin.common.util.StampHtmlRenderer;

import open.dolphin.infomodel.ModuleModel;
import open.dolphin.order.AbstractStampEditor;
import open.dolphin.order.OldNewValuePair;

/**
 * KartePane に Component　として挿入されるスタンプを保持スルクラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class StampHolder extends AbstractComponentHolder {
    
    public static final String ATTRIBUTE_NAME = "stampHolder";
    private static final Color LBL_COLOR = new Color(0xFF, 0xCE, 0xD9);
    private static final Color FOREGROUND = new Color(20, 20, 140);

    private ModuleModel stamp;

    public StampHolder(KartePane kartePane, ModuleModel stamp) {
        this(kartePane, stamp, false);
    }
    
    public StampHolder(KartePane kartePane, ModuleModel stamp, boolean lazy) {
        super(kartePane);
        getFunction().setDeleteAction(StampHolder.this);
        setFont(getHints().getFont());
        setForeground(FOREGROUND);
        if (lazy) {
            this.stamp = stamp;
        } else {
            setStamp(stamp);
        }
    }

    private StampHolderFunction getFunction() {
        return StampHolderFunction.getInstance();
    }
    
    private StampRenderingHints getHints() {
        return StampRenderingHints.getInstance();
    }
    
    // StampRenderingHintsのlineSpacingを0にすると、ラベルの上部に隙間ができてしまう
    //　見栄えが悪いので線を追加描画する
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Color c = g.getColor();
        g.setColor(LBL_COLOR);
        int w = getWidth() - 2;
        int h = g.getFontMetrics().getHeight() + 2;
        g.drawLine(1, 1, w, 1);
        g.drawLine(1, h, w, h);
        g.setColor(c);
    }
    
    /**
     * Popupメニューを表示する。
     */
    @Override
    public void mabeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            StampHolder sh = (StampHolder) e.getComponent();
            getFunction().setSelectedStampHolder(sh);
            getFunction().showPopupMenu(e.getPoint());
        }
    }

    /**
     * このホルダのモデルを返す。
     * @return
     */
    public ModuleModel getStamp() {
        return stamp;
    }
    
    /**
     * このホルダのモデルを設定する。
     * @param stamp
     */
    public void setStamp(ModuleModel stamp) {
        if (this.stamp != stamp) {
            this.stamp = stamp;
        }
        setMyText();
    }
    
    /**
     * KartePane でこのスタンプがダブルクリックされた時コールされる。
     * StampEditor を開いてこのスタンプを編集する。
     */
    @Override
    public void edit() {
        getFunction().setSelectedStampHolder(this);
        getFunction().edit();
    }
    
    /**
     * エディタで編集した値を受け取り内容を表示する。
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {

        String prop = e.getPropertyName();

        if (AbstractStampEditor.VALUE_PROP.equals(prop)) {

            Object obj = e.getNewValue();
            if (obj instanceof OldNewValuePair) {

                OldNewValuePair valuePair = (OldNewValuePair) obj;
                getFunction().setSelectedStampHolder(this);
                ModuleModel[] oldValue = (ModuleModel[]) valuePair.getOldValue();
                ModuleModel[] newStamps = (ModuleModel[]) valuePair.getNewValue();
                getFunction().setNewValue(newStamps, oldValue);
            }
        }
    }
    
    /**
     * スタンプの内容を置き換える。
     * @param newStamp
     */
    public void importStamp(final ModuleModel newStamp) {
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                setStamp(newStamp);
            }
        });
        
        kartePane.setDirty(true);
    }
    
    public String createStampHtml() {
        StampHtmlRenderer renderer = new StampHtmlRenderer(getStamp(), getHints());
        return renderer.getStampHtml(true);
    }

    public void setMyText() {

        if (getStamp() == null) {
            return;
        }

        setText(createStampHtml());
        // カルテペインへ展開された時広がるのを防ぐ
        setMaximumSize(getPreferredSize());
    }

    public void setMyTextLater() {
        
        if (getStamp() == null) {
            return;
        }
        
        final String stampHtml = createStampHtml();

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                setText(stampHtml);
                // カルテペインへ展開された時広がるのを防ぐ
                setMaximumSize(getPreferredSize());
            }
        });
    }

    @Override
    public String getAttributeName() {
        return ATTRIBUTE_NAME;
    }
}
