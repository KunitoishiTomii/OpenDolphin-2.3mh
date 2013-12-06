package open.dolphin.order;

/**
 * StampEditorで使う定数群
 * AbstractStampEditorから分離した
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public interface StampEditorConst {
    
    public static final String VALUE_PROP = "value";
    public static final String VALIDA_DATA_PROP = "validData";
    public static final String EMPTY_DATA_PROP = "emptyData";
    public static final String EDIT_END_PROP = "editEnd";
    public static final String CURRENT_SHINKU_PROP = "currentShinkuProp";

    public static final String DEFAULT_NUMBER = "1";

    public static final String DEFAULT_STAMP_NAME     = "新規スタンプ";
    public static final String FROM_EDITOR_STAMP_NAME = "エディタから";

    public static final String[] MED_COST_FLGAS = {"廃","金","都","","","","","減","不"};
    public static final String[] TOOL_COST_FLGAS = {"廃","金","都","","","%加","","","","乗"};
    public static final String[] TREAT_COST_FLGAS = {"廃","金","","+点","都","%加","%減","減","-点"};
    public static final String[] IN_OUT_FLAGS = {"入外","入","外"};
    public static final String[] HOSPITAL_CLINIC_FLAGS = {"病診","病","診"};
    public static final String[] OLD_FLAGS = {"社老","社","老"};

    public static final String ADMIN_MARK = "[用法] ";
    public static final String REG_ADMIN_MARK = "\\[用法\\] ";

    public static final int START_NUM_ROWS = 20;
    
    // 組み合わせができるマスター項目
    public static final String REG_BASE_CHARGE           = "[手そ]";
    public static final String REG_INSTRACTION_CHARGE    = "[手そ薬材]";     // 在宅で薬剤、材料を追加
    public static final String REG_MED_ORDER             = "[薬用材そ]";     // 保険適用外の医薬品等追加
    public static final String REG_INJECTION_ORDER       = "[手そ注材]";
    public static final String REG_TREATMENT             = "[手そ薬材]";
    public static final String REG_SURGERY_ORDER         = "[手そ薬材]";
    public static final String REG_BACTERIA_ORDER        = "[手そ薬材]";
    public static final String REG_PHYSIOLOGY_ORDER      = "[手そ薬材]";
    public static final String REG_LABO_TEST             = "[手そ薬材]";
    public static final String REG_RADIOLOGY_ORDER       = "[手そ薬材部]";
    public static final String REG_OTHER_ORDER           = "[手そ薬材]";
    public static final String REG_GENERAL_ORDER         = "[手そ薬材用部]";

    // セットできる診療行為区分
    public static final String SHIN_BASE_CHARGE           = "^(11|12)";
    public static final String SHIN_INSTRACTION_CHARGE    = "^(13|14)";
    public static final String SHIN_MED_ORDER             = "";              // 210|220|230
    public static final String SHIN_INJECTION_ORDER       = "^3";            // 310|320|330
    public static final String SHIN_TREATMENT             = "^4";
    public static final String SHIN_SURGERY_ORDER         = "^5";
    public static final String SHIN_BACTERIA_ORDER        = "^6";
    public static final String SHIN_PHYSIOLOGY_ORDER      = "^6";
    public static final String SHIN_LABO_TEST             = "^6";
    public static final String SHIN_RADIOLOGY_ORDER       = "^7";
    public static final String SHIN_OTHER_ORDER           = "^8";
    public static final String SHIN_GENERAL_ORDER         = "\\d";

    // エディタに表示する名前
    public static final String NAME_BASE_CHARGE           = "診断料";
    public static final String NAME_INSTRACTION_CHARGE    = "管理料 ";       // 指導・在宅
    public static final String NAME_MED_ORDER             = "処 方";
    public static final String NAME_INJECTION_ORDER       = "注 射";
    public static final String NAME_TREATMENT             = "処 置";
    public static final String NAME_SURGERY_ORDER         = "手 術";
    public static final String NAME_BACTERIA_ORDER        = "細菌検査";
    public static final String NAME_PHYSIOLOGY_ORDER      = "生理・内視鏡検査";
    public static final String NAME_LABO_TEST             = "検体検査";
    public static final String NAME_RADIOLOGY_ORDER       = "放射線";
    public static final String NAME_OTHER_ORDER           = "その他";
    public static final String NAME_GENERAL_ORDER         = "汎 用";

    // 暗黙の診療行為区分
    public static final String IMPLIED_BASE_CHARGE           = "";
    public static final String IMPLIED_INSTRACTION_CHARGE    = "";
    public static final String IMPLIED_MED_ORDER             = "";
    public static final String IMPLIED_INJECTION_ORDER       = "";
    public static final String IMPLIED_TREATMENT             = "400";
    public static final String IMPLIED_SURGERY_ORDER         = "";
    public static final String IMPLIED_BACTERIA_ORDER        = "600";
    public static final String IMPLIED_PHYSIOLOGY_ORDER      = "600";
    public static final String IMPLIED_LABO_TEST             = "600";
    public static final String IMPLIED_RADIOLOGY_ORDER       = "700";
    public static final String IMPLIED_OTHER_ORDER           = "800";
    public static final String IMPLIED_GENERAL_ORDER         = "";

    // 情報
    public static final String INFO_BASE_CHARGE           = "診断料（診区=110-120）";
    public static final String INFO_INSTRACTION_CHARGE    = "管理料（診区=130-140）";
    public static final String INFO_MED_ORDER             = "処 方";
    public static final String INFO_INJECTION_ORDER       = "注 射（診区=300）";
    public static final String INFO_TREATMENT             = "処 置（診区=400）";
    public static final String INFO_SURGERY_ORDER         = "手 術（診区=500）";
    public static final String INFO_BACTERIA_ORDER        = "細菌検査（診区=600）";
    public static final String INFO_PHYSIOLOGY_ORDER      = "生理・内視鏡検査（診区=600）";
    public static final String INFO_LABO_TEST             = "検体検査（診区=600）";
    public static final String INFO_RADIOLOGY_ORDER       = "放射線（診区=700）";
    public static final String INFO_OTHER_ORDER           = "その他（診区=800）";
    public static final String INFO_GENERAL_ORDER         = "汎 用（診区=100-999）";

    // 病名
    public static final String NAME_DIAGNOSIS             = "傷病名";
    public static final String REG_DIAGNOSIS              = "[手そ薬材用部]";

    // 辞書のキー
    public static final String KEY_ORDER_NAME    = "orderName";
    public static final String KEY_PASS_REGEXP   = "passRegExp";
    public static final String KEY_SHIN_REGEXP   = "shinkuRegExp";
    public static final String KEY_INFO          = "info";
    public static final String KEY_IMPLIED       = "implied007";

    // 編集可能コメント
    public static final String EDITABLE_COMMENT_81   = "81";;   //"810000001";
    public static final String EDITABLE_COMMENT_0081 = "0081";
    public static final String EDITABLE_COMMENT_83   = "83";
    public static final String EDITABLE_COMMENT_0083 = "0083";
    public static final String EDITABLE_COMMENT_84   = "84";
    public static final String EDITABLE_COMMENT_0084 = "0084";
    public static final String EDITABLE_COMMENT_85   = "85";
    public static final String EDITABLE_COMMENT_0085 = "0085";  //"008500000";

    // 検索特殊記号文字
    public static final String ASTERISK_HALF = "*";
    public static final String ASTERISK_FULL = "＊";
    public static final String TENSU_SEARCH_HALF = "///";
    public static final String TENSU_SEARCH_FULL = "／／／";
    public static final String COMMENT_SEARCH_HALF = "8";
    public static final String COMMENT_SEARCH_FULL = "８";
    public static final String COMMENT_85_SEARCH_HALF = "85";
    public static final String COMMENT_85_SEARCH_FULL = "８５";

    // 検索タイプ
    public static final int TT_INVALID       = -1;
    public static final int TT_LIST_TECH     = 0;
    public static final int TT_TENSU_SEARCH  = 1;
    public static final int TT_85_SEARCH     = 2;
    public static final int TT_CODE_SEARCH   = 3;
    public static final int TT_LETTER_SEARCH = 4;
    public static final int TT_SHINKU_SERACH = 5;
    
    // Editor button
    public static final String STAMP_EDITOR_BUTTON_TYPE = "stamp.editor.buttonType";
    public static final String BUTTON_TYPE_IS_ICON = "icon";
    public static final String BUTTON_TYPE_IS_ITEXT = "text";

    // ORCA 有効期限用のDF
    public static final String effectiveFormat = "yyyyMMdd";
    
    public static final String CLAIM_007 = "Claim007";
    public static final String CLAIM_003 = "Claim003";
}
