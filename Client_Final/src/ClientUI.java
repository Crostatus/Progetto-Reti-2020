import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.SocketException;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

/*
OVERVIEW: Un oggetto ClientUI implementa l'inizializzazione e gestione delle varie schermate presenti nell'
          interfaccia grafica, con metodi per interagire responsivamente agli eventi.
 */

public class ClientUI {
    private  JFrame window;
    private  MenuPanel menu;
    private  EntryPanel loginScreen;
    private  SocialPanel socialPanel;
    private ChallengePanel challengePanel;
    private  GamePanel gamePanel;
    private  ResultPanel resultPanel;

    public ClientUI()throws SocketException {
        Client testClient = new Client(8080, 6666,this);
        startUI();
        loginScreen = new EntryPanel(testClient, this);
        menu = new MenuPanel(testClient, this);
        socialPanel = menu.getSocialPanel();
        challengePanel = menu.getChallengePanel();
        gamePanel = new GamePanel(testClient,this);
        testClient.setChallengePanel(challengePanel);
        testClient.setGamePanel(gamePanel);
        resultPanel = new ResultPanel(testClient, this);
        testClient.setResultPanel(resultPanel);

        window.setContentPane(loginScreen.getEntryPanel());
        window.setVisible(true);
    }
    //Avvio interfaccia con schermata iniziale di login/registrazione.
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
    //Metodo per mostrare la schermata di menu.
    public void switchToMenu(){
        menu.refreshPage();
        window.setContentPane(menu.getMenuPanel());
        window.revalidate();
        window.repaint();
    }
    //Metodo per mostrare la schermata iniziale.
    public void switchToEntryPage(){
        loginScreen.clearErrors();
        window.setContentPane(loginScreen.getEntryPanel());
        window.revalidate();
        window.repaint();
    }
    //Metodo per mostrare la schermata per l' aggiunta di nuove amicizie.
    public void switchToSocialPage(){
        socialPanel.refreshPage();
        window.setContentPane(socialPanel.getPanel());
        window.revalidate();
        window.repaint();
    }
    //Metodo per mostrare la schermata per mandare richieste di sfida.
    public void switchToChallengePage(){
        challengePanel.refreshPage();
        window.setContentPane(challengePanel.getPanel());
        window.revalidate();
        window.repaint();
    }
    //Metodo per mostrare la schermata di gioco.
    public void switchToGamePage(){
        gamePanel.refreshPage();
        window.setContentPane(gamePanel.getPanel());
        window.revalidate();
        window.repaint();
    }
    //Metodo per mostrare la schermata
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
