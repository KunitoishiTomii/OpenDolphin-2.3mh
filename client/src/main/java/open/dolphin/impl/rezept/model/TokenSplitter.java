package open.dolphin.impl.rezept.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 自前でsplitを実装
 * 
 * @author masuda, Masuda Naika
 */
public class TokenSplitter {

    public static String[] split(String csv) {

        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int len = csv.length();
        for (int i = 0; i < len; ++i) {
            char c = csv.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                    break;
                case ',':
                    list.add(sb.toString());
                    sb.setLength(0);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        list.add(sb.toString());

        return list.toArray(new String[list.size()]);
    }
}
