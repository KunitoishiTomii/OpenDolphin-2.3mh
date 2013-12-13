package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

/**
 * IStampModel
 * StampModelの実体となるものと定義
 * 
 * @author masuda, Masuda Naika
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
    @Type(value = BundleDolphin.class),
    @Type(value = BundleMed.class),
    @Type(value = ClaimBundle.class),
    @Type(value = RegisteredDiagnosisModel.class),
    @Type(value = TextStampModel.class)})
public interface IStampModel extends Serializable {
    
}
