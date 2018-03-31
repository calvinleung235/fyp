package comk.example.kcleung235.fyp;

import org.opencv.core.Point;
import org.opencv.core.Rect;

/**
 * Created by calvinleung on 30/3/2018.
 */

public class PointWithRect {

    private Point point;
    private org.opencv.core.Rect rect;

    public PointWithRect( Point p, org.opencv.core.Rect r ){
        point = p;
        rect = r;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }
}
