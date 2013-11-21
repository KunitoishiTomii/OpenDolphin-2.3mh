package open.dolphin.infomodel;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * InfoModel
 *
 * @author Minagawa,Kazushi
 */
public class ModelUtils implements IInfoModel {
    
    //public static final Date AD1800 = new Date(-5362016400000L);
    
    public static String trimTime(String mmlDate) {
        
        if (mmlDate != null) {
            int index = mmlDate.indexOf('T');
            if (index > -1) {
                return mmlDate.substring(0, index);
            } else {
                return mmlDate;
            }
        }
        return null;
    }
    
    public static String trimDate(String mmlDate) {
        
        if (mmlDate != null) {
            int index = mmlDate.indexOf('T');
            if (index > -1) {
                // THH:mm:ss -> HH:mm
                return mmlDate.substring(index + 1, index + 6);
            } else {
                return mmlDate;
            }
        }
        return null;
    }
    
    public static String getAgeBirthday(String mmlBirthday) {

        String age = getAge(mmlBirthday);

        if (age != null) {

            StringBuilder sb = new StringBuilder();
            sb.append(age);
            sb.append(" ");
            sb.append(AGE);
            sb.append(" (");
            sb.append(mmlBirthday);
            sb.append(")");
            return sb.toString();
        }
        return null;
    }
    
    public static String getAge(String mmlBirthday) {
        
        try {
            GregorianCalendar gc1 = getCalendar(mmlBirthday);
            GregorianCalendar gc2 = new GregorianCalendar(); // Today
            int years = 0;
            int month = 0;
            
            gc1.clear(Calendar.MILLISECOND);
            gc1.clear(Calendar.SECOND);
            gc1.clear(Calendar.MINUTE);
            gc1.clear(Calendar.HOUR_OF_DAY);
            
            gc2.clear(Calendar.MILLISECOND);
            gc2.clear(Calendar.SECOND);
            gc2.clear(Calendar.MINUTE);
            gc2.clear(Calendar.HOUR_OF_DAY);

            while (gc1.before(gc2)) {
                gc1.add(Calendar.YEAR, 1);
                years++;
            }

            gc1.add(Calendar.YEAR, -1);
            years--;

            while (gc1.before(gc2)) {
                gc1.add(Calendar.MONTH, 1);
                month++;
            }
            month--;
            
            StringBuilder buf = new StringBuilder();
            buf.append(years);

            if (month != 0) {
                buf.append(".");
                buf.append(month);
            }
            
            return buf.toString();
            
        } catch (Exception e) {
            return null;
        }
    }

    public static int[] getAgeSpec(String mmlBirthday) {

        try {
            GregorianCalendar gc1 = getCalendar(mmlBirthday);
            GregorianCalendar gc2 = new GregorianCalendar(); // Today
            int years = 0;
            int month = 0;

            gc1.clear(Calendar.MILLISECOND);
            gc1.clear(Calendar.SECOND);
            gc1.clear(Calendar.MINUTE);
            gc1.clear(Calendar.HOUR_OF_DAY);

            gc2.clear(Calendar.MILLISECOND);
            gc2.clear(Calendar.SECOND);
            gc2.clear(Calendar.MINUTE);
            gc2.clear(Calendar.HOUR_OF_DAY);

            while (gc1.before(gc2)) {
                gc1.add(Calendar.YEAR, 1);
                years++;
            }

            gc1.add(Calendar.YEAR, -1);
            years--;

            while (gc1.before(gc2)) {
                gc1.add(Calendar.MONTH, 1);
                month++;
            }
            month--;

            return new int[]{years, month};

        } catch (Exception e) {
            e.printStackTrace(System.err);
            return new int[]{-1, -1};
        }
    }
    
