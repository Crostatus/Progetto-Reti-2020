import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;

/*
OVERVIEW: Un oggetto di tipo MenuPanel rappresenta la schermata interattiva che viene mostrata sull' interfaccia
          dopo aver effettuato un login o finito una sfida. In questa schermata ci sono pulsanti per
          essere diretti in tutte le altre schermate.
 */

public class MenuPanel {
    private JPanel menuPanel;
    private Client user;
    private ClientUI clientUI;
    private SocialPanel socialPanel;
    private ChallengePanel challengePanel;

    private JTextField username;
    private JTextField score;
    private JButton playButton;
    private JButton socialButton;
    private JButton logoutButton;
    private JButton refreshButton;
    private JLabel  screen;
    private JTextField best;
    private JTextArea scoreList;
    private JScrollPane scrollPane;


    private JTextField errorText;

    public MenuPanel(Client user, ClientUI clientUI){
        this.user = user;
        this.clientUI = clientUI;
        menuPanel = new JPanel();
        menuPanel.setLayout(null);

        logoutButton = setupButton(713,5, 32,30);
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    user.logout();
                }
                catch (IOException z){
                    user.forceLogout();
                    z.printStackTrace();
                }
                clientUI.switchToEntryPage();
            }
        });
        menuPanel.add(logoutButton);

        refreshButton = setupButton(680, 120, 32, 30);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                errorText.setText("");
                try {
                    JSONArray leaderboard = user.mostra_classifica(user.getNickname());
                    updateUserScores(leaderboard);
                }
                catch (IOException z){
                    errorText.setText("Ops, qualcosa è andato storto :(");
                    user.forceLogout();
                    z.printStackTrace();
                }
                catch (ParseException x){
                    errorText.setText("Ops, qualcosa è andato storto :(");
                    user.forceLogout();
                    x.printStackTrace();
                }
                menuPanel.repaint();
            }
        });
        menuPanel.add(refreshButton);

        playButton = setupButton(20, 250, 316, 75);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientUI.switchToChallengePage();
            }
        });
        menuPanel.add(playButton);

        socialButton = setupButton(20, 365, 316, 75);
        socialButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientUI.switchToSocialPage();
            }
        });
        menuPanel.add(socialButton);

        username = setupTextField(80, 50, 180, 47, "", 25);
        menuPanel.add(username);
        score = setupTextField(55, 102, 80, 47, "", 23);
        menuPanel.add(score);
        best = setupTextField(510, 163, 180, 47, "", 23);
        menuPanel.add(best);

        errorText = setupTextField(210, 70, 400, 47, "", 26);
        menuPanel.add(errorText);

        setupLeaderBoard(490, 215, 200, 263);
        menuPanel.add(scrollPane);

        getWallpaper();
        menuPanel.add(screen);

        socialPanel = new SocialPanel(user, clientUI);
        socialPanel.setButtons();
        socialPanel.setScreen();

        challengePanel = new ChallengePanel(user, clientUI);
        challengePanel.setButtons();
        challengePanel.setScreen();

    }

    //Ordinamento e visualizzazione della classifica punti "leaderboard" negli appositi componenti.
    private void updateUserScores(JSONArray leaderboard){
        if(leaderboard == null){
            //mostra testo errore
            user.forceLogout();
            errorText.setText("Leaderboard a null");
            return;
        }
        Iterator<JSONObject> leaderBoardIterator = leaderboard.iterator();
        String myScore = "";
        if(leaderBoardIterator.hasNext())
            myScore = String.valueOf(leaderBoardIterator.next().get("punteggio"));

        score.setText(myScore);
        username.setText(user.getNickname());

        leaderboard.sort(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                JSONObject o11 = (JSONObject) o1;
                JSONObject o22 = (JSONObject) o2;
                int punteggio1 = Integer.parseInt(o11.get("punteggio").toString());
                int punteggio2 = Integer.parseInt(o22.get("punteggio").toString());

                return punteggio2 - punteggio1;
            }
        });
        leaderBoardIterator = leaderboard.iterator();
        String bestNickname = "";
        JSONObject bestPlayer = null;
        if(leaderBoardIterator.hasNext())
            bestPlayer = leaderBoardIterator.next();

        leaderBoardIterator.remove();
        bestNickname = String.valueOf(bestPlayer.get("nickname"));
        String bestScore = String.valueOf(bestPlayer.get("punteggio"));

        best.setText(bestNickname + "   " + bestScore);
    }

    //Inizializzazione componenti per la visualizzazione della classifica.
    private void setupLeaderBoard(int x, int y, int width, int height){
        scoreList = new JTextArea();
        scoreList.setEditable(false);
        String fontName = String.valueOf(scoreList.getFont());
        scoreList.setFont(new Font(fontName,Font.PLAIN,23));
        scoreList.setColumns(1);
        scoreList.setOpaque(false);
        scrollPane = new JScrollPane(scoreList);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(x,y,width, height);
    }

    //Aggiornamento di questa schermata
    public void refreshPage(){
        errorText.setText("");
        try {
            JSONArray leaderboard = user.mostra_classifica(user.getNickname());
            updateUserScores(leaderboard);
            scoreList.setText("");

            int areaSize = leaderboard.size();
            scoreList.setRows(areaSize);

            Iterator<JSONObject> iterator = leaderboard.iterator();
            JSONObject friend = null;
            String punteggio = "";
            String friendNickname = "";
            while (iterator.hasNext()){
                friend = iterator.next();
                iterator.remove();
                friendNickname = String.valueOf(friend.get("nickname"));
                punteggio = String.valueOf(friend.get("punteggio"));
                scoreList.append("   "+friendNickname + "   " + punteggio +"\n");
            }

        }
        catch (IOException z){
            errorText.setText("Ops, qualcosa è andato storto :(");
            user.forceLogout();
            z.printStackTrace();
        }
        catch (ParseException x){
            errorText.setText("Ops, qualcosa è andato storto :(");
            user.forceLogout();
            x.printStackTrace();
        }

        menuPanel.repaint();
    }

    //Creazione campi di testo di output
    private JTextField setupTextField(int x, int y, int width, int height, String text, int fontSize){
        JTextField tField = new JTextField(text);
        tField.setBounds(x, y, width, height);
        tField.setOpaque(false);
        tField.setBorder(null);
        tField.setEditable(false);
        tField.setFont(new Font("Monospaced Bold", Font.PLAIN, fontSize));
        return tField;
    }

    //Creazione pulsanti
    private JButton setupButton(int x, int y, int width, int height){
        JButton button = new JButton();
        button.setBounds(x, y, width, height);
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);

        return button;
    }

    public JPanel getMenuPanel(){
        return this.menuPanel;
    }

    //Inizializzazione schermata del menuPanel
    private void getWallpaper(){
        screen = new JLabel();
        screen.setBounds(0,0,750, 520);
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("./Design/menu1.png"));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        Image dimg = img.getScaledInstance(screen.getWidth(), screen.getHeight(),
                Image.SCALE_SMOOTH);
        ImageIcon testImage = new ImageIcon(dimg);
        screen.setIcon(testImage);
    }

    public SocialPanel getSocialPanel(){
        return this.socialPanel;
    }

    public ChallengePanel getChallengePanel(){
        return  this.challengePanel;
    }
}
