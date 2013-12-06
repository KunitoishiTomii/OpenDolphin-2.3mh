package open.dolphin.stampbox;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.client.ClientContext;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.StampModel;
import open.dolphin.project.Project;
import open.dolphin.util.GUIDGenerator;
import open.dolphin.util.HexBytesTool;
import open.dolphin.common.util.XmlUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Xml to StampTree
 * 
 * @author masuda, Masuda Naika
 */
public class StampTreeXmlParser {
    
    //private static final String ELEM_STAMPBOX = "stampBox";
    //private static final String ELEM_STAMPTREE = "stampTree";
    //private static final String ELEM_EXTENDED_STAMPTREE = "extendedStampTree";
    private static final String ELEM_ROOT = "root";
    private static final String ELEM_NODE = "node";
    private static final String ELEM_STAMPINFO = "stampInfo";
    
    private static final String ATTR_NAME = "name";
    private static final String ATTR_ENTITY = "entity";
    private static final String ATTR_ROLE = "role";
    private static final String ATTR_EDITABLE = "editable";
    private static final String ATTR_MEMO = "memo";
    private static final String ATTR_STAMPID = "stampId";
    private static final String ATTR_STAMPBYTES = "stampBytes";
    
    public static enum MODE {DEFAULT, FILE, ASP};
    
    private final boolean debug;
    private final Logger logger;
    private final MODE mode;
    
    // StampTree のルートノード
    private StampTreeNode rootNode;
    private String rootName;
    
    // エディタから発行があるか
    private boolean hasEditor;
    
    private final List<StampTree> treeList;
    
    private final LinkedList<StampTreeNode> stack;
    
    private final Set<String> allStampIds;
    private final List<StampModel> stampListToAdd;
    
    
    public StampTreeXmlParser(MODE mode) {
        this.mode = mode;
        logger = ClientContext.getBootLogger();
        debug = (logger.getLevel() == Level.DEBUG);
        treeList = new ArrayList<>();
        stack = new LinkedList<>();
        allStampIds = new HashSet<>();
        stampListToAdd = new ArrayList<>();
    }
    
    public List<StampTree> parse(String xml) {
        StringReader stringReader = new StringReader(xml);
        return parse(stringReader);
    }
    
