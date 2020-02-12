import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

public class ChallengeFrame {
    private UDP_Listener user;

    private JFrame smallWindow;
    private JPanel smallPanel;
    private JLabel smallScreen;
    private JButton yesButton;
    private JButton noButton;

    private JTextField challengeMessage;
    private String friendChallenge;

    public ChallengeFrame(UDP_Listener user){
        this.user = user;

        smallWindow = new JFrame("Someone challenged you!");
        smallWindow.setSize(375,275);
        smallWindow.setLocation(1275,270);
        smallWindow.setResizable(false);
        smallWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    user.inviaRisposta("no",friendChallenge);
                    smallWindow.setVisible(false);
                    smallWindow.dispose();
                }catch (IOException z){
                    // errorText.setText("Ops, qualcosa è andato storto :(");
                    //user.forceLogout();
                    z.printStackTrace();
                    smallWindow.setVisible(false);
                    smallWindow.dispose();
                }
            }
        });

        smallPanel = new JPanel();
        smallPanel.setLayout(null);


        yesButton = setupButton(5, 149, 156, 76);
        yesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    user.inviaRisposta("si",friendChallenge);
                    smallWindow.setVisible(false);
                    smallWindow.dispose();
                }catch (IOException z){
                   // errorText.setText("Ops, qualcosa è andato storto :(");
                    //user.forceLogout();
                    z.printStackTrace();
                    smallWindow.setVisible(false);
                    smallWindow.dispose();
                }
            }
        });
        smallPanel.add(yesButton);

        noButton = setupButton(210, 149, 159, 76);
        noButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    user.inviaRisposta("no",friendChallenge);
                    smallWindow.setVisible(false);
                    smallWindow.dispose();
                }catch (IOException z){
                    // errorText.setText("Ops, qualcosa è andato storto :(");
                    //user.forceLogout();
                    z.printStackTrace();
                    smallWindow.setVisible(false);
                    smallWindow.dispose();
                }
            }
        });
        smallPanel.add(noButton);

        challengeMessage = setupTextField(4,45,360,60,user.getChallengeMessage(),16);
        StringTokenizer token = new StringTokenizer(user.getChallengeMessage());
        friendChallenge = token.nextToken();
        smallPanel.add(challengeMessage);



        getWallpaper();
        smallPanel.add(smallScreen);


        smallWindow.setContentPane(smallPanel);
        smallWindow.setVisible(true);
        smallWindow.toFront();
        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                smallWindow.setVisible(false);
                smallWindow.dispose();
        }
        };
        Timer timer = new Timer(10000 ,taskPerformer);
        timer.setRepeats(false);
        timer.start();

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


    private JTextField setupTextField(int x, int y, int width, int height, String text, int fontSize){
        JTextField tField = new JTextField(text);
        tField.setBounds(x, y, width, height);
        tField.setOpaque(false);
        tField.setBorder(null);
        tField.setEditable(false);
        tField.setFont(new Font("Monospaced Bold", Font.PLAIN, fontSize));
        return tField;
    }

    private void getWallpaper(){
        smallScreen = new JLabel();
        smallScreen.setBounds(0,0,375, 275);
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("./Design/challenge.png"));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        Image dimg = img.getScaledInstance(smallScreen.getWidth(), smallScreen.getHeight(),
                Image.SCALE_SMOOTH);
        ImageIcon testImage = new ImageIcon(dimg);
        smallScreen.setIcon(testImage);
    }

}
