import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;

public class EntryPanel {
    private JPanel entryPanel;
    private Client user;
    private ClientUI clientUI;

    private JLabel entryPage;
    private JTextField usernameArea;
    private JPasswordField passwordArea;
    private JButton loginButton;
    private JButton registerButton;
    private JTextField errorLogin;
    private JTextField errorRegister;

    public EntryPanel(Client user, ClientUI clientUI) {
        this.clientUI = clientUI;
        this.user = user;
        entryPanel = new JPanel();
        entryPanel.setLayout(null);
        usernameArea = new JTextField("", 10);
        usernameArea.setEditable(true);
        usernameArea.setBounds(310, 230, 130, 28);
        entryPanel.add(usernameArea);
        passwordArea = new JPasswordField("", 10);
        passwordArea.setEditable(true);
        passwordArea.setBounds(310,300, 130, 28);
        entryPanel.add(passwordArea);

        errorLogin = getErrorField(120, 445);
        errorRegister = getErrorField(405, 445);
        entryPanel.add(errorLogin);
        entryPanel.add(errorRegister);



        loginButton = getButton(107, 376);
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String nickname = usernameArea.getText();
                String password = String.valueOf(passwordArea.getPassword());
                System.out.println("Username: "+nickname+" password: "+password);
                usernameArea.setText("");
                passwordArea.setText("");
                String risposta ="";
                try {
                    risposta = user.login(nickname, password);
                }
                catch (IOException e){
                    //label con scritto "server non raggiungibile!
                    risposta = "Server non raggiungibile!";
                }
                System.out.println("Risposta: " + risposta);
                if(risposta.equals("OK")){
                    clientUI.switchToMenu();
                }
                else {
                    errorLogin.setText(risposta + " :(");
                    errorRegister.setText("");
                    entryPanel.repaint();
                }

            }
        });
        registerButton = getButton(425, 371);
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String nickname = usernameArea.getText();
                String password = String.valueOf(passwordArea.getPassword());
                System.out.println("Username: "+nickname+" password: "+password);
                usernameArea.setText("");
                passwordArea.setText("");
                String risposta ="";
                try {
                    risposta = user.registra(nickname, password);
                }
                catch (IOException e){
                    //label con scritto "server non raggiungibile!
                    risposta = "Server non raggiungibile!";
                }
                catch (NotBoundException e){
                    risposta = "Server non raggiungible!";
                }
                if(risposta.equals("Operazione avvenuta con successo")){
                    try {
                        risposta = user.login(nickname, password);
                    }
                    catch (IOException e){
                        //label con scritto "server non raggiungibile!
                        risposta = "Server non raggiungibile!";
                    }
                    System.out.println("Risposta: " + risposta);
                    if(risposta.equals("OK")){
                        clientUI.switchToMenu();
                    }
                    else {
                        errorLogin.setText(risposta + " :(");
                        errorRegister.setText("");
                        entryPanel.repaint();
                    }
                }
                else {
                    errorRegister.setText(risposta + " :(");
                    errorLogin.setText("");
                    entryPanel.repaint();
                }

            }
        });

        entryPanel.add(loginButton);
        entryPanel.add(registerButton);

        getWallpaper();
        entryPanel.add(entryPage);
    }

    public void clearErrors(){
        errorLogin.setText("");
        errorRegister.setText("");

    }


    private JButton getButton(int x, int y){
        JButton button = new JButton();
        button.setBounds(x, y, 217, 63);
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);

        return button;
    }

    private JTextField getErrorField(int x, int y){
        JTextField text = new JTextField("");
        text.setBounds(x, y, 320, 30);
        text.setOpaque(false);
        text.setBorder(null);
        text.setFont(new Font("Segoe Script", Font.PLAIN, 20));
        text.setEditable(false);
        return text;
    }


    private void getWallpaper(){
        entryPage = new JLabel();
        entryPage.setBounds(0,0,750, 520);
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("./Design/entry_Page0.png"));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        Image dimg = img.getScaledInstance(entryPage.getWidth(), entryPage.getHeight(),
                Image.SCALE_SMOOTH);
        ImageIcon testImage = new ImageIcon(dimg);
        entryPage.setIcon(testImage);
    }

    public JPanel getEntryPanel(){
        return this.entryPanel;
    }

}
