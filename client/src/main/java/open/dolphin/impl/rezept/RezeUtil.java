package open.dolphin.impl.rezept;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import open.dolphin.client.ClientContext;

/**
 * RezeUtil
 * 
 * @author masuda, Masuda Naika
 */
public class RezeUtil {
    
    private static final Charset ENCODING = StandardCharsets.UTF_8;
    private static final String CAMMA = ",";
    private static final String SRYCD_FRMT = "000000000";
    private static final String DATE_FRMT = "Gyy.MM.dd";
    private static final String YM_FRMT = "GYY.MM";
    private static final Locale JP_LOCALE = new Locale("ja","JP","JP");
    private static final String[] PREFECTURES = {
        "北海道", "青森", "岩手", "宮城", "秋田", "山形", "福島", "茨城", "栃木", "群馬",
        "埼玉", "千葉", "東京", "神奈川", "新潟", "富山", "石川", "福井", "山梨", "長野",
        "岐阜", "静岡", "愛知", "三重", "滋賀", "京都", "大阪", "兵庫", "奈良", "和歌山",
        "鳥取", "島根", "岡山", "広島", "山口", "徳島", "香川", "愛媛", "高知", "福岡",
        "佐賀", "長崎", "熊本", "大分", "宮崎", "鹿児島", "沖縄"};
    private static final String[] TO_UNITS = {
        "分", "回", "種", "箱", "巻", "枚", "本", "組", "セット", "個",
        "裂", "方向", "トローチ", "アンプル", "カプセル", "錠", "丸", "包", "瓶", "袋",
        "瓶（袋）", "管", "シリンジ", "回分", "テスト分", "ガラス筒", "桿錠", "単位", "万単位", "フィート",
        "滴", "mg", "g", "kg", "cc", "mL", "L", "mLV", "バイアル", "cm",
        "cm2", "m", "uCi", "mCi", "ug", "管（瓶）", "筒", "GBq", "MBq", "KBq",
        "キット", "国際単位", "患者当り", "気圧", "缶", "手術当り", "容器", "mL(g)", "ブリスター", "シート"};
    
    private Map<Integer, String> SHAHO_TYPE;
    private Map<Integer, String> KOKUHO_TYPE;
     
    private static final RezeUtil instance;

    static {
        instance = new RezeUtil();

    }

    private RezeUtil() {
        init();
    }

    public static final RezeUtil getInstance() {
        return instance;
    }

    private void init() {
        
        SHAHO_TYPE = new HashMap<>();
        KOKUHO_TYPE = new HashMap<>();
        
        try (InputStream is = ClientContext.getResourceAsStream("reze/rezeType.csv");
                BufferedReader br = new BufferedReader(new InputStreamReader(is, ENCODING))) {
            
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (line.isEmpty() || line.startsWith("コード")) {
                    continue;
                }
                String[] tokens = line.split(CAMMA);
                int code = Integer.parseInt(tokens[0]);
                String shahoType = tokens[1];
                SHAHO_TYPE.put(code, shahoType);
                if (tokens.length > 2) {
                    String kokuhoType = tokens[2];
                    KOKUHO_TYPE.put(code, kokuhoType);
                }
            }
            
        } catch (IOException ex) {
        }
    }
    
    public String getSexDesc(String sex) {
        sex = sex.trim();
        return "1".equals(sex) ? "男" : "女";
    }
    
    public String getSrycdStr(int srycd) {
        DecimalFormat frmt = new DecimalFormat(SRYCD_FRMT);
        return frmt.format(srycd);
    }
    
    public String getDateStr(Date date) {
        SimpleDateFormat frmt = new SimpleDateFormat(DATE_FRMT, JP_LOCALE);
        return frmt.format(date);
    }
    public String getYMStr(Date date) {
        SimpleDateFormat frmt = new SimpleDateFormat(YM_FRMT, JP_LOCALE);
        return frmt.format(date);
    }
    
    public Date fromYearMonthDate(String str) {
        str = str.trim();
        if (str.isEmpty()) {
            return null;
        }
        String nengo = str.substring(0, 1);
        String year = str.substring(1, 3);
        String month = str.substring(3, 5);
        String day = str.substring(5, 7);
        StringBuilder sb = new StringBuilder();
        switch (nengo) {
            case "1":
                sb.append("M");
                break;
            case "2":
                sb.append("T");
                break;
            case "3":
                sb.append("S");
                break;
            case "4":
                sb.append("H");
                break;
            default:
                sb.append(nengo);
                break;
        }
        sb.append(year);
        sb.append(".").append(month);
        sb.append(".").append(day);

        SimpleDateFormat frmt = new SimpleDateFormat(DATE_FRMT, JP_LOCALE);
        try {
            return frmt.parse(sb.toString());
        } catch (ParseException ex) {
        }
        return null;
    }
    
    public Date fromYearMonth(String str) {
        return fromYearMonthDate(str.trim() + "01");
    }

    public String getPrefecture(int code) {
        return PREFECTURES[code -1];
    }

    public String getTenTable(int tenTable) {
        return tenTable == 1 ? "医科" : "";
    }

    public String getRezeTypeDesc(int num, int rezeType) {
        switch (num) {
            case 1:
                return SHAHO_TYPE.get(rezeType);
            case 2:
            case 6:
                return KOKUHO_TYPE.get(rezeType);
            default:
                return "Unkonwn";
        }
    }
    
    public String getTOUnit(int unit) {
        return TO_UNITS[unit - 1];
    }

}
