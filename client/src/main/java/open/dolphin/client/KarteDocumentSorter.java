/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package open.dolphin.client;

import java.util.Comparator;
import open.dolphin.infomodel.DocInfoModel;
import org.jboss.logging.Logger;

/**
 *
 * @author buntaro
 */
public class KarteDocumentSorter implements Comparator<DocInfoModel>{
    private long lSortMethod;

    KarteDocumentSorter(long arg_SortMethod){
        lSortMethod = arg_SortMethod;
    }
    
    @Override
    public int compare(DocInfoModel d1, DocInfoModel d2){
        int i;
    // 関数名のcompareは決まりごとである。
     // s1がs2より先に来るなら、s1の特徴量がs2のそれより小さくなるような関数を作成する。
     if( d1.getConfirmDate().compareTo(d2.getConfirmDate()) < 0 ){
      i = lSortMethod==0 ? 1 : -1;
     }else if( d1.getConfirmDate().compareTo(d2.getConfirmDate()) > 0){ // s1の方がs2より短い文字列になっている場合
      i = lSortMethod==0 ? -1 : 1;
     }else{
      i = 0;
     }
     Logger.getLogger(this.getClass().toString()).warn("compare ret : "+i);
     return i;
    }
};
