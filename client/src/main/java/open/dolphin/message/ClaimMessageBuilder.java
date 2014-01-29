package open.dolphin.message;

import open.dolphin.infomodel.ClaimBundle;
import open.dolphin.infomodel.ClaimItem;
import open.dolphin.infomodel.PVTHealthInsuranceModel;
import open.dolphin.infomodel.PVTPublicInsuranceItemModel;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;

/**
 * ClaimをJDOM2で作ってみる
 *
 * @author masuda, Masuda naika
 */
public class ClaimMessageBuilder {

    private static final String URL_xhtml = "http://www.w3.org/1999/xhtml";
    private static final String URL_mmlCm = "http://www.medxml.net/MML/SharedComponent/Common/1.0";
    private static final String URL_mmlNm = "http://www.medxml.net/MML/SharedComponent/Name/1.0";
    private static final String URL_mmlFc = "http://www.medxml.net/MML/SharedComponent/Facility/1.0";
    private static final String URL_mmlDp = "http://www.medxml.net/MML/SharedComponent/Department/1.0";
    //private static final String URL_mmlAd = "http://www.medxml.net/MML/SharedComponent/Address/1.0";
    //private static final String URL_mmlPh = "http://www.medxml.net/MML/SharedComponent/Phone/1.0";
    private static final String URL_mmlPsi = "http://www.medxml.net/MML/SharedComponent/PersonalizedInfo/1.0";
    private static final String URL_mmlCi = "http://www.medxml.net/MML/SharedComponent/CreatorInfo/1.0";
    //private static final String URL_mmlPi = "http://www.medxml.net/MML/ContentModule/PatientInfo/1.0";
    //private static final String URL_mmlBc = "http://www.medxml.net/MML/ContentModule/BaseClinic/1.0";
    //private static final String URL_mmlFcl = "http://www.medxml.net/MML/ContentModule/FirstClinic/1.0";
    private static final String URL_mmlHi = "http://www.medxml.net/MML/ContentModule/HealthInsurance/1.1";
    //private static final String URL_mmlLs = "http://www.medxml.net/MML/ContentModule/Lifestyle/1.0";
    //private static final String URL_mmlPc = "http://www.medxml.net/MML/ContentModule/ProgressCourse/1.0";
    private static final String URL_mmlRd = "http://www.medxml.net/MML/ContentModule/RegisteredDiagnosis/1.0";
    //private static final String URL_mmlSg = "http://www.medxml.net/MML/ContentModule/Surgery/1.0";
    //private static final String URL_mmlSm = "http://www.medxml.net/MML/ContentModule/Summary/1.0";
    //private static final String URL_mmlLb = "http://www.medxml.net/MML/ContentModule/test/1.0";
    //private static final String URL_mmlRp = "http://www.medxml.net/MML/ContentModule/report/1.0";
    //private static final String URL_mmlRe = "http://www.medxml.net/MML/ContentModule/Referral/1.0";
    private static final String URL_mmlSc = "http://www.medxml.net/MML/SharedComponent/Security/1.0";
    private static final String URL_claim = "http://www.medxml.net/claim/claimModule/2.1";
    //private static final String URL_claimA = "http://www.medxml.net/claim/claimAmountModule/2.1";

