package open.dolphin.delegater;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import open.dolphin.infomodel.*;

/**
 * Stamp関連の Delegater クラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class StampDelegater extends BusinessDelegater {

    private static final String UTF8 = "UTF-8";
    private static final String RES_STAMP_TREE = "stampTree/";
    private static final String RES_STAMP = "stamp/";
    
    private static final boolean debug = false;
    private static final StampDelegater instance;
    
    private final Map<String, StampModel> stampCache;

    static {
        instance = new StampDelegater();
    }

    public static StampDelegater getInstance() {
        return instance;
    }

    private StampDelegater() {
        stampCache = new HashMap<String, StampModel>();
    }
    
    /**
     * StampTree を保存/更新する。
     * @param model 保存する StampTree
     * @return 保存個数
     */
    public long putTree(IStampTreeModel model) throws Exception {
        
        model.setTreeBytes(model.getTreeXml().getBytes(UTF8)); // UTF-8 bytes

        // こっちでキャストしておく
        StampTreeModel treeModel = (StampTreeModel) model;

        Entity entity = toJsonEntity(treeModel);

        // resource post
        String path = RES_STAMP_TREE;
        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        long pk = Long.parseLong(entityStr);
        return pk;
    }

    public List<IStampTreeModel> getTrees(long userPK) throws Exception {
        
        String path = RES_STAMP_TREE + String.valueOf(userPK);
        
        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        UserStampTreeModel ret = (UserStampTreeModel) getConverter()
                .fromJson(is, UserStampTreeModel.class);
        
        response.close();

        List<IStampTreeModel> treeList = new ArrayList<IStampTreeModel>();
        List<IStampTreeModel> list = ret.getTreeList();

        for (IStampTreeModel model : list) {
            try {
                String treeXml = new String(model.getTreeBytes(), UTF8);
                model.setTreeXml(treeXml);
                model.setTreeBytes(null);
                treeList.add(model);
            } catch (UnsupportedEncodingException ex) {
                logger.warn(ex.getMessage());
            }
        }

        return treeList;
    }
    
    /**
     * 個人用のStampTreeを保存し公開する。
     * @param model 個人用のStampTreeで公開するもの
     * @return id
     */
    public long saveAndPublishTree(StampTreeModel model, byte[] publishBytes) throws Exception {
        
        model.setTreeBytes(model.getTreeXml().getBytes(UTF8));

        PublishedTreeModel publishedModel = createPublishedTreeModel(model, publishBytes);

        // interfaceはmarshallingできない
        UserStampTreeModel treeModel = new UserStampTreeModel();
        treeModel.setStampTreeList(Collections.singletonList(model));
        treeModel.setPublishedList(Collections.singletonList(publishedModel));

        Entity entity = toJsonEntity(treeModel);

        String path = RES_STAMP_TREE + "published";

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return Long.valueOf(entityStr);
    }
    
    /**
     * 既に保存されている個人用のTreeを公開する。
     * @param model 既に保存されている個人用のTreeで公開するもの
     * @return 公開数
     */
    public int publishTree(StampTreeModel model, byte[] publishBytes) throws Exception {
        
        // updatePublishedTreeといっしょ
        return updatePublishedTree(model, publishBytes);

    }
    
    /**
     * 公開されているTreeを更新する。
     * @param model 更新するTree
     * @return 更新数
     */
    public int updatePublishedTree(StampTreeModel model, byte[] publishBytes) throws Exception {
        
        model.setTreeBytes(model.getTreeXml().getBytes(UTF8));

        PublishedTreeModel publishedModel = createPublishedTreeModel(model, publishBytes);

        // interfaceはunmarshallingできない
        UserStampTreeModel treeModel = new UserStampTreeModel();
        treeModel.setStampTreeList(Collections.singletonList(model));
        treeModel.setPublishedList(Collections.singletonList(publishedModel));

        Entity entity = toJsonEntity(treeModel);

        String path = RES_STAMP_TREE + "published";

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return Integer.valueOf(entityStr);
     }
    
    /**
     * 公開されているTreeを削除する。
     * @param id 削除するTreeのID
     * @return 削除数
     */
    public int cancelPublishedTree(StampTreeModel model) throws Exception {
        
        model.setTreeBytes(model.getTreeXml().getBytes(UTF8));

        Entity entity = toJsonEntity(model);

        String path = RES_STAMP_TREE + "published/cancel/";

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        debug(status, "put response");
        
        response.close();

        return 1;
    }
    
    public List<PublishedTreeModel> getPublishedTrees() throws Exception {
        
        String path = RES_STAMP_TREE + "published";

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<PublishedTreeModel>>(){};
        List<PublishedTreeModel> ret = (List<PublishedTreeModel>)
                getConverter().fromJson(is, typeRef);
        
        response.close();

        return ret;
    }

    // 個人用StampTreeから公開用StampTreeを生成する。
    // byte[] publishBytes は公開されるカテゴリのみを含むサブセットバイト
    private PublishedTreeModel createPublishedTreeModel(StampTreeModel model, byte[] publishBytes) {
        PublishedTreeModel publishedModel = new PublishedTreeModel();
        publishedModel.setId(model.getId());                            // pk
        publishedModel.setUserModel(model.getUserModel());              // UserModel
        publishedModel.setName(model.getName());                        // 名称
        publishedModel.setPublishType(model.getPublishType());          // 公開タイプ
        publishedModel.setCategory(model.getCategory());                // カテゴリ
        publishedModel.setPartyName(model.getPartyName());              // パーティー名
        publishedModel.setUrl(model.getUrl());                          // URL
        publishedModel.setDescription(model.getDescription());          // 説明
        publishedModel.setPublishedDate(model.getPublishedDate());      // 公開日
        publishedModel.setLastUpdated(model.getLastUpdated());          // 更新日
        publishedModel.setTreeBytes(publishBytes);                      // XML bytes
        return publishedModel;
    }


    //---------------------------------------------------------------------------

    public List<Long> subscribeTrees(List<SubscribedTreeModel> subscribeList) throws Exception {
        
        Entity entity = toJsonEntity(subscribeList);

        String path = RES_STAMP_TREE + "subscribed";

        Response response = getWebTarget()
                .path(path) 
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        String[] pks = entityStr.split(",");
        List<Long> ret = new ArrayList<Long>(pks.length);
        for (String str : pks) {
            ret.add(Long.valueOf(str));
        }
        return ret;
    }
    
    
    public int unsubscribeTrees(List<SubscribedTreeModel> removeList) throws Exception {

        String path = RES_STAMP_TREE +"subscribed";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (SubscribedTreeModel s : removeList) {
            if (!first) {
                sb.append(CAMMA);
            } else {
                first = false;
            }
            sb.append(String.valueOf(s.getTreeId()));
            sb.append(CAMMA);
            sb.append(String.valueOf(s.getUserModel().getId()));
        }

        Response response = getWebTarget()
                .path(path)
                .queryParam(IDS, sb.toString())
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();

        return 1;
    }
    

    //---------------------------------------------------------------------------

    /**
     * Stampを保存する。
     * @param model StampModel
     * @return 保存件数
     */
    public List<String> putStamp(List<StampModel> list) throws Exception {
        
        // キャッシュに登録する
        for (StampModel model : list) {
            stampCache.put(model.getId(), model);
        }

        Entity entity = toJsonEntity(list);
        String path = RES_STAMP + "list";

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        String[] params = entityStr.split(",");
        List<String> ret = Arrays.asList(params);

        return ret;
    }
    
    /**
     * Stampを保存する。
     * @param model StampModel
     * @return 保存件数
     */
    public String putStamp(StampModel model) throws Exception {

        // キャッシュに登録する
        stampCache.put(model.getId(), model);

        Entity entity = toJsonEntity(model);
        String path = RES_STAMP + "id";

        Response response = getWebTarget()
                .path(path)   
                .request(MEDIATYPE_TEXT_UTF8)
                .put(entity);

        int status = checkHttpStatus(response);
        String entityStr = response.readEntity(String.class);
        debug(status, entityStr);
        
        response.close();

        return entityStr;
    }

    /**
     * Stampを置き換える。
     * @param model
     * @return
     * @throws Exception
     */
    public String replaceStamp(StampModel model) throws Exception {
        
        // キャッシュを更新する
        stampCache.put(model.getId(), model);
        
        return putStamp(model);
    }
    
    /**
     * Stampを取得する。
     * @param stampId 取得する StampModel の id
     * @return StampModel
     */
    public StampModel getStamp(String stampId) throws Exception {
        
        // StampModelのキャッシュを参照する
        StampModel ret = stampCache.get(stampId);
        if (ret != null) {
            return ret;
        }

        String path = RES_STAMP + "id/" +  stampId;

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        ret = (StampModel)
                getConverter().fromJson(is, StampModel.class);
        
        response.close();

        // キャッシュに登録する
        stampCache.put(stampId, ret);

        return ret;
    }
    
    /**
     * Stampを取得する。
     * @param stampId 取得する StampModel の id
     * @return StampModel
     */
    public List<StampModel> getStamps(List<ModuleInfoBean> list) throws Exception {
        
        // キャッシュにあるか調べる
        List<ModuleInfoBean> infosToGet = new ArrayList<ModuleInfoBean>();
        for (ModuleInfoBean info : list) {
            if (!stampCache.containsKey(info.getStampId())) {
                infosToGet.add(info);
            }
        }
        
        if (!infosToGet.isEmpty()) {
            String path = RES_STAMP + "list";
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (ModuleInfoBean info : infosToGet) {
                if (!first) {
                    sb.append(CAMMA);
                } else {
                    first = false;
                }
                sb.append(info.getStampId());
            }

            Response response = getWebTarget()
                    .path(path)
                    .queryParam(IDS, sb.toString())
                    .request(MEDIATYPE_JSON_UTF8)
                    .get();

            checkHttpStatus(response);
            InputStream is = response.readEntity(InputStream.class);
            TypeReference typeRef = new TypeReference<List<StampModel>>(){};
            List<StampModel> smList = (List<StampModel>)
                        getConverter().fromJson(is, typeRef);
            
            response.close();

            // キャッシュに登録する
            for (StampModel sm : smList) {
                stampCache.put(sm.getId(), sm);
            }
        }
        
        // キャッシュを参照してStampModel Listを返す
        List<StampModel> ret = new ArrayList<StampModel>();
        for (ModuleInfoBean info : list) {
            ret.add(stampCache.get(info.getStampId()));
        }
        
        return ret;
    }
    
    /**
     * Stampを削除する。
     * @param stampId 削除する StampModel の id
     * @return 削除件数
     */
    public int removeStamp(String stampId) throws Exception {
        
        // キャッシュから削除する
        stampCache.remove(stampId);

        String path = RES_STAMP + "id/" + stampId;

        Response response = getWebTarget()
                .path(path)
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();

        return 1;
    }
    
    /**
     * Stampを削除する。
     * @param stampId 削除する StampModel の id
     * @return 削除件数
     */
    public int removeStamps(List<String> ids) throws Exception {
        
        // キャッシュから削除する
        for (String stampId : ids) {
            stampCache.remove(stampId);
        }

        String path = RES_STAMP + "list";

        Response response = getWebTarget()
                .path(path)
                .queryParam(IDS, getConverter().fromList(ids))
                .request()
                .delete();

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();

        return ids.size();
    }
    
