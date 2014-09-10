package open.dolphin.impl.rezept;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * LexicalAnalyzer
 * 
 * @author masuda, Masuda Naika
 */
public class LexicalAnalyzer {

    private static final String HN_SPC = " ";
    private static final String ZN_SPC = "　";
    private static final String AND_OPERATOR = "&";
    private static final String OR_OPERATOR = "|";
    private static final String LEFT_PARENTHESIS = "(";
    private static final String RIGHT_PARENTHESIS = ")";
    private static final String ERR_INVALID_SYNTAX = "Invalid syntax.";
    private static final String ERR_INVALID_PARENTHESIS_PAIR = "Invalid parenthesis pair.";

    private static enum TOKEN_TYPE {

        NONE, OPERATOR, LEFT_PARENTHESIS, RIGHT_PARENTHESIS, STRING_TOKEN
    }

    // 操車場アルゴリズム
    public static List<String> toPostFixNotation(List<String> tokens) throws Exception {

        List<String> out = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();

        for (String token : tokens) {

            switch (token) {
                case AND_OPERATOR:
                case OR_OPERATOR:
                    for (String top = stack.peek(); top != null; top = stack.peek()) {
                        if (AND_OPERATOR.equals(top) || OR_OPERATOR.equals(top)) {
                            stack.pop();
                            out.add(top);
                        } else {
                            break;
                        }
                    }
                    stack.push(token);
                    break;
                case LEFT_PARENTHESIS:
                    stack.push(token);
                    break;
                case RIGHT_PARENTHESIS:
                    //boolean found = false;
                    for (String top = stack.peek(); top != null; top = stack.peek()) {
                        stack.pop();
                        if (LEFT_PARENTHESIS.equals(top)) {
                            //found = true;
                            break;
                        }
                        out.add(top);
                    }
                    //if (!found) {
                    //    throw new Exception(ERR_INVALID_PARENTHESIS_PAIR);
                    //}
                    break;
                default:
                    out.add(token);
                    break;
            }
        }

        while (!stack.isEmpty()) {
            String last = stack.pop();
            //if (LEFT_PARENTHESIS.equals(last)) {
            //    throw new Exception(ERR_INVALID_PARENTHESIS_PAIR);
            //}
            out.add(last);
        }

        return out;
    }
    
    public static List<String> toPostFixNotation(String data) throws Exception {
        List<String> tokens = getTokens(data);
        return toPostFixNotation(tokens);
    }

    public static List<String> getTokens(String data) throws Exception {

        List<String> tokens = new ArrayList<>();
        data = data.replace("and", AND_OPERATOR).replace("AND", AND_OPERATOR);
        data = data.replace("or", OR_OPERATOR).replace("OR", OR_OPERATOR);

        int len = data.length();

        StringBuilder strTokenBuf = new StringBuilder();

        for (int i = 0; i < len; ++i) {
            String s = data.substring(i, i + 1);
            switch (s) {
                case AND_OPERATOR:
                case OR_OPERATOR:
                case LEFT_PARENTHESIS:
                case RIGHT_PARENTHESIS:
                    if (strTokenBuf.length() > 0) {
                        tokens.add(strTokenBuf.toString());
                        strTokenBuf.setLength(0);
                    }
                    tokens.add(s);
                    break;
                case HN_SPC:
                case ZN_SPC:
                    if (strTokenBuf.length() > 0) {
                        tokens.add(strTokenBuf.toString());
                        strTokenBuf.setLength(0);
                    }
                    break;
                default:
                    strTokenBuf.append(s);
                    break;
            }
        }

        if (strTokenBuf.length() > 0) {
            tokens.add(strTokenBuf.toString());
        }

        checkValidation(tokens);

        return tokens;
    }

    // validation
    private static void checkValidation(List<String> tokens) throws Exception {

        int leftParenthesisCount = 0;
        int rightParenthesisCount = 0;
        int strTokenCount = 0;
        int operatorCount = 0;
        TOKEN_TYPE lastTokenType = TOKEN_TYPE.NONE;

        // 構文とカウント
        for (String token : tokens) {
            switch (token) {
                case AND_OPERATOR:
                case OR_OPERATOR:
                    switch (lastTokenType) {
                        case NONE:
                        case OPERATOR:
                        case LEFT_PARENTHESIS:
                            throw new Exception(ERR_INVALID_SYNTAX);
                    }
                    operatorCount++;
                    lastTokenType = TOKEN_TYPE.OPERATOR;
                    break;
                case RIGHT_PARENTHESIS:
                    switch (lastTokenType) {
                        case NONE:
                        case OPERATOR:
                        case LEFT_PARENTHESIS:
                            throw new Exception(ERR_INVALID_SYNTAX);
                    }
                    rightParenthesisCount++;
                    lastTokenType = TOKEN_TYPE.RIGHT_PARENTHESIS;
                    break;
                case LEFT_PARENTHESIS:
                    switch (lastTokenType) {
                        case RIGHT_PARENTHESIS:
                        case STRING_TOKEN:
                            throw new Exception(ERR_INVALID_SYNTAX);
                    }
                    leftParenthesisCount++;
                    lastTokenType = TOKEN_TYPE.LEFT_PARENTHESIS;
                    break;
                default:
                    switch (lastTokenType) {
                        case RIGHT_PARENTHESIS:
                        case STRING_TOKEN:
                            throw new Exception(ERR_INVALID_SYNTAX);
                    }
                    strTokenCount++;
                    lastTokenType = TOKEN_TYPE.STRING_TOKEN;
                    break;
            }
        }
        // 演算子数
        if (operatorCount != strTokenCount - 1) {
            throw new Exception(ERR_INVALID_SYNTAX);
        }
        // 括弧数
        if (leftParenthesisCount != rightParenthesisCount) {
            throw new Exception(ERR_INVALID_PARENTHESIS_PAIR);
        }
    }
}
