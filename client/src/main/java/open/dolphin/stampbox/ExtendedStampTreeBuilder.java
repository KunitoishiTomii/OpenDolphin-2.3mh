package open.dolphin.stampbox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import open.dolphin.client.ClientContext;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.infomodel.IInfoModel;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.StampModel;
import open.dolphin.project.Project;
import open.dolphin.util.HexBytesTool;
import open.dolphin.util.XmlUtils;
import org.apache.log4j.Logger;

/**
 * stampBytesも含めたStampTreeBuilder
 *
 * based on DefaultStampTreeBuilder.java
 * @author masuda, Masuda Naika
 */

public class ExtendedStampTreeBuilder {

    /** エディタから発行のスタンプ名 */
    private static final String FROM_EDITOR = "エディタから発行...";
    /** rootノードの名前 */
    private String rootName;
    /** エディタから発行があったかどうかのフラグ */
    private boolean hasEditor;
    /** StampTree のルートノード*/
    private StampTreeNode rootNode;
    /** StampTree のノード*/
    private StampTreeNode node;
    /** ノードの UserObject になる StampInfo */
    private ModuleInfoBean info;
    /** 制御用のリスト */
    private LinkedList<StampTreeNode> linkedList;
    /** 生成物 */
    private List<StampTree> products;
    /** Logger */
    private Logger logger;  // = ClientContext.getLogger("boot");
    
    private Set<String> allStampIds;
    private List<StampModel> stampList;

