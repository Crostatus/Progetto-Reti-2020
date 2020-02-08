import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

public class ClientUI {
    private static JFrame window;

    public static void main(String[] args) throws Exception{
        Client testClient = new Client(8080, 6666);
        startUI();
        EntryPanel loginScreen = new EntryPanel(testClient, window);


        window.setContentPane(loginScreen.getEntryPanel());
        window.setVisible(true);

    }

    private static void startUI(){
        window = new JFrame("Word Quizzle");
        window.setSize(750,550);
        window.setLocation(300,100);
        window.setResizable(false);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }



}
