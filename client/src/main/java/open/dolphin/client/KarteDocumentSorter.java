/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package open.dolphin.client;

import java.util.Comparator;
import open.dolphin.infomodel.DocInfoModel;

/**
 *
 * @author buntaro
 */
public class KarteDocumentSorter implements Comparator<DocInfoModel>{
    private boolean bSortMethod;

    KarteDocumentSorter(boolean arg_SortMethod){
        bSortMethod = arg_SortMethod;
    }
    
    @Override
    public int compare(DocInfoModel d1, DocInfoModel d2){
        int i;
        // 関数名のcompareは決まりごとである。
        // s1がs2より先に来るなら、s1の特徴量がs2のそれより小さくなるような関数を作成する。
        i =(int) (d1.getFirstConfirmDate().compareTo(d2.getFirstConfirmDate()));
        if (i == 0){
            i =(int) (d1.getDocPk() - d2.getDocPk());
        }
        if(bSortMethod == false){
            i = i*(-1);
        }
        return i;
    }
};
