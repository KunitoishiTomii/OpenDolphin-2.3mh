package open.dolphin.tr;

import java.io.Serializable;
import open.dolphin.infomodel.SchemaModel;

/**
 * SchemaList
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public class SchemaList implements Serializable {

    private final SchemaModel[] schemaList;

    public SchemaList(SchemaModel[] schemaList) {
        this.schemaList = schemaList;
    }
    
    public SchemaModel[] getSchemaList() {
        return schemaList;
    }
}