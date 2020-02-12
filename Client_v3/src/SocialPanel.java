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

public class SocialPanel {
    private JPanel socialPanel;
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
    private JButton addButton;
    private JTextField friendToAdd;


    public SocialPanel(Client user, ClientUI clientUI){
        socialPanel = new JPanel();
        socialPanel.setLayout(null);
        this.clientUI = clientUI;
        this.user = user;



    }


    public void setButtons(){
        logoutButton = setupButton(713,5, 32,30);
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    user.logout();
                }
                catch (IOException z){
                    //mostra alert con "Ops, qualcosa è andato storto :(
                    z.printStackTrace();
                }
                clientUI.switchToEntryPage();
            }
        });
        socialPanel.add(logoutButton);

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
                socialPanel.repaint();
            }
        });
        socialPanel.add(refreshButton);

        addButton = setupButton(247,310,50,49);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String query = friendToAdd.getText();
                friendToAdd.setText("");
                try {
                    String risposta = user.aggiungi_amico(user.getNickname(), query);
                    minorErrorText.setText(risposta);
                    if(risposta.equals("Utente aggiunto correttamente")){
                        refreshPage();
                    }
                }
                catch (IOException z){
                    errorText.setText("Ops, qualcosa è andato storto :(");
                    user.forceLogout();
                    z.printStackTrace();
                }




            }
        });
        socialPanel.add(addButton);

        backButton = setupButton(364, 149, 62, 62);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientUI.switchToMenu();
            }
        });
        socialPanel.add(backButton);

    }

    private void updateUserScores(JSONArray leaderboard){
        //1° fase inserire il mio punteggio
        if(leaderboard == null){
            //mostra testo errore
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
        //mostro il bimbo con più punti di tutti
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

        socialPanel.repaint();
    }

    private void getWallpaper(){
        screen = new JLabel();
        screen.setBounds(0,0,750, 520);
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("./Design/social.png"));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        Image dimg = img.getScaledInstance(screen.getWidth(), screen.getHeight(),
                Image.SCALE_SMOOTH);
        ImageIcon testImage = new ImageIcon(dimg);
        screen.setIcon(testImage);
    }

    public void setScreen(){
        errorText = setupTextField(210, 75, 400, 47, "", 26);
        socialPanel.add(errorText);

        minorErrorText = setupTextField(18, 280, 320, 20, "", 18);
        socialPanel.add(minorErrorText);

        friendToAdd = setupTextFieldString(90,315,140,35,"",16);
        socialPanel.add(friendToAdd);

        score = setupTextField(55, 102, 80, 47, "", 23);
        socialPanel.add(score);

        best = setupTextField(510, 163, 180, 47, "", 23);
        socialPanel.add(best);

        username = setupTextField(80, 50, 180, 47, "", 25);
        socialPanel.add(username);

        setupLeaderBoard(490, 215, 200, 263);
        socialPanel.add(scrollPane);

        getWallpaper();
        socialPanel.add(screen);
        socialPanel.repaint();
    }

    private void setupLeaderBoard(int x, int y, int width, int height){
        scoreList = new JTextArea();
        scoreList.setEditable(false);
        String fontName = String.valueOf(scoreList.getFont());
        scoreList.setFont(new Font(fontName,Font.PLAIN,23));
        scoreList.setColumns(1);
        scoreList.setOpaque(false);
        //scoreList.setBounds(0, 0, 100, 200);
        scrollPane = new JScrollPane(scoreList);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(x,y,width, height);
    }

    public JPanel getPanel(){
        return this.socialPanel;
    }

    private JTextField setupTextField(int x, int y, int width, int height, String text, int fontSize){
        JTextField tField = new JTextField(text);
        tField.setBounds(x, y, width, height);
        tField.setOpaque(false);
        tField.setBorder(null);
        tField.setEditable(false);
        tField.setFont(new Font("Monospaced Bold", Font.PLAIN, fontSize));
        return tField;
    }

    private JTextField setupTextFieldString(int x, int y, int width, int height , String text, int fontSize){
        JTextField tField = new JTextField(text);
        tField.setBounds(x, y, width, height);
        tField.setBorder(null);
        tField.setEditable(true);
        tField.setFont(new Font("Monospaced Bold", Font.PLAIN, fontSize));
        return tField;
    }

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
