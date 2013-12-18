package open.dolphin.client;

import open.dolphin.common.util.StampRenderingHints;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.SwingUtilities;

import open.dolphin.infomodel.ModuleModel;
import open.dolphin.order.AbstractStampEditor;

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

    private final StampRenderingHints hints;
    private final StampHolderFunction function;

    private ModuleModel stamp;

    public StampHolder(KartePane kartePane, ModuleModel stamp) {
        super(kartePane);
        function = StampHolderFunction.getInstance();
        function.setDeleteAction(StampHolder.this);
        hints = StampRenderingHints.getInstance();
        setForeground(FOREGROUND);
        setStamp(stamp);
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
            function.setSelectedStampHolder(sh);
            function.showPopupMenu(e.getPoint());
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
        function.setMyText(this);
    }
    
    public StampRenderingHints getHints() {
        return hints;
    }
    
    /**
     * KartePane でこのスタンプがダブルクリックされた時コールされる。
     * StampEditor を開いてこのスタンプを編集する。
     */
    @Override
    public void edit() {
        function.setSelectedStampHolder(this);
        function.edit();
    }
    
    /**
     * エディタで編集した値を受け取り内容を表示する。
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {

        String prop = e.getPropertyName();

        if (AbstractStampEditor.VALUE_PROP.equals(prop)) {

            Object obj = e.getNewValue();
            if (obj instanceof Object[]) {

                Object[] valuePair = (Object[]) obj;
                if (valuePair.length < 2) {
                    return;
                }
                function.setSelectedStampHolder(this);
                ModuleModel[] oldValue = (ModuleModel[]) valuePair[0];
                ModuleModel[] newStamps = (ModuleModel[]) valuePair[1];
                function.setNewValue(newStamps, oldValue);
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
    
    @Override
    public String getAttributeName() {
        return ATTRIBUTE_NAME;
    }
}
