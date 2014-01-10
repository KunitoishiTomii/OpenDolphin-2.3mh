package open.dolphin.stampbox;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;
import open.dolphin.client.ClientContext;
import open.dolphin.common.util.SimpleXmlWriter;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.StampModel;
import open.dolphin.project.Project;
import open.dolphin.util.HexBytesTool;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * StampTree to Xml
 * 
 * @author masuda, Masuda Naika
 */
public class StampTreeXmlBuilder {
    
    //private static final String ELEM_STAMPBOX = "stampBox";
    private static final String ELEM_STAMPTREE = "stampTree";
    private static final String ELEM_EXTENDED_STAMPTREE = "extendedStampTree";
    private static final String ELEM_ROOT = "root";
    private static final String ELEM_NODE = "node";
    private static final String ELEM_STAMPINFO = "stampInfo";
    
    private static final String ATTR_PROJECT = "project";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_ENTITY = "entity";
    private static final String ATTR_ROLE = "role";
    private static final String ATTR_EDITABLE = "editable";
    private static final String ATTR_MEMO = "memo";
    private static final String ATTR_STAMPID = "stampId";
    private static final String ATTR_STAMPBYTES = "stampBytes";
    private static final String COMMENT = " StampBox Export Data, Creator: %s, Created on: %s ";

    public static enum MODE {DEFAULT, FILE};
    
    private final MODE mode;
    private final boolean debug;
    private final Logger logger;
    
    private final Map<String, StampModel> allStampMap;
    private final Deque<StampTreeNode> stack;
    
    private SimpleXmlWriter writer;
    
    
    public StampTreeXmlBuilder(MODE mode) {
        
        this.mode = mode;
        logger = ClientContext.getBootLogger();
        debug = (logger.getLevel() == Level.DEBUG);
        stack = new ArrayDeque<>();
        allStampMap = new HashMap<>();
    }
    
    /**
     * スタンプツリー全体をXMLにエンコードする。
     * @param allTrees StampTreeのリスト
     * @return XML
     */
    public String build(List<StampTree> allTrees) {

        if (debug) {
            logger.debug("StampTree Build start");
        }

        writer = new SimpleXmlWriter();
        writer.setRepcaceXmlChar(true);
        writer.setReplaceZenkaku(false);

        String treeElementName = ELEM_STAMPTREE;
        if (mode == MODE.FILE) {
            // 先にユーザーのスタンプをデータベースからまとめて取得しHashMapに登録しておく
            getAllStamps();
            // コメントを設定する
            writer.writeStartDocument("UTF-8", "1.0");
            String facility = Project.getUserModel().getFacilityModel().getFacilityName();
            String today = new Date().toString();
            writer.writeComment(String.format(COMMENT, facility, today));
            treeElementName = ELEM_EXTENDED_STAMPTREE;
        }

        // stampTree elementを書き出す
        writer.writeStartElement(treeElementName)
                .writeAttribute(ATTR_PROJECT, "open.dolphin")
                .writeAttribute(ATTR_VERSION, "1.0");

        // 各StampTreeを書き出していく
        for (StampTree tree : allTrees) {
            writeStampTree(tree);
        }

        // stampTree elementを閉じる
        writer.writeEndElement();

        allStampMap.clear();

        return writer.getProduct();
    }
    
    private void writeStampTree(StampTree tree) {
        
        // ルートノードを取得しチャイルドのEnumerationを得る
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration<StampTreeNode> e = rootNode.preorderEnumeration();
        
        // 順に書き出していく
        while (e.hasMoreElements()) {
            
            StampTreeNode node = e.nextElement();
            endElementsIfParentNodeChanged(node);
            
            if (node.isRoot()) {
                buildRootNode(node);
            } else if (node.isLeaf()) {
                buildLeafNode(node);
            } else {
                buildDirectoryNode(node);
            }
        }
        
        endStakcedElements();
    }

