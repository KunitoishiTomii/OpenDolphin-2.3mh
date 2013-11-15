package open.dolphin.stampbox;

import java.util.List;
import open.dolphin.tr.AspStampTreeTransferHandler;

/**
 * AspStampBox
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public class AspStampBox extends AbstractStampBox {
    
    /** Creates new StampBoxPlugin */
    public AspStampBox() {
    }
    
    @Override
    protected void buildStampBox() {

        // Build stampTree
//masuda^
        StampTreeXmlParser parser = new StampTreeXmlParser(StampTreeXmlParser.MODE.ASP);
        List<StampTree> aspTrees = parser.parse(stampTreeModel.getTreeXml());
//masuda$
        stampTreeModel.setTreeXml(null);

        // StampTreeに設定するポップアップメニューとトランスファーハンドラーを生成する
        AspStampTreeTransferHandler transferHandler = new AspStampTreeTransferHandler();

        // StampBox(TabbedPane) へリスト順に格納する
        for (StampTree stampTree : aspTrees) {
            stampTree.setTransferHandler(transferHandler);
            stampTree.setAsp(true);
            stampTree.setStampBox(getContext());
            StampTreePanel treePanel = new StampTreePanel(stampTree);
            this.addTab(stampTree.getTreeName(), treePanel);
        }
    }
}