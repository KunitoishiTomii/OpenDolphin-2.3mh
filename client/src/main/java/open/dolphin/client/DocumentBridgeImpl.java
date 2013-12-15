package open.dolphin.client;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JScrollPane;
import open.dolphin.infomodel.DocInfoModel;
import open.dolphin.infomodel.IInfoModel;

/**
 * 参照タブ画面を提供する Bridge クラス。このクラスの scroller へ
 * カルテ、紹介状等のドキュメントが表示される。
 * 
 * @author kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class DocumentBridgeImpl extends AbstractChartDocument 
    implements PropertyChangeListener, DocumentBridger {
    
    private static final String TITLE = "参 照";
        
    // Scroller  
    private JScrollPane scroller;
    
    // 現在の文書表示クラスのインターフェイス
    private DocumentViewer curViewer;    
    
    // 使い回し
    private final Map<String, DocumentViewer> viewerCache;
    
    
    public DocumentBridgeImpl() {
        setTitle(TITLE);
        viewerCache = new HashMap<>();
    }
    
    @Override
    public void start() {
        
        scroller = new JScrollPane();
        getUI().setLayout(new BorderLayout());
        getUI().add(scroller, BorderLayout.CENTER);
        
//pns^   カルテ表示のスクロール増分を多くする        
        scroller.getVerticalScrollBar().setUnitIncrement(15);
        //スクロールバーを常に表示しないと，スクロールバーが表示されるときにカルテがスクロールバー分伸びて尻切れになることがある
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//pns$

        //----------------------------------------
        // 文書履歴のプロパティ通知をリッスンする
        //----------------------------------------
        DocumentHistory h = getContext().getDocumentHistory();
        // 文書種別
        h.addPropertyChangeListener(DocumentHistory.DOCUMENT_TYPE, this);
        // 抽出期間
        h.addPropertyChangeListener(DocumentHistory.HISTORY_UPDATED, this);
        // 選択
        h.addPropertyChangeListener(DocumentHistory.SELECTED_HISTORIES, this);
        
        // 検索結果でカルテ選択するためproperty change listenerを登録しておく
        SearchResultInspector sr = h.getSearchResult();
        sr.addPropertyChangeListener(DocumentHistory.SELECTED_HISTORIES, this);
        // scrollerの縁を狭くする
        scroller.setBorder(null);
        
        // 最初はKarteDocumentViewer
        curViewer = createKarteDocumentViewer();
            
        enter();
    }

    @Override
    public void stop() {
        // viewerをすべてstopする
        for (DocumentViewer viewer : viewerCache.values()) {
            viewer.stop();
        }
    }
    
    @Override
    public void enter() {
        if (curViewer != null) {
            // これによりメニューは viwer で制御される
            curViewer.enter();
        } else {
            super.enter();
        }
    }
    
    /**
     * Bridge 機能を提供する。選択された文書のタイプに応じてビューへブリッジする。
     * @param docs 表示する文書の DocInfo 配列
     */
    @Override
    public void showDocuments(DocInfoModel[] docs) {
        
        if (docs != null && docs.length > 0) {
            
            String handleClass = docs[0].getHandleClass();

            if (handleClass != null) {
                // 紹介状などの場合
                try {
                    curViewer = createLetterModuleViewer(handleClass);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    curViewer = null;
                    e.printStackTrace(System.err);
                }
            }
        }

        // current viewerで文書を表示する
        if (curViewer != null) {
            curViewer.showDocuments(docs, scroller);
            curViewer.enter();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        
        String prop = evt.getPropertyName();

        switch (prop) {
            case DocumentHistory.DOCUMENT_TYPE:
                // 文書種別が変更された場合
                String docType = (String) evt.getNewValue();
                switch (docType) {
                    case IInfoModel.DOCTYPE_LETTER:     // 紹介状
                        curViewer = null;               // showDocumentsでcurViewerを設定する
                        break;
                    case IInfoModel.DOCTYPE_KARTE:      // カルテ文書
                        curViewer = createKarteDocumentViewer();
                        break;
                }
                break;
            
            case DocumentHistory.HISTORY_UPDATED:
                // 文書履歴の抽出期間が変更された場合
                if (curViewer != null) {
                    curViewer.historyPeriodChanged();
                }
                //scroller.setViewportView(null);
                break;
                
            case DocumentHistory.SELECTED_HISTORIES:
                // 文書履歴の選択が変更された場合
                DocInfoModel[] selectedHistoroes = (DocInfoModel[]) evt.getNewValue();
                showDocuments(selectedHistoroes);
                break;
        }
    }
    
    public KarteViewer getBaseKarte() {
        if (curViewer != null && curViewer instanceof KarteDocumentViewer) {
            KarteDocumentViewer viewer = (KarteDocumentViewer) curViewer;
            return viewer.getBaseKarte();
        }
        return null;
    }

    private DocumentViewer createKarteDocumentViewer() {

        DocumentViewer viewer = viewerCache.get(KarteDocumentViewer.class.getName());
        
        // キャッシュになければ作成する
        if (viewer == null) {
            viewer = new KarteDocumentViewer();
            viewer.setContext(getContext());
            viewer.start();     // ここでstartしておく
            viewerCache.put(viewer.getClass().getName(), viewer);
        }

        return viewer;
    }

    private DocumentViewer createLetterModuleViewer(String handleClass)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        DocumentViewer viewer = viewerCache.get(handleClass);
        
        // キャッシュになければ作成する
        if (viewer == null) {
            viewer = (DocumentViewer) Class.forName(handleClass).newInstance();
            viewer.setContext(getContext());
            viewer.start();     // ここでstartしておく
            viewerCache.put(viewer.getClass().getName(), viewer);
        }

        return viewer;
    }
    
}
