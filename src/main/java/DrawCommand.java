
import java.awt.*;
import java.io.Serializable;
import java.util.List;

public class DrawCommand implements Serializable {
    public enum Type {
        LINE, RECTANGLE, OVAL, TRIANGLE, FREEDRAW, TEXT, RUBBER;
    }

    private String username;
    private Type type;
    private Point startPoint;
    private Point endPoint;
    private List<Point> path;
    private Color color;
    private String text;
    private int eraserSize;
    private Stroke stroke;

    public DrawCommand(String username, Type type, Point start, Point end, Color color, String text, int eraserSize) {
        this.username = username;
        this.type = type;
        this.startPoint = start;
        this.endPoint = end;
        this.color = color;
        this.text = text;
        this.eraserSize = eraserSize;
    }

    public DrawCommand(String username, Type type, List<Point> path, Color color, int eraserSize) {
        this.username = username;
        this.type = type;
        this.path = path;
        this.color = color;
        this.eraserSize = eraserSize;
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
    public int getEraserSize() { return eraserSize; }
}
