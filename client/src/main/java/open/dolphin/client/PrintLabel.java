package open.dolphin.client;

import open.dolphin.common.util.StampRenderingHints;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingWorker;
import open.dolphin.infomodel.*;
import open.dolphin.project.Project;
import open.dolphin.setting.MiscSettingPanel;
import open.dolphin.util.MMLDate;
import open.dolphin.common.util.StringTool;

/**
 * Brother QL-580Nで処方注射ラベルを印刷する
 * ウチはしばし紙カルテと併用なので役に立つかも？？
 * 62mmロール紙きめうち。半角文字の処理は甘い。
 * port9100にesc/pのデータをそのまま送るのでwindowsにドライバは必要ありません
 *
 * @author masuda, Masuda Naika
 */
public class PrintLabel {

    // QL-580Nの全角最大桁数(全角32dot fontで半角換算の桁数)
    private static final int maxColumn = 42;
    private static final String ENCODING = "JIS";

    // esc/pコマンド関連
    private static final byte[] escpInitialize = {0x1b, 0x40};
    private static final byte[] escpCmdModeChange = {0x1b, 0x69, 0x61, 0x00};    // esc/pモード
    private static final byte[] escpKISO = {0x1c, 0x26, 0x12};
    private static final byte[] escpKOSI = {0x1c, 0x2e, 0x0f};
    private static final byte[] escpFF = {0x0c};
    // JISコードに変換時のKI/KO
    private static final byte[] KI = {0x1b, 0x24, 0x42};
    //private static final byte[] KO = {0x1b, 0x28, 0x42};

    private final List<LineModel> lineData = new ArrayList<>();
    private final List<ModuleModel> rpStamp = new ArrayList<>();
    private final List<ModuleModel> exStamp = new ArrayList<>();
    private final List<ModuleModel> otherStamp = new ArrayList<>();
    private final List<ModuleModel> otherStamp2 = new ArrayList<>();
    private final List<ModuleModel> injStamp = new ArrayList<>();
    private List<StampHolder> stampHolders = new ArrayList<>();
    private KartePane kartePane;

    private String date;
    
    private final StampRenderingHints hints;
    

    public PrintLabel() {
        hints = StampRenderingHints.getInstance();
    }

    public void enter(KartePane kp) {
        // JISで送っていたが、毎文字にKI/KOを付加していたので、半角全角変更時
        // 適宜KI/KOを送信するように変更した。

        kartePane = kp;

        collectMedStampHolder();
        collectModuleModel();
        buildLineDataArray();
        sendRawPrintData();
    }

    public void enter2(List<StampHolder> al) {
        // スタンプホルダのpopupから実行した場合
        if (al.isEmpty()) {
            return;
        }

        kartePane = al.get(0).getKartePane();
        stampHolders = al;
        setDate();

        collectModuleModel();
        buildLineDataArray();
        sendRawPrintData();
    }

    private void sendRawPrintData() {
        try {
            String str = buildPrintString();
            byte[] rawData = makeRawData(str);
            sendData(rawData);
        } catch (UnsupportedEncodingException ex) {
        }
    }

    private void collectMedStampHolder() {

        KarteStyledDocument doc = (KarteStyledDocument) kartePane.getTextPane().getDocument();
        List<StampHolder> list = doc.getStampHolders();
        for (StampHolder sh : list) {
            String entity = sh.getStamp().getModuleInfoBean().getEntity();
            if (IInfoModel.ENTITY_MED_ORDER.equals(entity) 
                    || IInfoModel.ENTITY_INJECTION_ORDER.equals(entity)) {
                stampHolders.add(sh);
            }
        }
        setDate();
    }

    private void setDate() {
        // ラベルの日付を、一個目のスタンプからDocInfoを調べて取得する。
        date = MMLDate.getDate();
        if (!stampHolders.isEmpty()) {
            Chart chart = stampHolders.get(0).getKartePane().getParent().getContext();
            KarteEditor editor = chart.getKarteEditor();
            if (editor.getModel().getDocInfoModel().getParentId() != null) {     // 新規作成でなかったら
                date = editor.getModel().getDocInfoModel().getFirstConfirmDateTrimTime();

            }
        }
    }


    private void collectModuleModel() {
        for (StampHolder sh : stampHolders) {
            ModuleModel stamp = sh.getStamp();
            String entity = stamp.getModuleInfoBean().getEntity();
            if (entity == null) {
                continue;
            }
            switch (entity) {
                case IInfoModel.ENTITY_MED_ORDER:
                    String rpName = stamp.getModuleInfoBean().getStampName();
                    // 順番に印刷するために定期臨時注射それぞれのArrayListに登録
                    if (rpName.contains("定期")) {
                        rpStamp.add(stamp);
                    } else if (rpName.contains("臨時")) {
                        exStamp.add(stamp);
                    } else {
                        otherStamp.add(stamp);
                    }
                    break;
                case IInfoModel.ENTITY_INJECTION_ORDER:
                    injStamp.add(stamp);
                    break;
                default:
                    otherStamp2.add(stamp);
                    break;
            }
        }
    }