    public static GregorianCalendar getCalendar(String mmlDate) {
        
        try {
            // Trim time if contains
            mmlDate = trimTime(mmlDate);
            String[] cmp = mmlDate.split("-");
            String yearSt = cmp[0];
            String monthSt = cmp[1];
            if (monthSt.startsWith("0")) {
                monthSt = monthSt.substring(1);
            }
            String daySt = cmp[2];
            if (daySt.startsWith("0")) {
                daySt = daySt.substring(1);
            }
            int year = Integer.parseInt(yearSt);
            int month = Integer.parseInt(monthSt);
            month--;
            int day = Integer.parseInt(daySt);
            
            return new GregorianCalendar(year, month, day);
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    
    public static Date getDateAsObject(String mmlDate) {
        if (mmlDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_WITHOUT_TIME);
                return sdf.parse(mmlDate);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }
    
    public static Date getDateTimeAsObject(String mmlDate) {
        if (mmlDate != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
                return sdf.parse(mmlDate);
                
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }
    
    public static String getDateAsString(Date date) {
        if (date != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_WITHOUT_TIME);
                return sdf.format(date);
                
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }
    
    public static String getDateTimeAsString(Date date) {
        if (date != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
                return sdf.format(date);
                
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }
    
    public static String getDateAsFormatString(Date date, String format) {
        if (date != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.format(date);
                
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }
    
    public static String getGenderDesc(String gender) {
        
        if (gender != null) {
            String test = gender.toLowerCase();
            if (test.equals(MALE)) {
                return MALE_DISP;
            } else if (test.equals(FEMALE)) {
                return FEMALE_DISP;
            } else {
                return UNKNOWN;
            }
        }
        return UNKNOWN;
    }

    public static String getGenderMFDesc(String gender) {

        if (gender != null) {
            String test = gender.toLowerCase();
            if (test.startsWith("m") || test.startsWith("男") ) {
                return "M";
            } else if (test.startsWith("f") || test.startsWith("女") ) {
                return "F";
            } else {
                return "U";
            }
        }
        return "U";
    }
    
    public boolean isValidModel() {
        return true;
    }
    
    public static String[] splitDiagnosis(String diagnosis) {
        if (diagnosis == null) {
            return null;
        }
        String[] ret = null;
        try {
            ret = diagnosis.split("\\s*,\\s*");
        } catch (Exception e) {
        }
        return ret;
    }
    
    public static String getDiagnosisName(String hasAlias) {
        String[] splits = splitDiagnosis(hasAlias);
        return (splits != null && splits.length == 2 && splits[0] != null) ? splits[0] : hasAlias;
    }
    
    public static String getDiagnosisAlias(String hasAlias) {
        String[] splits = splitDiagnosis(hasAlias);
        return (splits != null && splits.length == 2 && splits[1] != null) ? splits[1] : null;
    }

    public static ModuleModel cloneModule(ModuleModel module) {
        try {
            return (ModuleModel)module.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }

    public static SchemaModel cloneSchema(SchemaModel model) {
        try {
            return (SchemaModel)model.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }

    public static BundleDolphin cloneBundleDolphin(BundleDolphin model) {
        try {
            return (BundleDolphin)model.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }

    public static BundleMed cloneBundleMed(BundleMed model) {
        try {
            return (BundleMed)model.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }
    
//masuda^
    // test(mmlDate形式)当時の年齢を計算する
    public static String getAge2(String mmlBirthday, String test) {

        try {
            GregorianCalendar gc1 = getCalendar(mmlBirthday);
            GregorianCalendar gc2 = getCalendar(test);
            int years = 0;

            gc1.clear(GregorianCalendar.MILLISECOND);
            gc1.clear(GregorianCalendar.SECOND);
            gc1.clear(GregorianCalendar.MINUTE);
            gc1.clear(GregorianCalendar.HOUR_OF_DAY);

            gc2.clear(GregorianCalendar.MILLISECOND);
            gc2.clear(GregorianCalendar.SECOND);
            gc2.clear(GregorianCalendar.MINUTE);
            gc2.clear(GregorianCalendar.HOUR_OF_DAY);

            while (gc1.before(gc2)) {
                gc1.add(GregorianCalendar.YEAR, 1);
                years++;
            }
            years--;

            int month = 12;

            while (gc1.after(gc2)) {
                gc1.add(GregorianCalendar.MONTH, -1);
                month--;
            }

            StringBuilder buf = new StringBuilder();
            buf.append(years);
            if (month != 0) {
                buf.append(".");
                buf.append(month);
            }
            return buf.toString();

        } catch (Exception e) {
            return null;
        }
    }
    // 日付の比較のためにNullなら大昔にする
    public static GregorianCalendar getStartDate(Date start) {
        GregorianCalendar ret;
        if (start != null) {
            ret = getMidnightGc(start);
        } else {
            ret = new GregorianCalendar();
            ret.setTime(AD1800);
        }
        return ret;
    }
    // 日付の比較のためにNullなら遠い未来にする
    public static GregorianCalendar getEndedDate(Date ended) {
        GregorianCalendar ret;
        if (ended != null) {
            ret = getMidnightGc(ended);
        } else {
            ret = new GregorianCalendar();
            ret.setTime(new Date(Long.MAX_VALUE));
        }
        return ret;
    }
    // 指定日の０時０分０秒のGregorianCalendarを取得する
    public static GregorianCalendar getMidnightGc(Date d) {
        GregorianCalendar ret = new GregorianCalendar();
        ret.setTime(d);
        int year = ret.get(GregorianCalendar.YEAR);
        int month = ret.get(GregorianCalendar.MONTH);
        int date = ret.get(GregorianCalendar.DAY_OF_MONTH);
        ret.clear();
        ret.set(year, month, date);
        return ret;
    }
    //
    public static boolean isDateBetween(Date start, Date end, Date test) {
        //boolean ret = (test.after(start) || test.getTime() == start.getTime())
        //           && (test.before(end) || test.getTime() == end.getTime());
        boolean ret = !test.before(start) && !test.after(end);
        return ret;
    }
    
    public static String getAgeBirthday2(String mmlBirthday){

        String age = getAge(mmlBirthday);
        if (age != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(age);
            sb.append(" ");
            sb.append(AGE);
            sb.append(" ([");
            sb.append(toNengo(mmlBirthday).substring(0, 3));
            sb.append("]");
            sb.append(mmlBirthday);
            sb.append(")");
            return sb.toString();
        }
        return null;
    }
    
    //// 西暦=>年号変換
    public static String toNengo(String mmlBirthday) {
        GregorianCalendar gc = getCalendar(mmlBirthday);
        Locale locale = new Locale("ja","JP","JP"); 
        SimpleDateFormat frmt = new SimpleDateFormat("Gyy-MM-dd", locale);
        return frmt.format(gc.getTime());
    }
    
    public static String getByoKanrenKbnStr(int byoKanrenKbn) {
        String ret = "";
        switch (byoKanrenKbn) {
            case 3:
                ret = "皮特疾Ⅰ";
                break;
            case 4:
                ret = "皮特疾Ⅱ";
                break;
            case 5:
                ret = "特定疾患";
                break;
        }
        return ret;
    }
    
    public static String extractText(String xml) {
        StringBuilder sb = new StringBuilder();
        String head[] = xml.split("<text>");
        for (String str : head) {
            String tail[] = str.split("</text>");
            if (tail.length == 2) {
                sb.append(tail[0].trim());
            }
        }
        return sb.toString();
    }
    
    public static String convertListLongToStr(List<Long> list){
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Long value : list) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(String.valueOf(value));
        }
        return sb.toString();
    }
    
    public static List<Long> convertStrToListLong(String str) {
        String[] values = str.split(",");
        List<Long> list = new ArrayList<Long>();
        for (String value : values) {
            list.add(Long.valueOf(value));
        }
        return list;
    }
//masuda$
}
