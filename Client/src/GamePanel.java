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

public class GamePanel {

        private JPanel gamePanel;
        private Client user;
        private JFrame window;

        private JTextField username;
        private JTextField score;
        private JTextField friendUsername;
        private JLabel  screen;
        private JTextField errorText;
        private JButton skipButton;
        private JButton sendButton;
        private JTextField wordToSend;
        private JTextField wordToReceive;
        private Timer timer;
        private  String EOM="_EOM";


        public GamePanel(Client user, JFrame window){
            gamePanel = new JPanel();
            gamePanel.setLayout(null);
            this.window = window;
            this.user = user;
            setButtons();
            setScreen();
        }


        public void setButtons(){

            skipButton = setupButton(499,183,87,48);
            skipButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            });
            gamePanel.add(skipButton);

            sendButton = setupButton(470, 297, 64, 64);
            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        String traduzione = wordToSend.getText();
                        wordToSend.setText("");
                        user.inviaRichiestaSfida(traduzione+EOM);
                    }
                    catch (IOException z){
                        errorText.setText("Ops, qualcosa è andato storto :(");
                        user.forceLogout();
                        z.printStackTrace();
                    }
                }
            });
            gamePanel.add(sendButton);

        }

        public void setNewWord(String newWord){
            wordToReceive.setText(newWord);
            gamePanel.repaint();
        }

        public void setInfo(String username, String score, String challengeUsername){
            this.username.setText(username);
            this.score.setText(score);
            this.friendUsername.setText(challengeUsername);
        }

        public void refreshPage(){
            //errorText.setText("");
            gamePanel.repaint();
        }

        private void getWallpaper(){
            screen = new JLabel();
            screen.setBounds(0,0,750, 520);
            BufferedImage img = null;
            try {
                img = ImageIO.read(new File("./Design/game1.png"));
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
            friendUsername = setupTextField(350,500,60,60,"",25);
            gamePanel.add(friendUsername);

            wordToReceive = setupTextField(305,180,180,49,"",26);
            gamePanel.add(wordToReceive);

            wordToSend = setupTextFieldString(240,305,225,46,"",23);
            gamePanel.add(wordToSend);

            errorText = setupTextField(210, 75, 400, 47, "", 26);
            gamePanel.add(errorText);

            score = setupTextField(55, 102, 80, 47, "", 23);
            gamePanel.add(score);

            username = setupTextField(80, 50, 180, 47, "", 25);
            gamePanel.add(username);

            getWallpaper();
            gamePanel.add(screen);
            gamePanel.repaint();
        }

        public JPanel getPanel(){
            return this.gamePanel;
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
