import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.SocketException;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

public class ClientUI {
    private  JFrame window;
    private  MenuPanel menu;
    private  EntryPanel loginScreen;
    private  SocialPanel socialPanel;
    private  PlayPanel playPanel;
    private  GamePanel gamePanel;
    private  ResultPanel resultPanel;

    public ClientUI()throws SocketException {
        Client testClient = new Client(8080, 6666,this);
        startUI();
        loginScreen = new EntryPanel(testClient, this);
        menu = new MenuPanel(testClient, this);
        socialPanel = menu.getSocialPanel();
        playPanel = menu.getPlayPanel();
        gamePanel = new GamePanel(testClient,this);
        testClient.setPlayPanel(playPanel);
        testClient.setGamePanel(gamePanel);

        resultPanel = new ResultPanel(testClient, this);
        testClient.setResultPanel(resultPanel);
        //window.setContentPane(gamePanel.getPanel());
        window.setContentPane(loginScreen.getEntryPanel());
        window.setVisible(true);
    }


    private void startUI(){
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
    public void switchToMenu(){
        menu.refreshPage();
        window.setContentPane(menu.getMenuPanel());
        window.revalidate();
        window.repaint();
    }

    public void switchToEntryPage(){
        loginScreen.clearErrors();
        window.setContentPane(loginScreen.getEntryPanel());
        window.revalidate();
        window.repaint();
    }

    public void switchToSocialPage(){
        socialPanel.refreshPage();
        window.setContentPane(socialPanel.getPanel());
        window.revalidate();
        window.repaint();
    }

    public void switchToPlayPage(){
        playPanel.refreshPage();
        window.setContentPane(playPanel.getPanel());
        window.revalidate();
        window.repaint();
    }

    public void switchToGamePage(){
        gamePanel.refreshPage();
        window.setContentPane(gamePanel.getPanel());
        window.revalidate();
        window.repaint();
    }

    public void switchToResult(){
        resultPanel.refreshPage();
        window.setContentPane(resultPanel.getPanel());
        window.revalidate();
        window.repaint();
    }

    public GamePanel getGamePanel(){
        return this.gamePanel;
    }

    public ResultPanel getResultPanel(){
        return this.resultPanel;
    }

}
