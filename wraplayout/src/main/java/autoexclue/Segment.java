package autoexclue;

import java.util.ArrayList;

public class Segment {
    int layoutRows;
    ArrayList<Integer> childMaxWidth;
    int measureRows;
    boolean haveMeasured;

    Segment(int start,
            int end,
            int size) {
        this.start = start;
        this.end = end;
        this.size = size;
    }

    int start;
    int end;
    int size;
    int height;
    int width;

    public void reset() {
        start = 0;
        end = 0;
        size = 0;
        height = 0;
        width = 0;
    }
}