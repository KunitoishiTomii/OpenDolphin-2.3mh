package open.dolphin.table;

import java.util.List;

/**
 * ListTableSorter
 *
 * @author masuda, Masuda Naika
 */
public class ListTableSorter<T> extends TableSorter {
    
    public ListTableSorter(ListTableModel<T> tableModel) {
        super(tableModel);
    }

    // 対応するObjectを返す
    public T getObject(int row) {

        if (row >= 0 && row < getTableModel().getRowCount()) {
            return getListTableModel().getObject(modelIndex(row));
        }
        return null;
    }
    
    public ListTableModel<T> getListTableModel() {
        return (ListTableModel<T>) getTableModel();
    }
    
    // ListTableModelにデータを設定する
    public void setDataProvider(List<T> dataProvider) {
        getListTableModel().setDataProvider(dataProvider);
    }
}