    private void endElementsIfParentNodeChanged(StampTreeNode stampTreeNode) {
        
        // 親Nodeが変更になる場合はwriteEndElementする
        StampTreeNode parent = (StampTreeNode) stampTreeNode.getParent();
        
        if (stack.contains(parent)) {
            for (Iterator<StampTreeNode> itr = stack.iterator(); itr.hasNext();) {
                StampTreeNode node = itr.next();
                if (node == parent) {
                    break;
                }
                itr.remove();
                writer.writeEndElement();
            }
        }
    }
    
    private void endStakcedElements() {
        for (int i = 0; i < stack.size(); ++i) {
            writer.writeEndElement();
        }
        stack.clear();
    }

    
    private void buildRootNode(StampTreeNode node) {
        
        if (debug) {
            logger.debug("Build Root Node: " + node.toString());
        }

        TreeInfo treeInfo = (TreeInfo) node.getUserObject();
        writer.writeStartElement(ELEM_ROOT)
                .writeAttribute(ATTR_NAME, treeInfo.getName())
                .writeAttribute(ATTR_ENTITY, treeInfo.getEntity());
        
        stack.push(node);
    }
    
    private void buildDirectoryNode(StampTreeNode node) {
        
        // 子ノードを持たないディレクトリノードは書き出さない
        if (node.getChildCount() > 0) {
            if (debug) {
                logger.debug("Build Directory Node: " + node.toString());
            }
            writer.writeStartElement(ELEM_NODE)
                    .writeAttribute(ATTR_NAME, node.toString());
            
            stack.push(node);
        }
    }
    
    private void buildLeafNode(StampTreeNode node) {
        
        if (debug) {
            logger.debug("Build Leaf Node: " + node.toString());
        }
        
        ModuleInfoBean info = (ModuleInfoBean) node.getUserObject();
        String stampId = info.getStampId();
        
        // ここで対応するstampBytesをHashMapから読み込む
        String stampHexBytes = null;
        if (mode == MODE.FILE) {
            stampHexBytes = getHexStampBytes(stampId);
            // 実体のないスタンプの場合があった。なぜゾンビができたのだろう？？
            if (stampId != null && !allStampMap.isEmpty() && stampHexBytes == null) {
                //System.out.println("ゾンビ:" + stampId);
                node.removeFromParent();
                return;
            }
        }
        
        // stampInfoはEmptyElementである
        writer.writeEmptyElement(ELEM_STAMPINFO)
                .writeAttribute(ATTR_NAME, node.toString())
                .writeAttribute(ATTR_ROLE, info.getStampRole())
                .writeAttribute(ATTR_ENTITY, info.getEntity())
                .writeAttribute(ATTR_EDITABLE, String.valueOf(info.isEditable()));
        
        String memo = info.getStampMemo();
        if (memo != null) {
            writer.writeAttribute(ATTR_MEMO, memo);
        }
        if (stampId != null) {
            writer.writeAttribute(ATTR_STAMPID, stampId);
        }
        if (stampHexBytes != null) {
            writer.writeAttribute(ATTR_STAMPBYTES, stampHexBytes);
        }
    }
    
    // StampIdから対応するStampModelを取得してstampBytesのHex文字列を作成する
    private String getHexStampBytes(String stampId) {

        // スタンプの実体を取得
        StampModel model = allStampMap.get(stampId);
        // データベースにない場合はnullを返す
        if (model == null){
            return null;
        }
        // stampBytesを返す
        byte[] stampBytes = model.getStampBytes();
        return HexBytesTool.bytesToHex(stampBytes);
    }
    
    private void getAllStamps() {
        long userId = Project.getUserModel().getId();
        try {
            List<StampModel> allStamps = StampDelegater.getInstance().getAllStamps(userId);
            for (StampModel stamp : allStamps) {
                allStampMap.put(stamp.getId(), stamp);
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }
}