    private void buildLineDataArray() {

        String name = kartePane.getParent().getContext().getPatient().getFullName() + "　様";
        // 印字桁数が限られているので削る
        if (date != null) {
            date = date.substring(2, date.length());
            date = date.replace("-", "");
        }
        // 一行目は患者名と処方日
        lineData.add(new LineModel(name, hankakuNumToZenkaku(date), "　"));

        // 定期処方・臨時処方・その他処方・注射の順で印刷データ作成
        for (ModuleModel mm : rpStamp) {
            addLineFromModule(mm);
        }
        for (ModuleModel mm : exStamp) {
            addLineFromModule(mm);
        }
        for (ModuleModel mm : otherStamp) {
            addLineFromModule(mm);
        }
        for (ModuleModel mm : injStamp) {
            addLineFromModule(mm);
        }
        for (ModuleModel mm : otherStamp2) {
            addLineFromModule(mm);
        }
    }

    private void addLineFromModule(ModuleModel stamp) {
        
        String entity = stamp.getModuleInfoBean().getEntity();
        if (entity == null) {
            return;
        }
        switch (entity) {
            case IInfoModel.ENTITY_MED_ORDER: {
                // 処方の場合の処理
                BundleMed bundle = (BundleMed) stamp.getModel();
                ClaimItem[] ci = bundle.getClaimItem();
                String rpName = stamp.getModuleInfoBean().getStampName();
                rpName = rpName.replace("定期 - ", "Ｒｐ");
                rpName = rpName.replace("臨時 - ", "Ｅｘ");
                lineData.add(new LineModel(rpName, "", "─"));
                for (ClaimItem c : ci) {
                    String itemName = c.getName();
                    String unit = c.getUnit();
                    if ("カプセル".equals(unit)) {
                        unit = "Ｃ";
                    }
                        // 0085系のコメントはunitがnullなので""に置き換える。
                    // 手技の場合もclassCodeが"0"なので置き換える
                    if (unit == null || "0".equals(c.getClassCode())) {
                        unit = "";
                    }
                    String itemNumber = hankakuNumToZenkaku(c.getNumber()) + unit;
                    lineData.add(new LineModel(itemName, itemNumber, "　"));
                }
                String admin = "【" + bundle.getAdmin() + "】";
                String bundleNumber = hankakuNumToZenkaku(bundle.getBundleNumber());
                // 頓用と外用なら「何回分」にする
                boolean tonyo = bundle.getClassCode().startsWith(ClaimConst.RECEIPT_CODE_TONYO.substring(0, 2));
                boolean gaiyo = bundle.getClassCode().startsWith(ClaimConst.RECEIPT_CODE_GAIYO.substring(0, 2));
                if (tonyo || gaiyo) {
                    bundleNumber = bundleNumber + "回分";
                } else {
                    bundleNumber = bundleNumber + "日分";
                }
                lineData.add(new LineModel(admin, bundleNumber, "　"));
                String adminMemo = bundle.getAdminMemo();
                if (adminMemo != null) {
                    lineData.add(new LineModel(adminMemo, "", "　"));
                }
                break;
            }
            case IInfoModel.ENTITY_INJECTION_ORDER: {
                BundleDolphin bundle = (BundleDolphin) stamp.getModel();
                lineData.add(new LineModel("注射", "", "─"));
                ClaimItem[] ci = bundle.getClaimItem();
                for (ClaimItem c : ci) {
                    String itemName = c.getName();
                    String unit = c.getUnit();
                    if (unit != null) {
                        // 注射の薬剤はここで処理される
                        String itemNumber = hankakuNumToZenkaku(c.getNumber()) + unit;
                        lineData.add(new LineModel(itemName, itemNumber, "　"));
                    } else {
                        // 注射の手技はここで処理される
                        itemName = "【" + itemName + "】";
                        lineData.add(new LineModel(itemName, "", "　"));
                    }
                }       // 入院注射、施行日
                String bundleNum = bundle.getBundleNumber();
                if (bundleNum.startsWith("*")) {
                    String itemName = hints.parseBundleNum(bundle);
                    lineData.add(new LineModel(itemName, "", "　"));
                }
                break;
            }
            default: {
                BundleDolphin bundle = (BundleDolphin) stamp.getModel();
                lineData.add(new LineModel("", "", "─"));
                ClaimItem[] ci = bundle.getClaimItem();
                for (ClaimItem c : ci) {
                    String itemName = c.getName();
                    String unit = c.getUnit();
                    if (unit != null) {
                        String itemNumber = hankakuNumToZenkaku(c.getNumber()) + unit;
                        lineData.add(new LineModel(itemName, itemNumber, "　"));
                    } else {
                        String num = c.getNumber();
                        String itemNumber = hankakuNumToZenkaku(num);
                        if (num != null) {
                            itemNumber = "×" + itemNumber;
                        }
                        lineData.add(new LineModel(itemName, itemNumber, "　"));
                    }
                }
                break;
            }
        }
    }

    
    private String buildPrintString() throws UnsupportedEncodingException {

        StringBuilder sb = new StringBuilder();

        // 第２項目（数量）の桁数を基準に第１項目の桁数を決める
        for (LineModel model : lineData) {
            String item1 = model.getItemName();     // 薬剤名、患者名
            String item2 = model.getNumDate();      // 数量、日付
            String filler =model.getFiller();       // Filler
            int fillerLength = StringTool.getByteLength(filler);

            boolean firstLine = true;
            int linePosition = 0;
            int item2Position = maxColumn - StringTool.getByteLength(item2) - 1;
            
            for (int i = 0; i < item1.length(); ++i) {
                if (linePosition < item2Position - 2) {
                    char c = item1.charAt(i);
                    sb.append(c);
                    linePosition = linePosition + StringTool.getByteLength(c);
                }
                if (i == item1.length() - 1 || linePosition >= item2Position - 2) {
                    if ((linePosition & 1) == 1) {
                        sb.append(" ");             // 半角の調整
                        ++linePosition;
                    }
                    if (firstLine) {
                        firstLine = false;
                        // ここは項目区切りのSPCx2を含めてFillerで埋めるので(item2Position - 2 + 2)となる
                        while (linePosition < item2Position) {
                            sb.append(filler);
                            linePosition = linePosition + fillerLength;
                        }
                        sb.append(item2);
                    } else {
                        while (linePosition < maxColumn) {
                            sb.append(filler);
                            linePosition = linePosition + fillerLength;
                        }
                    }
                    sb.append("\n");
                    linePosition = 0;
                }
            }
        }
        //System.out.println(sb.toString());

        return sb.toString();
    }

