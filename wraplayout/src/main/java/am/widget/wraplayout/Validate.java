package am.widget.wraplayout;

/**
 * Created by lei.jialin on 2021/4/19
 */
public class Validate {
    public static void isTrue(boolean b, String s) {
        if(!b) throw new IllegalArgumentException(s);
    }
}
