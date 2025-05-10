public enum ToolType {
    LINE,
    TRIANGLE,
    RECTANGLE,
    OVAL,
    FREEDRAW,
    TEXT,
    RUBBER;

    public static ToolType fromDrawCommandType(DrawCommand.Type type) {
        return switch (type) {
            case LINE -> LINE;
            case RECTANGLE -> RECTANGLE;
            case OVAL -> OVAL;
            case TEXT -> TEXT;
            case TRIANGLE -> TRIANGLE;
            case FREEDRAW -> FREEDRAW;
            case RUBBER -> RUBBER;
        };
    }

    public static DrawCommand.Type fromToolType(ToolType type) {
        return switch (type) {
            case LINE -> DrawCommand.Type.LINE;
            case RECTANGLE -> DrawCommand.Type.RECTANGLE;
            case OVAL -> DrawCommand.Type.OVAL;
            case TEXT -> DrawCommand.Type.TEXT;
            case TRIANGLE -> DrawCommand.Type.TRIANGLE;
            case FREEDRAW -> DrawCommand.Type.FREEDRAW;
            case RUBBER -> DrawCommand.Type.RUBBER;
        };
    }
}