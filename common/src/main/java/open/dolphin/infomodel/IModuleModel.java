package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

/**
 * IModuleModel
 * ModuleModelのモデル実体になりうるものと定義 test
 *
 * @author masuda, Masuda Naika
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BundleDolphin.class),
    @JsonSubTypes.Type(value = BundleMed.class),
    //@JsonSubTypes.Type(value = ClaimBundle.class),
    @JsonSubTypes.Type(value = ProgressCourse.class)})
public interface IModuleModel extends Serializable {

}