    private static final Namespace xhtml = Namespace.getNamespace("xhtml", URL_xhtml);
    private static final Namespace mmlCm = Namespace.getNamespace("mmlCm", URL_mmlCm);
    private static final Namespace mmlNm = Namespace.getNamespace("mmlNm", URL_mmlNm);
    private static final Namespace mmlFc = Namespace.getNamespace("mmlFc", URL_mmlFc);
    private static final Namespace mmlDp = Namespace.getNamespace("mmlDp", URL_mmlDp);
    //private static final Namespace mmlAd = Namespace.getNamespace("mmlAd", URL_mmlAd);
    //private static final Namespace mmlPh = Namespace.getNamespace("mmlPh", URL_mmlPh);
    private static final Namespace mmlPsi = Namespace.getNamespace("mmlPsi", URL_mmlPsi);
    private static final Namespace mmlCi = Namespace.getNamespace("mmlCi", URL_mmlCi);
    //private static final Namespace mmlPi = Namespace.getNamespace("mmlPi", URL_mmlPi);
    //private static final Namespace mmlBc = Namespace.getNamespace("mmlBc", URL_mmlBc);
    //private static final Namespace mmlFcl = Namespace.getNamespace("mmlFcl",URL_mmlFcl);
    private static final Namespace mmlHi = Namespace.getNamespace("mmlHi", URL_mmlHi);
    //private static final Namespace mmlLs = Namespace.getNamespace("mmlLs", URL_mmlLs);
    //private static final Namespace mmlPc = Namespace.getNamespace("mmlPc", URL_mmlPc);
    private static final Namespace mmlRd = Namespace.getNamespace("mmlRd", URL_mmlRd);
    //private static final Namespace mmlSg = Namespace.getNamespace("mmlSg", URL_mmlSg);
    //private static final Namespace mmlSm = Namespace.getNamespace("mmlSm", URL_mmlSm);
    //private static final Namespace mmlLb = Namespace.getNamespace("mmlLb", URL_mmlLb);
    //private static final Namespace mmlRp = Namespace.getNamespace("mmlRp", URL_mmlRp);
    //private static final Namespace mmlRe = Namespace.getNamespace("mmlRe", URL_mmlRe);
    private static final Namespace mmlSc = Namespace.getNamespace("mmlSc", URL_mmlSc);
    private static final Namespace claim = Namespace.getNamespace("claim", URL_claim);
    //private static final Namespace claimA = Namespace.getNamespace("claimA", URL_claimA);

    private static final String TYPE = "type";
    private static final String TABLE_ID = "tableId";
    private static final String MML0024 = "MML0024";    // ID type (ID区分)
    private static final String MML0025 = "MML0025";    // Name representation code (表記コード，"I"= Ideographic (i.e., Kanji))
    private static final String MML0026 = "MML0026";    // Creator license code (記録者分類および医療資格コード)
    private static final String MML0027 = "MML0027";    // Facility ID type (施設ID区分)
    private static final String MML0029 = "MML0029";    // Department ID type
    private static final String MML0031 = "MML0031";    // Insurance Class (保険種別)
    private static final String CLAIM003 = "Claim003";
    private static final String CLAIM004 = "Claim004";
    private static final String CLAIM007 = "Claim007";

    private final XMLOutputter outputter;

    private static final ClaimMessageBuilder instance;

    static {
        instance = new ClaimMessageBuilder();
    }

    private ClaimMessageBuilder() {
        outputter = new XMLOutputter();
        outputter.getFormat().setEncoding("UTF-8").setExpandEmptyElements(false);
    }

    public static final ClaimMessageBuilder getInstance() {
        return instance;
    }

    // 診療行為
    public String build(ClaimHelper helper) {

        // Mml elementを作成
        Element root = new Mml(helper);

        // MmlHeaderを作成
        root.addContent(new MmlHeader(helper));

        // MmlBodyを作成
        root.addContent(new MmlBody(helper));

        // xml出力
        Document doc = new Document(root);

        return outputter.outputString(doc);
    }
    
    // 病名
    public String build(DiseaseHelper helper) {

        // Mml elementを作成
        Element root = new Mml(helper);

        // MmlHeaderを作成
        root.addContent(new MmlHeader(helper));

        // MmlBodyを作成
        root.addContent(new MmlBody(helper));

        // xml出力
        Document doc = new Document(root);

        return outputter.outputString(doc);
    }
    

    private static class Mml extends Element {

        private Mml(IMessageHelper helper) {
            super("Mml");
            setAttribute("version", "2.3");
            setAttribute("createDate", helper.getConfirmDate());
            addNamespaceDeclaration(xhtml);
            addNamespaceDeclaration(mmlCm);
            addNamespaceDeclaration(mmlNm);
            addNamespaceDeclaration(mmlFc);
            addNamespaceDeclaration(mmlDp);
            //addNamespaceDeclaration(mmlAd);
            //addNamespaceDeclaration(mmlPh);
            addNamespaceDeclaration(mmlPsi);
            addNamespaceDeclaration(mmlCi);
            //addNamespaceDeclaration(mmlPi);
            //addNamespaceDeclaration(mmlBc);
            //addNamespaceDeclaration(mmlFcl);
            addNamespaceDeclaration(mmlHi);
            //addNamespaceDeclaration(mmlLs);
            //addNamespaceDeclaration(mmlPc);
            addNamespaceDeclaration(mmlRd);
            //addNamespaceDeclaration(mmlSg);
            //addNamespaceDeclaration(mmlSm);
            //addNamespaceDeclaration(mmlLb);
            //addNamespaceDeclaration(mmlRp);
            //addNamespaceDeclaration(mmlRe);
            addNamespaceDeclaration(mmlSc);
            addNamespaceDeclaration(claim);
            //addNamespaceDeclaration(claimA);
        }
    }