    // Creates new ExtendedStampTreeBuilder
    public ExtendedStampTreeBuilder() {
        
        logger = ClientContext.getBootLogger();
        
        // 先にユーザーのスタンプをデータベースからまとめて取得しstampIdをHashSetに登録しておく
        allStampIds = new HashSet<>();
        long userId = Project.getUserModel().getId();
        try {
            List<StampModel> allStamps = StampDelegater.getInstance().getAllStamps(userId);
            for (StampModel stamp : allStamps) {
                allStampIds.add(stamp.getId());
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        stampList = new ArrayList<>();
    }

    public List<StampTree> getProduct() {
        return products;
    }

    //build を開始する。
    public void buildStart() {
        products = new ArrayList<StampTree>();
        if (logger != null) {
            logger.debug("Build StampTree start");
        }
    }

    /**
     * Root を生成する。
     * @param name root名
     * @param Stamptree の Entity
     */
    public void buildRoot(String name, String entity) {

        if (logger != null) {
            logger.debug("Root=" + name);
        }
        linkedList = new LinkedList<StampTreeNode>();

        // TreeInfo を 生成し rootNode に保存する
        TreeInfo treeInfo = new TreeInfo();
        treeInfo.setName(name);
        treeInfo.setEntity(entity);
        rootNode = new StampTreeNode(treeInfo);

        hasEditor = false;
        rootName = name;
        linkedList.addFirst(rootNode);
    }

    /**
     * ノードを生成する。
     * @param name ノード名
     */
    public void buildNode(String name) {

        if (logger != null) {
            logger.debug("Node=" + name);
        }
        // Node を生成し現在のノードに加える
        node = new StampTreeNode(fromXmlText(name));
        getCurrentNode().add(node);
        // このノードを first に加える
        linkedList.addFirst(node);
    }

    /**
     * StampInfo を UserObject にするノードを生成する。
     * @param name ノード名
     * @param entity エンティティ
     * @param editable 編集可能かどうかのフラグ
     * @param memo メモ
     * @param id DB key
     * @param stampHexBytes StampModelのstampByetsをHex文字列にしたもの
     */
    public void buildStampInfo(String name,
            String role,
            String entity,
            String editable,
            String memo,
            String id,
            String stampHexBytes) throws Exception {     // stampBytesのHex文字列を追加

        if (logger != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            sb.append(",");
            sb.append(role);
            sb.append(",");
            sb.append(entity);
            sb.append(",");
            sb.append(editable);
            sb.append(",");
            sb.append(memo);
            sb.append(",");
            sb.append(id);
            sb.append(",");
            sb.append(stampHexBytes);   // stampBytesのHex文字列を追加
            logger.debug(sb.toString());
        }

        // StampInfo を生成する
        info = new ModuleInfoBean();
        info.setStampName(fromXmlText(name));
        info.setStampRole(role);
        info.setEntity(entity);
        if (editable != null) {
            info.setEditable(Boolean.valueOf(editable).booleanValue());
        }
        if (memo != null) {
            info.setStampMemo(fromXmlText(memo));
        }
        if (id != null && stampHexBytes != null) {
            info.setStampId(id);
            // データベースにスタンプが存在しない場合は新たに作成して登録する。
            if (!allStampIds.contains(id)) {
                long userId = Project.getUserModel().getId();
                StampModel model = new StampModel();
                //String stampId = GUIDGenerator.generate(model);
                //model.setId(stampId);
                // infoのstampIdは新たに生成したものに置き換える
                //info.setStampId(stampId);
                model.setId(id);    // id 再利用
                model.setEntity(entity);
                model.setUserId(userId);
                byte[] stampBytes = HexBytesTool.hexToBytes(stampHexBytes);
                model.setStampBytes(stampBytes);
                // 新たに作成したStampModelを登録リストに追加する
                stampList.add(model);
            }
        }
        // StampInfo から TreeNode を生成し現在のノードへ追加する
        node = new StampTreeNode(info);
        getCurrentNode().add(node);

        // エディタから発行を持っているか
        if (info.getStampName().equals(FROM_EDITOR) && (! info.isSerialized()) ) {
            hasEditor = true;
            info.setEditable(false);
        }
    }

    // Node の生成を終了する。
    public void buildNodeEnd() {
        if (logger != null) {
            logger.debug("End node");
        }
        linkedList.removeFirst();
    }

    // Root Node の生成を終了する。
    public void buildRootEnd() {

        // エディタから発行...を削除された場合に追加する処置
        if ( (!hasEditor) && (getEntity(rootName) != null) ) {

            if	( getEntity(rootName).equals(IInfoModel.ENTITY_TEXT) || getEntity(rootName).equals(IInfoModel.ENTITY_PATH)) {
                // テキストスタンプとパススタンプにはエディタから発行...はなし
            } else {
                ModuleInfoBean si = new ModuleInfoBean();
                si.setStampName(FROM_EDITOR);
                si.setStampRole(IInfoModel.ROLE_P);
                si.setEntity(getEntity(rootName));
                si.setEditable(false);
                StampTreeNode sn = new StampTreeNode(si);
                rootNode.add(sn);
            }
        }
        // StampTree を生成しプロダクトリストへ加える
        StampTree tree = new StampTree(new StampTreeClientModel(rootNode));
        products.add(tree);

        if (logger != null) {
            int pCount = products.size();
            logger.debug("End root " + "count=" + pCount);
        }
    }

    // build を終了する。
    public void buildEnd() {

        if (logger != null) {
            logger.debug("Build end");
        }
        // ORCAセットを加える
        boolean hasOrca = false;
        for (StampTree st : products) {
            String entity = st.getTreeInfo().getEntity();
            if (entity.equals(IInfoModel.ENTITY_ORCA)) {
                hasOrca = true;
            }
        }

        if (!hasOrca) {
            TreeInfo treeInfo = new TreeInfo();
            treeInfo.setName(IInfoModel.TABNAME_ORCA);
            treeInfo.setEntity(IInfoModel.ENTITY_ORCA);
            rootNode = new StampTreeNode(treeInfo);
            OrcaTree tree = new OrcaTree(new StampTreeClientModel(rootNode));
            products.add(IInfoModel.TAB_INDEX_ORCA, tree);
            if (logger != null) {
                logger.debug("ORCAセットを加えました");
            }
        }
        
        // まとめてデータベースに登録する
        if (!stampList.isEmpty()) {
            try {
                StampDelegater.getInstance().putStamp(stampList);
            } catch (Exception ex) {
                if (logger != null) {
                    logger.debug(ex.getMessage());
                }
            }
        }
    }

    // リストから先頭の StampTreeNode を取り出す。
    private StampTreeNode getCurrentNode() {
        return linkedList.getFirst();
    }

    // 特殊文字を変換する。
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
