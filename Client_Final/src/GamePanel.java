import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/*
 OVERVIEW: Un oggetto di tipo GamePanel rappresenta la schermata visualizzata da un utente mentre
 è in una sfida.
 */

public class GamePanel {

        private JPanel  gamePanel;
        private Client user;
        private ClientUI clientUI;

        private JTextField username;
        private JTextField friendUsername;
        private JLabel  screen;
        private JTextField errorText;
        private JButton skipButton;
        private JButton sendButton;
        private JTextField wordToSend;
        private JTextField wordToReceive;
        private  String EOM="_EOM";


        public GamePanel(Client user, ClientUI clientUI){
            gamePanel = new JPanel();
            gamePanel.setLayout(null);
            this.clientUI = clientUI;
            this.user = user;
            setButtons();
            setScreen();
        }
        //Inizializzazione pulsanti nel gamePanel
        public void setButtons(){

            skipButton = setupButton(499,183,87,48);
            skipButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        wordToSend.setText("");
                        user.inviaRichiestaSfida(""+EOM);
                    } catch (IOException z) {
                        errorText.setText("Ops, qualcosa è andato storto :(");
                        user.forceLogout();
                        z.printStackTrace();
                    }
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

        public void setInfo(String username, String challengeUsername){
            this.username.setText(username);
            this.friendUsername.setText(challengeUsername);
            refreshPage();
        }

        public void refreshPage(){
            gamePanel.repaint();
        }

        //Inizializzazione sfondo della schermata
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

        //Inizializzazione posizione e dimensione componenti presenti nel gamePanel.
        public void setScreen(){
            friendUsername = setupTextField(423,415,130,47,"",25);
            gamePanel.add(friendUsername);

            wordToReceive = setupTextField(305,180,180,49,"",26);
            gamePanel.add(wordToReceive);

            wordToSend = setupTextFieldInput(240,305,225,46,"",23);
            gamePanel.add(wordToSend);

            errorText = setupTextField(210, 75, 400, 47, "", 26);
            gamePanel.add(errorText);

            username = setupTextField(197, 415, 130, 47, "", 25);
            gamePanel.add(username);

            getWallpaper();
            gamePanel.add(screen);
            gamePanel.repaint();
        }

        public JPanel getPanel(){
            return this.gamePanel;
        }
        //Metodo per creare campi di testo di output verso l' utente
        private JTextField setupTextField(int x, int y, int width, int height, String text, int fontSize){
            JTextField tField = new JTextField(text);
            tField.setBounds(x, y, width, height);
            tField.setOpaque(false);
            tField.setBorder(null);
            tField.setEditable(false);
            tField.setFont(new Font("Monospaced Bold", Font.PLAIN, fontSize));
            return tField;
        }

        //Metodo per creare campi di testo di input per l'utente
        private JTextField setupTextFieldInput(int x, int y, int width, int height , String text, int fontSize){
            JTextField tField = new JTextField(text);
            tField.setBounds(x, y, width, height);
            tField.setBorder(null);
            tField.setEditable(true);
            tField.setFont(new Font("Monospaced Bold", Font.PLAIN, fontSize));
            return tField;
        }

        //Metodo per creare pulsanti.
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
