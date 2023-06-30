package bid.yuanlu.ifr_controller;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MapClicker {
    public final double x1, x2, y1, y2;
    private final Point[] points;

    private MapClicker(double x1, double y1, double x2, double y2, @NonNull Collection<Point> points) {
        this.points = points.toArray(new Point[0]);
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
    }

    /**
     * 生成实例
     *
     * @param x1  地图左上角
     * @param y1  地图左上角
     * @param x2  地图右下角
     * @param y2  地图右下角
     * @param pts 所有点(x,y)
     * @return 实例
     */
    @NonNull
    @Contract(value = "_, _, _, _, _ -> new", pure = true)
    public static MapClicker getInstance(double x1, double y1, double x2, double y2, @NonNull double... pts) {
        double w = x2 - x1, h = y2 - y1;
        if (pts.length % 2 != 0) throw new IllegalArgumentException("Unpaired pt (" + pts.length + "): " + Arrays.toString(pts));
        ArrayList<Point> points = new ArrayList<>(pts.length / 2);
        for (int i = 0; i < pts.length; i += 2) {
            points.add(new Point((pts[i] - x1) / w, (pts[i + 1] - y1) / h));
        }
        return new MapClicker(x1, y1, x2, y2, points);
    }

    /**
     * 获取Robocon2023竞赛地图点位
     */
    @NonNull
    @Contract(value = " -> new", pure = true)
    public static MapClicker getRobocon2023() {
        return getInstance(2000, 4500, 10000, 10000,//
                2800, 9200,//己方左下角
                6000, 9200,//己方下方
                9200, 9200,//己方右下角
                4700, 7300,//吴哥区左下角
                7300, 7300,//吴哥区右下角
                6000, 6000,//吴哥区中心
                4700, 4700,//吴哥区左上角
                7300, 4700//吴哥区右上角
        );
    }

    /**
     * 获取最接近的点序号
     *
     * @param x 坐标[0,1]
     * @param y 坐标[0,1]
     * @return 点序号
     */
    public int getClosest(double x, double y) {
        int min = 0;
        double dis = points[min].distanceSquare(x, y);
        for (int i = 1; i < points.length; i++) {
            double d = points[i].distanceSquare(x, y);
            if (d < dis) {
                dis = d;
                min = i;
            }
        }
        return min;
    }

    public static final class Point {
        public final double x, y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Contract(pure = true)
        private double distanceSquare(double x, double y) {
            x -= this.x;
            y -= this.y;
            return x * x + y * y;
        }
    }
}