    private static class MmlHeader extends Element {

        private MmlHeader(IMessageHelper helper) {
            super("MmlHeader");
            // mmlCi:CreatorInfoを追加
            addContent(new MmlCi_CreatorInfo(helper));
            // masterIdを追加
            addContent(new MasterId(helper));
            // tocを追加 省略不可
            addContent(new Toc());
        }
    }

    private static class MmlModuleItem extends Element {

        private MmlModuleItem() {
            super("MmlModuleItem");
        }
    }

    private static class MmlContent extends Element {

        private MmlContent() {
            super("content");
        }
    }

    private static class MmlBody extends Element {

        // 診療行為ClaimのMmlBodyを作成する
        private MmlBody(ClaimHelper helper) {
            super("MmlBody");

            // MmlModuleItemのuid, groupIdを決める
            String uid = helper.getDocId();
            String groupId = helper.getDocId();

            // docInfo healthInsuranceを追加
            addContent(new MmlModuleItem()
                    .addContent(new DocInfo("healthInsurance", helper, uid, groupId))
                    .addContent(new MmlContent()
                            .addContent(new MmlHi_HealthInsuranceModule(helper)))
            );

            // docInfo claimを追加
            addContent(new MmlModuleItem()
                    .addContent(new DocInfo("claim", helper, uid, groupId))
                    .addContent(new MmlContent()
                            .addContent(new Claim_ClaimModule(helper)))
            );
        }

        // 病名ClaimのMmlBodyを作成する
        private MmlBody(DiseaseHelper helper) {
            super("MmlBody");

            for (DiagnosisModuleItem item : helper.getDiagnosisModuleItems()) {

                // MmlModuleItemのuid, groupIdを決める
                String uid = helper.getGroupId();
                String groupId = item.getDocInfo().getDocId();

                // MmlModuleItemを追加。MmlModuleItemに病名は一個だけと
                addContent(new MmlModuleItem()
                        .addContent(new DocInfo("registeredDiagnosis", helper, uid, groupId))
                        .addContent(new MmlContent()
                                .addContent(new MmlRd_RegisteredDiagnosisModule(item)))
                );
            }
        }
    }

    private static class DocInfo extends Element {

        private DocInfo(String moduleType, IMessageHelper helper, String uid, String groupId) {
            super("docInfo");
            setAttribute("contentModuleType", moduleType);

            // securityLevel
            addContent(new Element("securityLevel")
                    .addContent(new Element("accessRight").setAttribute("permit", "all")
                            .addContent(new Element("facility", mmlSc)
                                    .addContent(new Element("facilityName", mmlSc)
                                            .setAttribute("facilityCode", "creator", mmlSc)
                                            .addContent("記載者施設"))
                            )
                    )
            );

            // title
            addContent(new Element("title")
                    .setAttribute("generationPurpose", "record")
                    .addContent(helper.getGenerationPurpose())
            );

            // docId
            addContent(new Element("docId")
                    .addContent(new Element("uid").addContent(uid))
                    .addContent(new Element("groupId").setAttribute("groupClass", "record").addContent(groupId))
            );

            // confirmDate
            addContent(new Element("confirmDate").addContent(helper.getConfirmDate()));

            // mmlCi:CreatorInfo
            addContent(new MmlCi_CreatorInfo(helper));

            // extRefs 省略不可
            addContent(new Element("extRefs"));
        }
    }

    private static class MmlCi_CreatorInfo extends Element {

