package open.dolphin.toucha;

import java.awt.Dimension;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import open.dolphin.common.util.BeanUtils;
import open.dolphin.common.util.ModuleBeanDecoder;
import open.dolphin.common.util.SchemaNumberComparator;
import open.dolphin.common.util.SimpleXmlWriter;
import open.dolphin.common.util.StampHtmlRenderer;
import open.dolphin.common.util.StampRenderingHints;
import open.dolphin.infomodel.*;

/**
 * KarteHtmlRenderer
 * 
 * @author masuda, Masuda Naika
 */
public class KarteHtmlRenderer {

    private static final String COMPONENT_ELEMENT_NAME = "component";
    private static final String TEXT_ELEMENT_NAME = "text";
    private static final String STAMP_HOLDER = "stampHolder";
    private static final String SCHEMA_HOLDER = "schemaHolder";
    private static final String NAME_NAME = "name";
    
    private static final String CR = "\n";
    private static final String TAG_DIV = "div";
    private static final String TAG_H4 = "h4";
    private static final String ATTR_ALIGN = "align";
    private static final String ATTR_STYLE = "style";
    
    private static final Dimension imageSize = new Dimension(192, 192);
    
    
    private static final KarteHtmlRenderer instance;
    
    static {
        instance = new KarteHtmlRenderer();
    }
    
    private KarteHtmlRenderer() {
    }
    
    public static KarteHtmlRenderer getInstance() {
        return instance;
    }

    /**
     * DocumentModel をレンダリングする。
     *
     * @param model レンダリングする DocumentModel
     */
    public String render(DocumentModel model) {

        List<ModuleModel> modules = model.getModules();

        // SOA と P のモジュールをわける
        // また夫々の Pane の spec を取得する
        List<ModuleModel> soaModules = new ArrayList<>();
        List<ModuleModel> pModules = new ArrayList<>();
        List<SchemaModel> schemas = model.getSchema();
        String soaSpec = null;
        String pSpec = null;

        for (ModuleModel bean : modules) {
            
            //bean.setModel((IModuleModel) BeanUtils.xmlDecode(bean.getBeanBytes()));
            bean.setModel(ModuleBeanDecoder.getInstance().decode(bean.getBeanBytes()));
            // メモリ節約？　→　ダメ！　Detachしてない
            //bean.setBeanBytes(null);

            String role = bean.getModuleInfoBean().getStampRole();
            if (role != null) {
                switch (role) {
                    case IInfoModel.ROLE_SOA:
                        soaModules.add(bean);
                        break;
                    case IInfoModel.ROLE_SOA_SPEC:
                        soaSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                        break;
                    case IInfoModel.ROLE_P:
                        pModules.add(bean);
                        break;
                    case IInfoModel.ROLE_P_SPEC:
                        pSpec = ((ProgressCourse) bean.getModel()).getFreeText();
                        break;
                }
            }
        }

        // 念のためソート
        Collections.sort(soaModules);
        Collections.sort(pModules);
        if (schemas != null) {
            Collections.sort(schemas, new SchemaNumberComparator());
        }
        
        SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.KARTE_DATE_FORMAT);
        String docDate = frmt.format(model.getStarted());
        String title = model.getDocInfoModel().getTitle();
        
        SimpleXmlWriter writer = new SimpleXmlWriter();
        writer.setRepcaceXmlChar(true);
        writer.setReplaceZenkaku(false);
        
        writer.writeStartElement(TAG_H4)
                .writeAttribute(ATTR_STYLE, "background-color:#cccccc")
                .writeAttribute(ATTR_ALIGN, "center")
                .writeCharacters(docDate)
                .writeBR()
                .writeCharacters(title)
                .writeEndElement();
        
        // SOA Pane をレンダリングする
        new KartePaneRenderer_StAX().renderPane(soaSpec, soaModules, schemas, writer);

        // P Pane をレンダリングする
        if (pSpec != null) {
            writer.writeHR();
            new KartePaneRenderer_StAX().renderPane(pSpec, pModules, schemas, writer);
        }
        
        return writer.getProduct();
    }

    private class KartePaneRenderer_StAX {

        private SimpleXmlWriter writer;
        private List<ModuleModel> modules;
        private List<SchemaModel> schemas;
        private boolean componentFlg;

        /**
         * TextPane Dump の XML を解析する。
         *
         * @param xml TextPane Dump の XML
         */
        private void renderPane(String xml, List<ModuleModel> modules, List<SchemaModel> schemas, SimpleXmlWriter writer) {
            
            this.modules = modules;
            this.schemas = schemas;
            this.writer = writer;
            writer.writeStartElement(TAG_DIV);

            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = null;

            try (StringReader stream = new StringReader(xml)) {

                reader = factory.createXMLStreamReader(stream);

                while (reader.hasNext()) {
                    int eventType = reader.next();
                    switch (eventType) {
                        case XMLStreamReader.START_ELEMENT:
                            startElement(reader);
                            break;
                    }
                }

            } catch (XMLStreamException ex) {
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (XMLStreamException ex) {
                }
            }
        }
        

        private void startElement(XMLStreamReader reader) throws XMLStreamException {
            
            String eName = reader.getName().getLocalPart();
            
            if (eName == null) {
                return;
            }
            
            switch (eName) {
                case TEXT_ELEMENT_NAME:
                    String text = reader.getElementText();
                    // Component直後の改行を消す
                    if (componentFlg && text.startsWith(CR)) {
                        text = text.substring(1);
                    }
                    componentFlg = false;
                    startContent(text);
                    break;
                case COMPONENT_ELEMENT_NAME:
                    componentFlg = true;
                    String name = reader.getAttributeValue(null, NAME_NAME);
                    String number = reader.getAttributeValue(null, COMPONENT_ELEMENT_NAME);
                    startComponent(name, number);
                    break;
                default:
                    break;
            }
        }

        private void startContent(String text) {

            // テキストを挿入する
            writer.writeCrReplaceCharacters(text);
        }
        
        private void startComponent(String name, String number) {

            if (name == null) {
                return;
            }
            
            int index = Integer.parseInt(number);
            switch (name) {
                case STAMP_HOLDER:
                    ModuleModel stamp = modules.get(index);
                    StampRenderingHints hints = StampRenderingHints.getInstance();
                    StampHtmlRenderer stampRen = new StampHtmlRenderer(stamp, hints);
                    writer.writeRawCharacters(stampRen.getStampHtml(false));
                    break;
                case SCHEMA_HOLDER:
                    SchemaModel schema = schemas.get(index);
                    ImageHtmlRenderer imgRen = new ImageHtmlRenderer();
                    writer.writeRawCharacters(imgRen.getImageHtml(schema.getJpegByte(), imageSize));
                    break;
            }

        }
    }
}