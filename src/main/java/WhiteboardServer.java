import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class WhiteboardServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            WhiteboardServerStub serverStub = new ServerServant();
            Naming.rebind("rmi://localhost:1099/whiteboard", serverStub);

            System.out.println("Whiteboard server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
