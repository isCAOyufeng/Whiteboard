package LocalWhiteboard;

import java.awt.*;
import java.io.Serializable;
import java.util.List;

public class DrawCommand implements Serializable {
    public enum Type {
        LINE, RECTANGLE, OVAL, TRIANGLE, FREEDRAW, TEXT, RUBBER;
    }

    private final String username;
    private final Type type;
    private final Point startPoint;
    private final Point endPoint;
    private List<Point> path;
    private final Color color;
    private String text;
    private final int rubberSize;

    public DrawCommand(String username, Type type, Point start, Point end, Color color, String text, int rubberSize) {
        this.username = username;
        this.type = type;
        this.startPoint = start;
        this.endPoint = end;
        this.color = color;
        this.text = text;
        this.rubberSize = rubberSize;
    }

    public DrawCommand(String username, Type type, List<Point> path, Color color, int rubberSize) {
        this.username = username;
        this.type = type;
        this.path = path;
        this.color = color;
        this.rubberSize = rubberSize;
        this.startPoint = path.get(0);
        this.endPoint = path.get(path.size() - 1);
    }

    public String getUsername() { return username; }
    public Type getType() { return type; }
    public Point getStartPoint() { return startPoint; }
    public Point getEndPoint() { return endPoint; }
    public List<Point> getPath() {return path;}
    public Color getColor() { return color; }
    public String getText() { return text; }
    public int getRubberSize() { return rubberSize; }
}
