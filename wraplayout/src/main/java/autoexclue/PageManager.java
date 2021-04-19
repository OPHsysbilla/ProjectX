package autoexclue;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;


public class PageManager {
     AutoExcludeLayout layout;
     List<CementItem<?>> data;
     List<Segment> segments;
     SparseArray<Integer> findPageMap;
     Segment curSegment;
     AutoExcludeLayout.Adapter adapter = new AutoExcludeLayout.Adapter();
     int pageCount = 0;

    public void setAdapter(AutoExcludeLayout.Adapter adapter) {
        this.adapter = adapter;
    }

    private void setData(List<CementItem<?>> data) {
        this.data = data;
    }


    private void switchToItem(CementItem<?> item) {
        int find = data.indexOf(item);
        if (find == -1) return;
        if (find >= 0 && find < data.size()) {
            switchToPage(find);
        }
    }

    private void switchToPage(int dataIndex) {
        int page = findPageByDataIndex(dataIndex);
        curSegment = segments.get(page);
        adapter.clearData();
        List<CementItem<?>> items = new ArrayList<>();
        for (int i = curSegment.start; i < curSegment.end; i++) {
            items.add(data.get(i));
        }
        adapter.addDataList(items);
    }

    private int findPageByDataIndex(int dataIndex) {
        int page = 0;
        Segment cur = segments.get(page);
        while (page < segments.size() && cur != null && cur.end < dataIndex) {
            page++;
            cur = segments.get(page);
        }
        return page;
    }

}