        private MmlCi_CreatorInfo(IMessageHelper helper) {
            super("CreatorInfo", mmlCi);

            // mmlPsi:PersonalizedInfoを追加
            Element personalizedInfo = new Element("PersonalizedInfo", mmlPsi);
            addContent(personalizedInfo);

            // PersonalizedInfoにmmlCm:Idを追加
            personalizedInfo.addContent(new Element("Id", mmlCm)
                    .setAttribute(TYPE, "local").setAttribute(TABLE_ID, MML0024)
                    .addContent(helper.getCreatorId())
            );

            // PersonalizedInfoにmmlPsi:personNameを追加
            personalizedInfo.addContent(new Element("personName", mmlPsi)
                    .addContent(new Element("Name", mmlNm)
                            .setAttribute("repCode", "I", mmlNm).setAttribute(TABLE_ID, MML0025, mmlNm)
                            .addContent(new Element("fullName", mmlNm).addContent(helper.getCreatorName()))
                    )
            );

            // PersonalizedInfoにmmlFc:Facilityを追加
            personalizedInfo.addContent(new Element("Facility", mmlFc)
                    .addContent(new Element("name", mmlFc)
                            .setAttribute("repCode", "I", mmlFc).setAttribute(TABLE_ID, MML0025, mmlFc)
                            .addContent(helper.getFacilityName()))
                    .addContent(new Element("Id", mmlCm)
                            .setAttribute(TYPE, "insurance", mmlCm)
                            .setAttribute(TABLE_ID, MML0027, mmlCm)
                            .addContent(helper.getJmariCode()))
            );

            // PersonalizedInfoにmmlDp:Departmentを追加
            if (!helper.isUseDefaultDept()) {
                personalizedInfo.addContent(new Element("Department", mmlDp)
                        .addContent(new Element("name", mmlDp)
                                .setAttribute("repCode", "I", mmlDp).setAttribute(TABLE_ID, MML0025, mmlDp)
                                .addContent(helper.getCreatorDeptDesc()))
                        .addContent(new Element("Id", mmlCm)
                                .setAttribute(TYPE, "medical", mmlCm).setAttribute(TABLE_ID, MML0029, mmlCm)
                                .addContent(helper.getCreatorDept()))
                );
            }

            // mmlCi:creatorLisenseを追加
            addContent(new Element("creatorLicense", mmlCi)
                    .setAttribute(TABLE_ID, MML0026, mmlCi)
                    .addContent(helper.getCreatorLicense())
            );
        }
    }

    private static class MmlHi_HealthInsuranceModule extends Element {

        private MmlHi_HealthInsuranceModule(ClaimHelper helper) {
            super("HealthInsuranceModule", mmlHi);
            setAttribute("countryType", "JPN", mmlHi);

            PVTHealthInsuranceModel insModel = helper.getSelectedInsurance();
            if (insModel != null) {

                String insClass = insModel.getInsuranceClass();
                String insClassCode = insModel.getInsuranceClassCode();

                // mmlHi:insuranceClassを追加
                if (insClass != null && insClassCode != null) {
                    addContent(new Element("insuranceClass", mmlHi)
                            .setAttribute("ClassCode", insClassCode, mmlHi)
                            .setAttribute(TABLE_ID, MML0031, mmlHi)
                            .addContent(insClass)
                    );
                }

                // mmlHi:insuranceNumberを追加
                addContent(new Element("insuranceNumber", mmlHi).addContent(insModel.getInsuranceNumber()));

                // mmlHi:clientIdを追加
                addContent(new Element("clientId", mmlHi)
                        .addContent(new Element("group", mmlHi).addContent(insModel.getClientGroup()))
                        .addContent(new Element("number", mmlHi).addContent(insModel.getClientNumber()))
                );

                // mmlHi:familyClass, mmlHi:startDate, mmlHi.expiredDateを追加
                addContent(new Element("familyClass", mmlHi).addContent(insModel.getFamilyClass()));
                addContent(new Element("startDate", mmlHi).addContent(insModel.getStartDate()));
                addContent(new Element("expiredDate", mmlHi).addContent(insModel.getExpiredDate()));

                // mmlHi:paymentInRatio, mmlHi:paymentOutRatioを追加
                String payInRatio = insModel.getPayInRatio();
                if (payInRatio != null && !payInRatio.isEmpty()) {
                    addContent(new Element("paymentInRatio", mmlHi).addContent(payInRatio));
                }
                String payOutRatio = insModel.getPayOutRatio();
                if (payOutRatio != null && !payOutRatio.isEmpty()) {
                    addContent(new Element("paymentOutRatio", mmlHi).addContent(payOutRatio));
                }

                // 公費を追加
                PVTPublicInsuranceItemModel[] pubInsModels = insModel.getPVTPublicInsuranceItem();
                if (pubInsModels != null && pubInsModels.length > 0) {
                    Element pubInsElem = new Element("publicInsurance", mmlHi);
                    for (PVTPublicInsuranceItemModel pubIns : pubInsModels) {
                        pubInsElem.addContent(new Element("publicInsuranceItem", mmlHi)
                                .setAttribute("priority", pubIns.getPriority(), mmlHi)
                                .addContent(new Element("providerName", mmlHi).addContent(pubIns.getProviderName()))
                                .addContent(new Element("provider", mmlHi).addContent(pubIns.getProvider()))
                                .addContent(new Element("recipient", mmlHi).addContent(pubIns.getRecipient()))
                                .addContent(new Element("startDate", mmlHi).addContent(pubIns.getStartDate()))
                                .addContent(new Element("expiredDate", mmlHi).addContent(pubIns.getExpiredDate()))
                                .addContent(new Element("paymentRatio", mmlHi)
                                        .setAttribute("ratioType", pubIns.getPaymentRatioType(), mmlHi)
                                        .addContent(pubIns.getPaymentRatio()))
                        );
                    }
                    addContent(pubInsElem);
                }
            }
        }
    }

