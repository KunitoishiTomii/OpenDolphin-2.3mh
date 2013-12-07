package open.dolphin.impl.pacsviewer;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * ThumbnailTableModel
 *
 * @author masuda, Masuda Naika
 */

public class ThumbnailTableModel<T>extends AbstractTableModel {
    
    private final int columnCount;
    private List<T> imageList;
    
    public ThumbnailTableModel(int columnCount) {
        this.columnCount = columnCount;
    }
    
    @Override
    public int getColumnCount() {
        return columnCount;
    }
    
    @Override
    public int getRowCount() {
        
        if (imageList == null) {
            return 0;
        }
        
        int size = imageList.size();
        int rowCount = size / columnCount;
        
        return ( (size % columnCount) != 0 ) ? rowCount + 1 : rowCount;
    }
    
    @Override
    public T getValueAt(int row, int col) {
        int index = row * columnCount + col;
        if (!isValidIndex(index)) {
            return null;
        }
        
        T entry = imageList.get(index);
        return  entry;
    }

    public void setImageList(List<T> list) {
        if (imageList != null) {
            imageList.clear();
            imageList = null;
        }
        imageList = list;
        this.fireTableDataChanged();
    }
    
    public List<T> getImageList() {
        return imageList;
    }

    public void addImage(T entry){
        if (imageList == null){
            imageList = new ArrayList<>();
        }
        imageList.add(entry);
        this.fireTableDataChanged();
    }
    
    private boolean isValidIndex(int index) {
        return !(imageList == null || index < 0 || index >= imageList.size());
    }
    
    public void clear() {
        if (imageList != null) {
            imageList.clear();
            this.fireTableDataChanged();
        }
    }
}