    private String hankakuNumToZenkaku(String input) {
        if (input == null) {
            return "";
        }
        final String hankaku = "0123456789 ./";
        final String zenkaku = "０１２３４５６７８９　．／";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.length(); ++i) {
            int l = hankaku.indexOf(input.charAt(i));
            if (l != -1) {
                sb.append(zenkaku.charAt(l));
            } else {
                sb.append(input.charAt(i));
            }
        }
        String output = sb.toString();
        return output;
    }

    private byte[] makeRawData(String input) {

        ByteBuffer buf = ByteBuffer.allocate(10240);
        boolean kanjiMode = false;

        buf.put(escpInitialize);
        buf.put(escpCmdModeChange);
        buf.put(escpKOSI);

        char[] charArray = input.toCharArray();
        for (char c : charArray) {
            byte[] bytes = convertToJisBytes(c);
            if (bytes != null) {
                if (bytes.length > 4) {
                    byte[] ctrl = {bytes[0], bytes[1], bytes[2]};
                    if (Arrays.equals(ctrl, KI)) {
                        if (!kanjiMode) {
                            buf.put(escpKISO);
                        }
                        buf.put(bytes[3]);
                        buf.put(bytes[4]);
                        kanjiMode = true;
                    }
                } else if (bytes.length > 0){
                    if (kanjiMode) {
                        buf.put(escpKOSI);
                    }
                    buf.put(bytes[0]);
                    kanjiMode = false;
                }
            }
        }

        buf.put(escpKOSI);
        buf.put(escpFF);

        buf.flip();
        byte[] ret = new byte[buf.limit()];
        buf.get(ret);
        return ret;
    }
    private byte[] convertToJisBytes(char c) {
        
        // 全角マイナス等は自分でJISに変換
        byte[] bytes = null;
        switch (c) {
            case '－':
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x5d, 0x1b, 0x28, 0x42};
                break;
            case '～':
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x41, 0x1b, 0x28, 0x42};
                break;
            case '∥':
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x42, 0x1b, 0x28, 0x42};
                break;
            case '￠':
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x71, 0x1b, 0x28, 0x42};
                break;
            case '￡':
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x21, 0x72, 0x1b, 0x28, 0x42};
                break;
            case '￢':
                bytes = new byte[]{0x1b, 0x24, 0x42, 0x22, 0x4c, 0x1b, 0x28, 0x42};
                break;
            default:
                try {
                    String str = new String(new char[]{c});
                    bytes = str.getBytes(ENCODING);
                } catch (UnsupportedEncodingException ex) {
                }
                break;
        }
        return bytes;
    }

    private void sendData(final byte[] rawData) {
        // esc/pのraw dataをQL-580Nに転送する
        if (rawData == null || rawData.length == 0) {
            return;
        }

        final String prtAddress = Project.getString(MiscSettingPanel.LBLPRT_ADDRESS, MiscSettingPanel.DEFAULT_LBLPRT_ADDRESS);
        final int prtPort = Project.getInt(MiscSettingPanel.LBLPRT_PORT, MiscSettingPanel.DEFAULT_LBLPRT_PORT);

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                // 転送
                try (Socket socket = new Socket(prtAddress, prtPort)) {
                    socket.getOutputStream().write(rawData);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }

                return null;
            }
        };
        worker.execute();
    }


    private static class LineModel {

        private final String itemName;
        private final String numDate;
        private final String filler;

        private LineModel(String itemName, String numDate, String filler) {
            this.itemName = itemName;
            this.numDate = numDate;
            this.filler = filler;
        }

        private String getItemName() {
            return itemName;
        }

        private String getNumDate() {
            return numDate;
        }

        private String getFiller() {
            return filler;
        }
    }
}
