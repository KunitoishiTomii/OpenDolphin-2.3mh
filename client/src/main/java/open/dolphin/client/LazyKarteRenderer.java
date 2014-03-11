package open.dolphin.client;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 怠惰なKarteRenderer
 *
 * @author masuda, Masuda Naika
 */
public class LazyKarteRenderer implements ChangeListener {

    // 未レンダリングのカルテビューア
    private final List<KarteViewer> viewerList;

    public LazyKarteRenderer() {
        viewerList = new CopyOnWriteArrayList<>();
    }

    public void addKarteViewer(KarteViewer viewer) {
        viewerList.add(viewer);
    }

    public void clearList() {
        viewerList.clear();
    }

    public void renderRemainedKarte() {

        if (viewerList.isEmpty()) {
            return;
        }

        List<KarteViewer> toRemove = new ArrayList<>();
        for (KarteViewer viewer : viewerList) {
            viewer.renderKarte();
            toRemove.add(viewer);
        }
        viewerList.removeAll(toRemove);
    }

    public void renderKarte(Rectangle viewRect) {

        if (viewerList.isEmpty()) {
            return;
        }
        
        List<KarteViewer> toRemove = new ArrayList<>();
        for (KarteViewer viewer : viewerList) {
            if (viewRect.intersects(viewer.getUI().getBounds())) {
                viewer.renderKarte();
                toRemove.add(viewer);
            }
        }
        viewerList.removeAll(toRemove);
    }

    @Override
    public void stateChanged(ChangeEvent e) {

        Rectangle viewRect = ((JViewport) e.getSource()).getViewRect();
        renderKarte(viewRect);
    }

}
