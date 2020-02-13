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
OVERVIEW: Schermata interattiva utilizzata per effettuale la sfida di traduzione del servizio GameQuizzle.
 */

public class ChallengePanel {
    private JPanel playPanel;
    private Client user;
    private ClientUI clientUI;

    private JTextField username;
    private JTextField score;
    private JButton logoutButton;
    private JButton refreshButton;
    private JLabel  screen;
    private JTextField best;
    private JTextArea scoreList;
    private JScrollPane scrollPane;

    private JTextField errorText;
    private JTextField minorErrorText;

    private JButton backButton;
    private JButton playButton;
    private JTextField friendToChallenge;

    public ChallengePanel(Client user, ClientUI clientUI){
        playPanel = new JPanel();
        playPanel.setLayout(null);
        this.clientUI = clientUI;
        this.user = user;
    }

    //Inizializzazione dimensione e coordinate delle componenti nel playPanel.
    public void setButtons(){
        logoutButton = setupButton(713,5, 32,30);
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    user.logout();
                }
                catch (IOException z){
                    user.forceLogout();
                }
                clientUI.switchToEntryPage();
            }
        });
        playPanel.add(logoutButton);

        refreshButton = setupButton(680, 120, 32, 30);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                errorText.setText("");
                minorErrorText.setText("");
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
                playPanel.repaint();
            }
        });
        playPanel.add(refreshButton);

        playButton = setupButton(272,257,50,50);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String query = friendToChallenge.getText();
                if(query.equals(""))
                    return;
                friendToChallenge.setText("");
                minorErrorText.setText("In attesa di risposta");
                playButton.setEnabled(false);
                try {
                    user.sfida(user.getNickname(), query);
                }
                catch (IOException z){
                    errorText.setText("Ops, qualcosa è andato storto :(");
                    user.forceLogout();
                    z.printStackTrace();
                }
                catch (InterruptedException y) {
                    errorText.setText("Ops, qualcosa è andato storto :(");
                    user.forceLogout();
                    y.printStackTrace();
                }
            }
        });
        playPanel.add(playButton);

        backButton = setupButton(364, 149, 62, 62);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientUI.switchToMenu();
            }
        });
        playPanel.add(backButton);

    }

    public void setMinorErrorText(String errorText){
        minorErrorText.setText(errorText);
        playPanel.repaint();
    }

    public void setEnabletToTrue(){
        playButton.setEnabled(true);
    }

    //Metodo che ordina e mostra a schermo il JSONArray leaderboard.
    private void updateUserScores(JSONArray leaderboard){
        if(leaderboard == null){
            user.forceLogout();
            errorText.setText("Ops, qualcosa è andato storto :(");
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

    public void refreshPage(){
        playButton.setEnabled(true);
        minorErrorText.setText("");
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

        playPanel.repaint();
    }

    //Inizializzazione sfondo
    private void getWallpaper(){
        screen = new JLabel();
        screen.setBounds(0,0,750, 520);
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("./Design/play.png"));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        Image dimg = img.getScaledInstance(screen.getWidth(), screen.getHeight(),
                Image.SCALE_SMOOTH);
        ImageIcon testImage = new ImageIcon(dimg);
        screen.setIcon(testImage);
    }

    //Inizializzazione coordinate e dimensioni dei componenti presenti nel playPanel
    public void setScreen(){
        errorText = setupTextField(210, 75, 400, 47, "", 26);
        playPanel.add(errorText);

        minorErrorText = setupTextField(18, 318, 320, 20, "", 18);
        playPanel.add(minorErrorText);

        friendToChallenge = setupTextFieldInput(90,265,160,35,"",16);
        playPanel.add(friendToChallenge);

        score = setupTextField(55, 102, 80, 47, "", 23);
        playPanel.add(score);

        best = setupTextField(510, 163, 180, 47, "", 23);
        playPanel.add(best);

        username = setupTextField(80, 50, 180, 47, "", 25);
        playPanel.add(username);

        setupLeaderBoard(490, 215, 200, 263);
        playPanel.add(scrollPane);

        getWallpaper();
        playPanel.add(screen);
        playPanel.repaint();
    }

    //Inizializzazione classifica scorrevole nel playPanel
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

    public JPanel getPanel(){
        return this.playPanel;
    }

    //Creazione campi di testo di output del playPanel
    private JTextField setupTextField(int x, int y, int width, int height, String text, int fontSize){
        JTextField tField = new JTextField(text);
        tField.setBounds(x, y, width, height);
        tField.setOpaque(false);
        tField.setBorder(null);
        tField.setEditable(false);
        tField.setFont(new Font("Monospaced Bold", Font.PLAIN, fontSize));
        return tField;
    }

    //Creazione campi di testo di input del playPanel
    private JTextField setupTextFieldInput(int x, int y, int width, int height , String text, int fontSize){
        JTextField tField = new JTextField(text);
        tField.setBounds(x, y, width, height);
        tField.setBorder(null);
        tField.setEditable(true);
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

}