//masuda^
    // URL文字数制限回避
    public int postRemoveStamps(List<String> ids) throws Exception {
        
        // キャッシュから削除する
        for (String stampId : ids) {
            stampCache.remove(stampId);
        }

        String path = RES_STAMP + "postRemoveStamps";
        Entity entity = toJsonEntity(ids);

        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_TEXT_UTF8)
                .post(entity);

        int status = checkHttpStatus(response);
        debug(status, "delete response");
        
        response.close();
        
        int cnt = Integer.valueOf(response.readEntity(String.class));
        
        return cnt;
    }
    
    // すべてのStampModelを取得する
    public List<StampModel> getAllStamps(long userId) throws Exception {
        
        String path = RES_STAMP + "allStamps/" + String.valueOf(userId);
        
        Response response = getWebTarget()
                .path(path)
                .request(MEDIATYPE_JSON_UTF8)
                .get();

        checkHttpStatus(response);
        InputStream is = response.readEntity(InputStream.class);
        TypeReference typeRef = new TypeReference<List<StampModel>>(){};
        List<StampModel> smList = (List<StampModel>) 
                getConverter().fromGzippedJson(is, typeRef);
        
        response.close();
        
        return smList;
    }
//masuda$
    
    @Override
    protected void debug(int status, String entity) {
        if (debug || DEBUG) {
            super.debug(status, entity);
        }
    }
}