    public List<StampTree> parse(Reader reader) {
        
        if (debug) {
            logger.debug("Build StampTree start");
        }
        
        // 先にユーザーのスタンプをデータベースからまとめて取得しsrycdをHashSetに登録しておく
        if (mode == MODE.FILE) {
            long userId = Project.getUserModel().getId();
            try {
                List<StampModel> allStamps = StampDelegater.getInstance().getAllStamps(userId);
                for (StampModel stamp : allStamps) {
                    allStampIds.add(stamp.getId());
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
        
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader streamReader = factory.createXMLStreamReader(reader);

            while (streamReader.hasNext()) {
                int eventType = streamReader.next();
                switch (eventType) {
                    case XMLStreamReader.START_ELEMENT:
                        startElement(streamReader);
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        endElement(streamReader);
                        break;
                }
            }
        } catch (XMLStreamException ex) {
        }
        
        // build を終了する。
        buidEnd();
        
        return treeList;
    }
    
    private void startElement(XMLStreamReader reader) throws XMLStreamException {

        String eName = reader.getName().getLocalPart();
        
        if (eName == null) {
            return;
        }
        
        switch (eName) {
            case ELEM_STAMPINFO:
                String name = reader.getAttributeValue(null, ATTR_NAME);
                String role = reader.getAttributeValue(null, ATTR_ROLE);
                String entity = reader.getAttributeValue(null, ATTR_ENTITY);
                String editable = reader.getAttributeValue(null, ATTR_EDITABLE);
                String memo = reader.getAttributeValue(null, ATTR_MEMO);
                String stampId = reader.getAttributeValue(null, ATTR_STAMPID);
                String stampHexBytes = reader.getAttributeValue(null, ATTR_STAMPBYTES);
                startStampInfo(name, role, entity, editable, memo, stampId, stampHexBytes);
                break;
            case ELEM_NODE:
                name = reader.getAttributeValue(null, ATTR_NAME);
                startNode(name);
                break;
            case ELEM_ROOT:
                name = reader.getAttributeValue(null, ATTR_NAME);
                entity = reader.getAttributeValue(null, ATTR_ENTITY);
                startRoot(name, entity);
                break;
        }
    }
    
    private void startStampInfo(String name, String role, String entity,
            String editable, String memo, String stampId, String stampHexBytes) {
        
        name = fromXmlText(name);
        memo = fromXmlText(memo);
        
        if (debug) {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(",");
            sb.append(role).append(",");
            sb.append(entity).append(",");
            sb.append(editable).append(",");
            sb.append(memo).append(",");
            sb.append(stampId);
            logger.debug(sb.toString());
        }
        
        if (mode == MODE.ASP) {
            // ASP Tree なのでエディタから発行を無視する
            //if (IInfoModel.TITLE_FROM_EDITOR.equals(name) && stampId == null && IInfoModel.ROLE_P.equals(role)) {
            if (IInfoModel.TITLE_FROM_EDITOR.equals(name)) {
                return;
            }
        }
        
        // StampInfo を生成する
        ModuleInfoBean info = new ModuleInfoBean();
        info.setStampName(name);
        info.setStampRole(role);
        info.setEntity(entity);
        if (editable != null) {
            info.setEditable(Boolean.valueOf(editable).booleanValue());
        }
        if (memo != null) {
            info.setStampMemo(memo);
        }
        if (stampId != null ) {
            info.setStampId(stampId);
        }
        
        if (mode == MODE.FILE && stampId != null && stampHexBytes != null) {
            // データベースにスタンプが存在しない場合は新たに作成して登録する。
            if (!allStampIds.contains(stampId)) {
                StampModel model = new StampModel();
                String newStampId = GUIDGenerator.generate(model);
                info.setStampId(newStampId);
                model.setId(newStampId);
                model.setEntity(entity);
                model.setUserId(Project.getUserModel().getId());
                byte[] stampBytes = HexBytesTool.hexToBytes(stampHexBytes);
                model.setStampBytes(stampBytes);
                // 新たに作成したStampModelを登録リストに追加する
                stampListToAdd.add(model);
            }
        }
        
        // StampInfo から TreeNode を生成し現在のノードへ追加する
        StampTreeNode node = new StampTreeNode(info);
        getCurrentNode().add(node);
        
        // エディタから発行を持っているか
        if (IInfoModel.TITLE_FROM_EDITOR.equals(info.getStampName()) && !info.isSerialized()) {
            hasEditor = true;
            info.setEditable(false);
        }
    }
    
    private void startNode(String name) {
        
        name = fromXmlText(name);
        
        if (debug) {
            logger.debug("Node=" + name);
        }
        
        // Node を生成し現在のノードに加える
        StampTreeNode node = new StampTreeNode(name);
        getCurrentNode().add(node);
        
        // このノードを first に加える
        stack.addFirst(node);
    }
    
    private void startRoot(String name, String entity) {
        
        name = fromXmlText(name);
        
        if (debug) {
            logger.debug("Root=" + name);
        }
        
        // TreeInfo を 生成し rootNode に保存する
        TreeInfo treeInfo = new TreeInfo();
        treeInfo.setName(fromXmlText(name));
        treeInfo.setEntity(entity);
        rootNode = new StampTreeNode(treeInfo);
        
        hasEditor = false;
        rootName = name;
        
        stack.clear();
        stack.addFirst(rootNode);
    }
    
    private void endElement(XMLStreamReader reader) throws XMLStreamException {

        String eName = reader.getName().getLocalPart();
        
        if (eName == null) {
            return;
        }
        
        switch(eName) {
            case ELEM_STAMPINFO:
                endStampInfo();
                break;
            case ELEM_NODE:
                endNode();
                break;
            case ELEM_ROOT:
                endRoot();
                break;
        }
    }
    
    private void endStampInfo() {
    }
    
    private void endNode() {
        if (debug) {
            logger.debug("End node");
        }
        stack.removeFirst();
    }
    
    private void endRoot() {
        
        // エディタから発行...を削除された場合に追加する処置
        if (mode != MODE.ASP && !hasEditor) {
            String entity = getEntity(rootName);
            // テキストスタンプとパススタンプにはエディタから発行...はなし
            if (!IInfoModel.ENTITY_TEXT.equals(entity) && !IInfoModel.ENTITY_PATH.equals(entity)) {
                ModuleInfoBean info = new ModuleInfoBean();
                info.setStampName(IInfoModel.TITLE_FROM_EDITOR);
                info.setStampRole(IInfoModel.ROLE_P);
                info.setEntity(getEntity(rootName));
                info.setEditable(false);
                StampTreeNode node = new StampTreeNode(info);
                rootNode.add(node);
            }
        }

        // StampTree を生成しプロダクトリストへ加える
        StampTree tree = new StampTree(new StampTreeClientModel(rootNode));
        treeList.add(tree);

        if (debug) {
            int pCount = treeList.size();
            logger.debug("End root " + "count=" + pCount);
        }
        
        stack.removeFirst();
    }
    
    private void buidEnd() {
        
        if (debug) {
            logger.debug("Build end");
        }
        
        if (mode == MODE.ASP) {
            return;
        }
        
        // ORCAセットを加える
        boolean hasOrca = false;
        for (StampTree tree : treeList) {
            String entity = tree.getTreeInfo().getEntity();
            if (IInfoModel.ENTITY_ORCA.equals(entity)) {
                hasOrca = true;
            }
        }

        if (!hasOrca) {
            TreeInfo treeInfo = new TreeInfo();
            treeInfo.setName(IInfoModel.TABNAME_ORCA);
            treeInfo.setEntity(IInfoModel.ENTITY_ORCA);
            rootNode = new StampTreeNode(treeInfo);
            OrcaTree tree = new OrcaTree(new StampTreeClientModel(rootNode));
            treeList.add(IInfoModel.TAB_INDEX_ORCA, tree);
            if (debug) {
                logger.debug("ORCAセットを加えました");
            }
        }
        
        // まとめてデータベースに登録する
        if (mode == MODE.FILE && !stampListToAdd.isEmpty()) {
            try {
                StampDelegater.getInstance().putStamp(stampListToAdd);
            } catch (Exception ex) {
                if (logger != null) {
                    logger.debug(ex.getMessage());
                }
            }
        }
    }
    
    /**
     * リストから先頭の StampTreeNode を取り出す。
     */
    private StampTreeNode getCurrentNode() {
        return stack.getFirst();
    }
    
    /**
     * 特殊文字を変換する。
     */
    private String fromXmlText(String xml) {
        return XmlUtils.fromXml(xml);
    }
    
    private String getEntity(String rootName) {
        
        String ret = null;
        
        if (rootName == null) {
            return ret;
        }
        
        for (int i = 0; i < IInfoModel.STAMP_ENTITIES.length; i++) {
            if (IInfoModel.STAMP_NAMES[i].equals(rootName)) {
                ret = IInfoModel.STAMP_ENTITIES[i];
                break;
            }
        }
        
        return ret;
    }
}