    private static class Claim_ClaimModule extends Element {

        private Claim_ClaimModule(ClaimHelper helper) {
            super("ClaimModule", claim);

            // claim:informationを追加
            Element infoElem = new Element("information", claim)
                    .setAttribute("status", "perform", claim)
                    .setAttribute("orderTime", helper.getConfirmDate(), claim)
                    .setAttribute("admitFlag", helper.getAdmitFlagStr(), claim)
                    .setAttribute("defaultTableId", "dolphin_2001_10_03", claim);
            String guid = helper.getHealthInsuranceGUID();
            if (guid != null && !guid.isEmpty()) {
                infoElem.setAttribute("insuranceUid", guid, claim);
            }
            infoElem.addContent(new Element("insuranceClass", mmlHi)
                    .setAttribute("ClassCode", helper.getHealthInsuranceClassCode(), mmlHi)
                    .setAttribute(TABLE_ID, MML0031, mmlHi)
                    .addContent(helper.getHealthInsuranceDesc())
            );
            addContent(infoElem);

            // claim:bundleを追加
            for (ClaimBundle bundle : helper.getClaimBundle()) {
                Element bundleElem = new Element("bundle", claim);
                addContent(bundleElem);
                String classCode = bundle.getClassCode();
                if (classCode != null && !classCode.isEmpty()) {
                    bundleElem.setAttribute("classCode", classCode, claim)
                            .setAttribute("classCodeId", CLAIM007)
                            .addContent(new Element("className", claim)
                                    .addContent(bundle.getClassName())
                            );
                }
                bundleElem.addContent(new Element("bundleNumber", claim).addContent(bundle.getBundleNumber()));

                for (ClaimItem item : bundle.getClaimItem()) {
                    // claim:itemを追加
                    Element itemElem = new Element("item", claim);
                    if ("0".equals(item.getClassCode())) {
                        itemElem.setAttribute("subclassCode", item.getClassCode(), claim);  // "0"?
                    }
                    itemElem.setAttribute("subclassCodeId", CLAIM003, claim)
                            .setAttribute("code", item.getCode(), claim);
                    itemElem.addContent(new Element("name", claim).addContent(item.getName()));

                    String number = item.getNumber();
                    if (number != null && !number.isEmpty()) {
                        itemElem.addContent(new Element("number", claim)
                                .setAttribute("numberCode", "10", claim)
                                .setAttribute("numberCodeId", CLAIM004, claim)
                                .addContent(item.getNumber())
                        );
                    }
                    bundleElem.addContent(itemElem);

                    // 残量廃棄の処理
                    if (item.getCanDispose()) {
                        bundleElem.addContent(new Element("item", claim)
                                .setAttribute("subclassCodeId", CLAIM003, claim)
                                .setAttribute("code", "099309901", claim)
                                .addContent(new Element("name", claim).addContent("(残量廃棄)"))
                        );
                    }
                }

                // 用法
                String admin = bundle.getAdmin();
                if (admin != null && !admin.isEmpty()) {
                    bundleElem.addContent(new Element("item", claim)
                            .setAttribute("subclassCodeId", CLAIM003, claim)
                            .setAttribute("code", bundle.getAdminCode(), claim)
                            .addContent(new Element("name", claim).addContent(admin))
                    );
                }

                // メモ
                String memo = bundle.getMemo();
                if (memo != null && !memo.isEmpty()) {
                    bundleElem.addContent(new Element("memo", claim).addContent(memo));
                }
            }
        }
    }

