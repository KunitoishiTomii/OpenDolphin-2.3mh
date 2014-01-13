package open.dolphin.impl.rezept.model;

/**
 * 算定日と回数のモデル
 * 
 * @author masuda, Masuda Naika
 */
public class DayNumberPair {
    
    private int day;
    private int number;
    
    public DayNumberPair(int day, int number) {
        this.day = day;
        this.number = number;
    }
    
    public void setDay(int day) {
        this.day = day;
    }
    public int getDay() {
        return day;
    }
    
    public void setNumber(int number) {
        this.number = number;
    }
    public int getNumber() {
        return number;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(day).append('(').append(number).append(')');
        return sb.toString();
    }
}
