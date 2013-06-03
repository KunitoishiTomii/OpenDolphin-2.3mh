package open.dolphin.client;

import java.awt.*;
import javax.swing.text.*;

/**
 * 改行文字を表示するEditorKit
 *
 * @author masuda, Masuda Naika
 * http://terai.xrea.jp/Swing/ParagraphMark.html
 * http://abebas.sub.jp/java/JavaPrograming/01_Editor/011.html
 */
public class KartePanelEditorKit extends StyledEditorKit {

    private static final Color COLOR = Color.GRAY;
    //private static final String CR = "↲";
    //private static final String EOF = "◀";
    private static final Image CR_ICON = ClientContext.getImageIconAlias("icon_cr").getImage();
    private static final Image EOF_ICON = ClientContext.getImageIconAlias("icon_eof").getImage();
    private static final int crMargin = 20;

    private static final KartePanelEditorKit instance;
    private ViewFactory viewFactory;
    
    static {
        instance = new KartePanelEditorKit();
    }

    private KartePanelEditorKit() {
        viewFactory = new VisibleCrViewFactory();
    }
    
    public static KartePanelEditorKit getInstance() {
        return instance;
    }

    @Override
    public ViewFactory getViewFactory() {
        return viewFactory;
    }
    
    private static final class VisibleCrViewFactory implements ViewFactory {

        @Override
        public View create(Element elem) {
            
            String kind = elem.getName();

            if (AbstractDocument.ContentElementName.equals(kind)) {
                return new WrapLabelView(elem);
            } else if (AbstractDocument.ParagraphElementName.equals(kind)) {
                return new MyParagraphView(elem);
            } else if (AbstractDocument.SectionElementName.equals(kind)) {
                return new BoxView(elem, View.Y_AXIS);
            } else if (StyleConstants.ComponentElementName.equals(kind)) {
                return new MyComponentView(elem);
            } else if (StyleConstants.IconElementName.equals(kind)) {
                return new IconView(elem);
            }

            return new LabelView(elem);
        }
    }
    
    // Thread: Word wraping behaviour in JTextPane since Java 7
    // https://forums.oracle.com/forums/thread.jspa?messageID=10757680
    private static final class WrapLabelView extends LabelView {

        private WrapLabelView(Element elem) {
            super(elem);
        }

        @Override
        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return 0;
                case View.Y_AXIS:
                    return super.getMinimumSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }

    private static final class MyComponentView extends ComponentView {

        private MyComponentView(Element elem) {
            super(elem);
        }

        // KartePane幅より広いスタンプの場合に直後の改行文字がwrapされないように
        // 厳密には正しくない
        @Override
        public float getPreferredSpan(int axis) {
            
            Component comp = getComponent();
            if (axis == View.X_AXIS && comp instanceof ComponentHolder) {
                return getComponentHolderSpanX(comp);
            }
            return super.getPreferredSpan(axis);
        }

        @Override
        public float getMaximumSpan(int axis) {
            
            Component comp = getComponent();
            if (axis == View.X_AXIS && comp instanceof ComponentHolder) {
                return getComponentHolderSpanX(comp);
            }
            return super.getMaximumSpan(axis);
        }

        private float getComponentHolderSpanX(Component comp) {
            
            KarteStyledDocument doc = (KarteStyledDocument) getDocument();
            int paneWidth = doc.getKartePane().getTextPane().getWidth() - crMargin;
            int compWidth = comp.getPreferredSize().width;
            
            if (paneWidth > compWidth) {
                return (float) compWidth;
            } else {
                return (float) paneWidth;
            }
        }
        
        @Override
        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return 0;
                case View.Y_AXIS:
                    return super.getMinimumSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }

    private static final class MyParagraphView extends ParagraphView {

        private MyParagraphView(Element elem) {
            super(elem);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            
            super.paint(g, a);

            // 編集可の場合は改行文字を表示する
            try {
                KarteStyledDocument doc = (KarteStyledDocument) getDocument();
                if (doc.getKartePane().getTextPane().isEditable()) {
                    Shape paragraph = modelToView(getEndOffset(), a, Position.Bias.Backward);
                    Rectangle r = (paragraph == null) ? a.getBounds() : paragraph.getBounds();
                    //int fontHeight = g.getFontMetrics().getHeight();
                    Color old = g.getColor();
                    g.setColor(COLOR);
                    if (getEndOffset() != getDocument().getEndPosition().getOffset()) {
                        //g.drawString(CR, r.x + 1, r.y + (r.height + fontHeight) / 2 - 2);
                        g.drawImage(CR_ICON, r.x + 1, r.y + (r.height - 10) / 2 + 2, null);
                    } else {
                        //g.drawString(EOF, r.x + 1, r.y + (r.height + fontHeight) / 2 - 2);
                        g.drawImage(EOF_ICON, r.x + 1, r.y + (r.height - 10) / 2 + 2, null);
                    }
                    g.setColor(old);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
