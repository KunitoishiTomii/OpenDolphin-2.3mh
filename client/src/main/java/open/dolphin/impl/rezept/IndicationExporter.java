package open.dolphin.impl.rezept;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.xml.bind.JAXB;
import open.dolphin.client.BlockGlass;
import open.dolphin.delegater.MasudaDelegater;
import open.dolphin.impl.rezept.model.JaxbList;
import open.dolphin.infomodel.IndicationItem;
import open.dolphin.infomodel.IndicationModel;
import open.dolphin.project.Project;

/**
 * IndicationExporter
 *
 * @author masuda, Masuda Naika
 */
public class IndicationExporter {

    private final RezeptViewer rezeptViewer;
    
    public IndicationExporter(RezeptViewer rezeptViewer) {
        this.rezeptViewer = rezeptViewer;
    }

    // XML出力
    public void exportToFile() {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setDialogTitle("適応症データエクスポート");
        File current = fileChooser.getCurrentDirectory();
        fileChooser.setSelectedFile(new File(current.getPath(), "indication.xml"));
        int selected = fileChooser.showSaveDialog(rezeptViewer.getRezeptView());
        final BlockGlass blockGlass = rezeptViewer.getBlockGlass();

        if (selected == JFileChooser.APPROVE_OPTION) {
            final Path path = fileChooser.getSelectedFile().toPath();
            if (!Files.exists(path) || overwriteConfirmed(path)) {
                blockGlass.setText("適応症データをエクスポート中です。");
                blockGlass.block();

                SwingWorker worker = new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() throws Exception {

                        List<String> srycds = Collections.emptyList();
                        List<IndicationModel> list = MasudaDelegater.getInstance().getIndicationList(srycds);

                        Charset cs = Charset.forName("UTF-8");
                        try (BufferedWriter writer = Files.newBufferedWriter(path, cs)) {
                            JaxbList<IndicationModel> jaxbList = new JaxbList<>(list);
                            JAXB.marshal(jaxbList, writer);
                            writer.close();
                        } catch (Exception ex) {
                        }

                        return null;
                    }

                    @Override
                    protected void done() {
                        blockGlass.unblock();
                    }
                };
                worker.execute();
            }
        }
    }

    /**
     * ファイル上書き確認ダイアログを表示する。
     *
     * @param file 上書き対象ファイル
     * @return 上書きOKが指示されたらtrue
     */
    private boolean overwriteConfirmed(Path path) {

        String title = "上書き確認";
        String message = "既存のファイル " + path.getFileName().toString() + "\n"
                + "を上書きしようとしています。続けますか？";

        int confirm = JOptionPane.showConfirmDialog(
                rezeptViewer.getRezeptView(), message, title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

        return confirm == JOptionPane.OK_OPTION;
    }

    // XML入力
    public void importFromFile() {

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setDialogTitle("適応症データインポート");
        File current = fileChooser.getCurrentDirectory();
        fileChooser.setSelectedFile(new File(current.getPath(), "indication.xml"));
        int selected = fileChooser.showOpenDialog(rezeptViewer.getRezeptView());
        final BlockGlass blockGlass = rezeptViewer.getBlockGlass();

        if (selected == JFileChooser.APPROVE_OPTION) {

            final Path path = fileChooser.getSelectedFile().toPath();
            blockGlass.setText("適応症データをインポート中です。");
            blockGlass.block();

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {

                    Charset cs = Charset.forName("UTF-8");
                    try (BufferedReader reader = Files.newBufferedReader(path, cs)) {
                        JaxbList<IndicationModel> jaxbList = JAXB.unmarshal(reader, JaxbList.class);
                        List<IndicationModel> list = jaxbList.getList();
                        toPersistAsNew(list);
                        MasudaDelegater.getInstance().importIndicationModels(list);

                    } catch (IOException ex) {
                    }

                    return null;
                }

                @Override
                protected void done() {
                    blockGlass.unblock();
                }

            };
            worker.execute();

        }
    }
    
    // 関係構築
    private void toPersistAsNew(List<IndicationModel> list) {
        
        String fid = Project.getFacilityId();
        for (IndicationModel model : list) {
            model.setId(0);
            model.setFacilityId(fid);
            //model.setLock(false);
            List<IndicationItem> items = model.getIndicationItems();
            if (items != null) {
                for (IndicationItem item : items) {
                    item.setId(0);
                    item.setIndicationModel(model);
                }
            }
        }
    }
    
}
