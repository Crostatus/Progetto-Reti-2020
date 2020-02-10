import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

public class ClientUI {
    private static JFrame window;
    private static MenuPanel menu;
    private static EntryPanel loginScreen;
    private static SocialPanel socialPanel;
    private static PlayPanel playPanel;
    private static GamePanel gamePanel;
    private static ResultPanel resultPanel;

    public static void main(String[] args) throws Exception{
        Client testClient = new Client(8080, 6666);
        startUI();
        loginScreen = new EntryPanel(testClient, window);
        menu = new MenuPanel(testClient, window);
        socialPanel = menu.getSocialPanel();
        playPanel = menu.getPlayPanel();
        gamePanel = new GamePanel(testClient, window);
        testClient.setPlayPanel(playPanel);
        testClient.setGamePanel(gamePanel);

        resultPanel = new ResultPanel(testClient, window);
        testClient.setResultPanel(resultPanel);
        //window.setContentPane(resultPanel.getPanel());
        window.setContentPane(loginScreen.getEntryPanel());
        window.setVisible(true);

    }

    private static void startUI(){
        window = new JFrame("Word Quizzle");
        window.setSize(750,550);
        window.setLocation(1100,210);
        window.setResizable(false);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    //da fare i refresh!
    public static void switchToMenu(){
        menu.refreshPage();
        window.setContentPane(menu.getMenuPanel());
        window.revalidate();
        window.repaint();
    }

    public static void switchToEntryPage(){
        loginScreen.clearErrors();
        window.setContentPane(loginScreen.getEntryPanel());
        window.revalidate();
        window.repaint();
    }

    public static void switchToSocialPage(){
        socialPanel.refreshPage();
        window.setContentPane(socialPanel.getPanel());
        window.revalidate();
        window.repaint();
    }

    public static void switchToPlayPage(){
        playPanel.refreshPage();
        window.setContentPane(playPanel.getPanel());
        window.revalidate();
        window.repaint();
    }

    public static void switchToGamePage(){
        gamePanel.refreshPage();
        window.setContentPane(gamePanel.getPanel());
        window.revalidate();
        window.repaint();
    }

    public static void switchToResult(){
        resultPanel.refreshPage();
        window.setContentPane(resultPanel.getPanel());
        window.revalidate();
        window.repaint();
    }


}
