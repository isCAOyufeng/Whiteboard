import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LoginDialog extends JDialog{
    private JFrame frame;
    private boolean isAdmin;
    private String username;
    private String ip;
    private int port;
    private boolean isSucceed = false;
    private WhiteboardClientStub clientStub;
    private WhiteboardServerStub serverStub;

    public LoginDialog() {
        frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 250);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 2));
        JTextField usernameField = new JTextField();
        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("1099");
        JRadioButton createBtn = new JRadioButton("Create Server");
        JRadioButton joinBtn = new JRadioButton("Join Server");
        ButtonGroup group = new ButtonGroup();
        group.add(createBtn);
        group.add(joinBtn);

        JButton submit = new JButton("Submit");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("IP Address:"));
        panel.add(ipField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(createBtn);
        panel.add(joinBtn);
        panel.add(new JLabel());
        panel.add(submit);

        frame.add(panel);
        frame.setVisible(true);

        submit.addActionListener(e -> {
            username = usernameField.getText().trim();
            ip = ipField.getText().trim();
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid port number.");
                return;
            }

            if (username.isEmpty() || (!createBtn.isSelected() && !joinBtn.isSelected())) {
                JOptionPane.showMessageDialog(frame, "Please fill all fields and choose an option.");
                return;
            }

            isAdmin = createBtn.isSelected();

            if (isAdmin) {
                try {
                    WhiteboardServer.main(new String[]{ip, ((Integer) port).toString()});
//                    WhiteboardServer.main(new String[]{});
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to start server on this IP/Port. It may already be in use.", "Server Error", JOptionPane.ERROR_MESSAGE);
                }
//                this.isSucceed = true;
            } else {

//                this.isSucceed = true;
            }

            try {
                this.clientStub = new ClientServant(this.username);
            } catch (RemoteException re) {
                System.out.println("client stub creation failed.");
            }

            try {
                // hard code for now
                Registry registry = LocateRegistry.getRegistry(ip);
                serverStub = (WhiteboardServerStub) registry.lookup("whiteboard");
            } catch (Exception ex) {
                System.out.println("registry or remote stub not found.");
                return;
            }

            try {
                serverStub.registerClient(clientStub);
            } catch (RemoteException ex) {
                System.out.println("client register failed.");
                JOptionPane.showMessageDialog(this, "client register failed.");
            } catch (DuplicateUsernameException ex) {
                System.out.println(ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage());
                usernameField.setText("");
                usernameField.requestFocus();
                this.username = null;
            }

            if (this.username != null) {
                this.isSucceed = true;
                frame.setVisible(false); // close login
            }
        });
    }

    public boolean isSucceed() {
        return isSucceed;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public WhiteboardServerStub getServerStub() {
        return this.serverStub;
    }

    public WhiteboardClientStub getClientStub() {
        return this.clientStub;
    }
}