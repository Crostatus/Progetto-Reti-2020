import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ResultPanel {

    private JPanel resultPanel;
    private JButton backButton;
    private JTextField scoreText;
    private JTextField resultField;
    private JLabel screen;
    private JTextField errorText;

    private Client user;
    private JFrame window;


    public ResultPanel(Client user, JFrame window){
        this.user = user;
        this.window = window;
        this.resultPanel = new JPanel();
        this.resultPanel.setLayout(null);

        this.errorText = setupTextField(30, 230, 60, 50, "", 24);
        resultPanel.add(errorText);


        this.scoreText = setupTextField(10,50, 730, 70, "", 28);
        resultPanel.add(scoreText);

        this.resultField = setupTextField(10, 140, 730, 70, "", 23);
        resultPanel.add(resultField);

        this.backButton = setupButton(320, 344, 107, 107);
        backButton.setEnabled(false);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClientUI.switchToMenu();
            }
        });
        resultPanel.add(backButton);

        getWallpaper();
        resultPanel.add(screen);

        resultPanel.repaint();

    }

    public void setResultString(String finalResult){
        resultField.setText(finalResult);
        backButton.setEnabled(true);
        resultPanel.repaint();

    }

    public void waitFinalResult(){
        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(!scoreText.getText().contains("Hai guadagnato")){
                    try {
                        resultField.repaint();
                        String finalResult = user.riceviRisposta();
                        resultField.setText(finalResult);
                    }
                    catch (IOException z){
                        errorText.setText("Ops, qualcosa è andato storto! :(");
                        user.forceLogout();
                        z.printStackTrace();
                    }
                    System.out.println("Qui ci arrivo");

                }
                backButton.setEnabled(true);
                resultPanel.repaint();
                System.out.println("Sto Aspettando...");
            }
        };
        Timer timer = new Timer(100 ,taskPerformer);
        timer.setRepeats(false);
        timer.start();

    }

    public void setBackButtonEnabled(boolean value){
        backButton.setEnabled(true);
        resultPanel.repaint();
    }

    public void setMyScore(String message){
        scoreText.setText(message);
        resultPanel.repaint();
        waitFinalResult();
    }

    public void refreshPage(){
        errorText.setText("");
        scoreText.setText("");
        resultField.setText("");
        resultPanel.repaint();
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

    private JButton setupButton(int x, int y, int width, int height){

        JButton button = new JButton();
        button.setBounds(x, y, width, height);
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);

        return button;
    }

    public JPanel getPanel(){
        return resultPanel;
    }

    private void getWallpaper(){
        screen = new JLabel();
        screen.setBounds(0,0,750, 520);
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("./Design/result.png"));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        Image dimg = img.getScaledInstance(screen.getWidth(), screen.getHeight(),
                Image.SCALE_SMOOTH);
        ImageIcon testImage = new ImageIcon(dimg);
        screen.setIcon(testImage);
    }
}