    private static class MmlRd_RegisteredDiagnosisModule extends Element {

        private MmlRd_RegisteredDiagnosisModule(DiagnosisModuleItem moduleItem) {
            super("RegisteredDiagnosisModule", mmlRd);

            RegisteredDiagnosisModel rd = moduleItem.getRegisteredDiagnosisModule();

            // mmlRd:diagnosisを追加
            String diagCode = rd.getDiagnosisCode();
            String diagName = rd.getDiagnosisName();
            if (diagCode != null && !diagCode.isEmpty()) {
                addContent(new Element("diagnosis", mmlRd)
                        .setAttribute("code", diagCode, mmlRd)
                        .setAttribute("system", "Diagnosis", mmlRd)
                        .addContent(diagName)
                );
            } else {
                addContent(new Element("diagnosis", mmlRd).addContent(diagName));
            }

            // mmlRd:catetoriesを追加
            String category = rd.getCategory();
            if (category != null && !category.isEmpty()) {
                addContent(new Element("categories", mmlRd)
                        .addContent(new Element("category", mmlRd)
                                .setAttribute(TABLE_ID, rd.getCategoryCodeSys(), mmlRd)
                                .addContent(category)
                        )
                );
            }

            // mmlRd:startDate, mmlRd:endDate, mmlRd:outcome,
            // mmlRd:firstEncounterDateを追加
            String startDate = rd.getStartDate();
            if (startDate != null && !startDate.isEmpty()) {
                addContent(new Element("startDate", mmlRd).addContent(startDate));
            }
            String endDate = rd.getEndDate();
            if (endDate != null && !endDate.isEmpty()) {
                addContent(new Element("endDate", mmlRd).addContent(endDate));
            }
            String outcome = rd.getOutcome();
            if (outcome != null && !outcome.isEmpty()) {
                addContent(new Element("outcome", mmlRd).addContent(outcome));
            }
            String feDate = rd.getFirstEncounterDate();
            if (feDate != null && !feDate.isEmpty()) {
                addContent(new Element("firstEncounterDate", mmlRd).addContent(feDate));
            }
        }

    }

    private static class MasterId extends Element {

        private MasterId(IMessageHelper helper) {
            super("masterId");
            addContent(new Element("Id", mmlCm)
                    .setAttribute(TYPE, "facility", mmlCm).setAttribute(TABLE_ID, MML0024, mmlCm)
                    .addContent(helper.getPatientId())
            );
        }
    }

    private static class Toc extends Element {

        private Toc() {
            super("toc");
            addContent(new TocItem(URL_mmlCm));
            addContent(new TocItem(URL_mmlNm));
            addContent(new TocItem(URL_mmlFc));
            addContent(new TocItem(URL_mmlDp));
            //addContent(new TocItem(URL_mmlAd));
            //addContent(new TocItem(URL_mmlPh));
            addContent(new TocItem(URL_mmlPsi));
            addContent(new TocItem(URL_mmlCi));
            //addContent(new TocItem(URL_mmlPi));
            //addContent(new TocItem(URL_mmlBc));
            //addContent(new TocItem(URL_mmlFcl));
            addContent(new TocItem(URL_mmlHi));
            //addContent(new TocItem(URL_mmlLs));
            //addContent(new TocItem(URL_mmlPc));
            addContent(new TocItem(URL_mmlRd));
            //addContent(new TocItem(URL_mmlSg));
            //addContent(new TocItem(URL_mmlSm));
            //addContent(new TocItem(URL_mmlLb));
            //addContent(new TocItem(URL_mmlRp));
            //addContent(new TocItem(URL_mmlRe));
            addContent(new TocItem(URL_mmlSc));
            addContent(new TocItem(URL_claim));
            //addContent(new TocItem(URL_claimA));
        }

        private class TocItem extends Element {

            private TocItem(String url) {
                super("tocItem");
                addContent(url);
            }
        }
    }

}
