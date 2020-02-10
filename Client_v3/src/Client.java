import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\033[0;34m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    private  String EOM="_EOM";
    private  int CHUNKSIZE = 128;
    private InetSocketAddress address;
    private SocketChannel client;
    private ByteBuffer buffer;
    private String nickname;
    private int registerPort;
    private int tcpPort;
    private boolean login;
    private AtomicBoolean inAMatch;
    private UDP_Listener challengeListener;
    private long startTime;
    private GamePanel gamePanel;
    private PlayPanel playPanel;
    private ResultPanel resultPanel;


    public Client(int registerPort, int tcpPort) throws SocketException{
        this.registerPort=registerPort;
        this.tcpPort = tcpPort;
        login = false;
        inAMatch = new AtomicBoolean(false);
    }

    public void setPlayPanel(PlayPanel playPanel){
        this.playPanel = playPanel;
    }

    public void setGamePanel(GamePanel gamePanel){
        this.gamePanel = gamePanel;
    }

    public void setResultPanel(ResultPanel resultPanel){
        this.resultPanel = resultPanel;
    }

    public void forceLogout(){
        this.login = false;
        setBoolean(false);
    }

    // setto inAMatch a val
    public void setBoolean(boolean val){
        inAMatch.set(val);
    }

    // restituisce il valore booleano di inAMactch
    public boolean getBoolean(){
        return inAMatch.get();
    }

    // operazione per registrare l'utente
    public String registra(String nickname, String password) throws NotBoundException,IOException {
        if(!login) {
            if(controllo_credenziali(nickname,password)) {
                Registry reg = LocateRegistry.getRegistry(registerPort);
                this.nickname = nickname;
                RegistrazioneInterface registra = (RegistrazioneInterface) reg.lookup("iscrizione");
                int risultato = registra.registra_utente(nickname, password);
                if (risultato == 0) {
                    System.out.println("Operazione avvenuta con successo");
                    return "Operazione avvenuta con successo";
                } else if (risultato == 1)
                    System.out.println(RED+"Nickname già presente"+RESET);
                return "Nickname già presente!";
            }
            return "Password non valida";
        }
        System.out.println(RED+"Registrazione non permessa, login gia effettuato"+RESET);
        return "Registrazione non permessa";
    }

    // richiesta di login di nickname
    public String login(String nickname, String password) throws IOException {
        if(login){
            System.out.println(RED+"Login già effettuato"+RESET);
            return "Login già effettuato";
        }
        if(controllo_credenziali(nickname,password)) {
            this.nickname = nickname;
            address = new InetSocketAddress("localhost", tcpPort);
            client = SocketChannel.open(address);
            buffer = ByteBuffer.allocate(CHUNKSIZE);
            int port = client.socket().getLocalPort();
            //inAMatch.set(true);
            challengeListener = new UDP_Listener(inAMatch,port,nickname, client);
            //challengeListener.setDaemon(true);
            challengeListener.start();
            //System.out.println(port);
            String messaggio = "login " + nickname + " " + password + EOM;
            inviaRichiesta(messaggio);
            String risposta = riceviRisposta();
            if (risposta.equals("OK")) {
                login = true;
                return risposta;
            }
            return risposta;
        }
        return "Credenziali errate";
    }

    // richiesta di logout
    public void logout()throws IOException{
        if(login && !inAMatch.get()) {
            String richiesta = "logout " + nickname + EOM;
            inviaRichiesta(richiesta);
            riceviRisposta();
            login = false;
            //System.out.println(nickname+"        "+inAMatch.get());
            challengeListener.interrupt();
            // la close la devo fare solo quando effettuo l'operazione di logout
            client.close();
        }
        else System.out.println(RED+"Login non effettuato"+RESET);
    }

    // richiesta di stampare la lista amici di nickname
    public void lista_amici(String nickname) throws IOException, ParseException {
        if(login && !inAMatch.get()){
            if(controllo_credenziali(nickname)) {
                String richiesta = "lista_amici " + nickname + EOM;
                inviaRichiesta(richiesta);
                riceviRispostaListaAmici();
            }
        }
        else System.out.println(RED+"Login non effettuato"+RESET);
    }

    public int mostra_punteggio(String nikname) throws IOException, ParseException{
        if(login && !inAMatch.get()){
            if(controllo_credenziali(nickname)) {
                String richiesta = "mostra_punteggio " + nickname + EOM;
                inviaRichiesta(richiesta);
                int punteggio = Integer.parseInt(riceviRisposta());
                System.out.println("Punteggio: " + punteggio);
                return punteggio;
            }
            return -1;
        }
        else {
            System.out.println(RED+"Login non effettuato"+RESET);
            return -1;
        }
    }

    public JSONArray mostra_classifica(String nickname)throws IOException,ParseException{
        if(login && !inAMatch.get()){
            if(controllo_credenziali(nickname)) {
                String richiesta = "mostra_classifica " + nickname + EOM;
                inviaRichiesta(richiesta);
                return riceviRispostaClassifica();
            }
        }
        else {
            System.out.println(RED+"Login non effettuato"+RESET);
            return null;
        }
        return null;
    }

    // richiesta di aggiungere friendNickname alla lista amici di myNickname e viceversa
    public String aggiungi_amico(String myNickname, String friendNickname) throws IOException {
        String risposta = "";
        if(login && !inAMatch.get()) {
            if(myNickname.equals(friendNickname)) {
                System.out.println(RED+"Non puoi aggiungere te stesso"+RESET);
                return "Sei già il tuo migliore amico!";
            }
            String richiesta = "aggiungi_amico " + myNickname + " " + friendNickname + EOM;
            inviaRichiesta(richiesta);
            risposta = riceviRisposta();
        }
        else {
            System.out.println(RED+"Login non effettuato"+RESET);
            risposta = "Login non effettuato";
        }
        return risposta;
    }
    /*
    public String sfida(String myNickname, String friendNickname) throws IOException, InterruptedException {
        if(login && !inAMatch.get()){
            if(myNickname.equals(friendNickname)) {
                System.out.println(RED+"Non puoi inviare una richiesta di sfida a te stesso"+RESET);
                return "La frase trash";
            }
            String richiesta = "sfida "+myNickname+" "+friendNickname+EOM;
            inAMatch.set(true);
            inviaRichiesta(richiesta);
            String risposta = riceviRisposta();
            if(risposta.equals("Richiesta di sfida non accettata")){
                inAMatch.set(false);
                return risposta;
            }
            else{
                // switch to game Page
                startTime = System.currentTimeMillis();
                gioca();
            }
            System.out.println(nickname+" "+inAMatch.get());
        }
        else {
            System.out.println(RED+"Login non effettuato"+RESET);
            return "Ops, qualcosa è andato storto :(";
        }
    }

    private String gioca()throws IOException{
        Scanner scanner = new Scanner(System.in);
        int i;
        for(i=0; i<3; i++){
            String parolaTradotta = scanner.nextLine();
            if(System.currentTimeMillis()-startTime>10000) {
                scanner.close();
                break;
            }
            inviaRichiesta(parolaTradotta + EOM);
            riceviRisposta();
        }
        System.out.println("Aspetto la risposta finale");
        scanner.close();
        riceviRisposta();
        inAMatch.set(false);
    }*/

    public void sfida(String myNickname, String friendNickname, String score) throws IOException, InterruptedException {
        if(login && !inAMatch.get()){
            if(myNickname.equals(friendNickname)) {
                System.out.println(RED+"Non puoi inviare una richiesta di sfida a te stesso"+RESET);
                playPanel.setMinorErrorText("Non puoi sfidare te stesso!");
                return;
            }
            String richiesta = "sfida "+myNickname+" "+friendNickname+EOM;
            inAMatch.set(true);
            inviaRichiesta(richiesta);
            String risposta = riceviRisposta();
            if(risposta.contains("Via alla sfida di traduzione:")){
                // switch to game Page
                ClientUI.switchToGamePage();
                gamePanel.setInfo(nickname,score,friendNickname);
                risposta = risposta.replace("Via alla sfida di traduzione:","");
                StringTokenizer token = new StringTokenizer(risposta);
                risposta = token.nextToken();
                gamePanel.setNewWord(risposta);
                //startTime = System.currentTimeMillis();
            }
            else{
                inAMatch.set(false);
                playPanel.setMinorErrorText(risposta);
                playPanel.setEnabletToTrue();
                return;
            }
        }
        else {
            System.out.println(RED+"Login non effettuato"+RESET);
            playPanel.setMinorErrorText("Ops, qualcosa è andato storto :(");
            return;
        }
    }

    private void gioca(String primaParola)throws IOException{
        String nuovaParola = primaParola;
        while (!nuovaParola.contains("Hai guadagnato")){
            System.out.println("Fa solo 1 iterazione");
            //gamePanel.setNewWord(nuovaParola);
            nuovaParola = riceviRisposta();
        }
    }

    // restituisce true se il controllo va a buon fine
    private boolean controllo_credenziali(String nickname, String password){
        if (nickname == null || nickname.length() <= 3 || nickname.length() >= 15){
            System.out.println(RED+"Nickname non valido"+RESET);
            return false;
        }
        if(password == null || password.length() <= 3 || password.length() >= 15 || noDigit(password)){
            System.out.println(RED+"password non valida"+RESET);
            return false;
        }
        return true;
    }

    // restituisce true se il controllo va a buon fine
    private boolean controllo_credenziali(String nickname){
        if (nickname == null || nickname.length() <= 3 || nickname.length() >= 15){
            System.out.println(RED+"Nickname non valido"+RESET);
            return false;
        }
        return true;
    }

    // restituisce true se la password contiene meno di due cifre, false altrimenti
    private boolean noDigit(String p){
        int count=0;
        for(int i=0;i<p.length();i++)
            if(Character.isDigit(p.charAt(i))) count++;
        if(count<2) return true;
        return false;
    }

    // invia la richiesta al server
    public void inviaRichiesta(String richiesta) throws IOException {
        byte[] data = richiesta.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println(BLUE+"Ho inviato: " + richiesta+RESET);
        buffer.clear();
    }

    // invia la richiesta al server
    public void inviaRichiestaSfida(String richiesta) throws IOException {
        byte[] data = richiesta.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println(BLUE+"Ho inviato: " + richiesta+RESET);
        buffer.clear();
        String receiveWord = riceviRisposta();
        if(receiveWord.contains("Hai tradotto correttamente")){
            //da modificare gli stringoni perchè non ci entra tutto!
            ClientUI.switchToResult();
            System.out.println("Sto per cambiare il panel!");
            resultPanel.setMyScore(receiveWord);
            resultPanel.getPanel().repaint();
        }
        else{
            System.out.println("Mi arriva: "+receiveWord);
            gamePanel.setNewWord(receiveWord);
        }
    }

    // riceve la risposta dal server
    public String riceviRisposta() throws IOException {
        String result="";
        int bytesRead = 0;
        byte[] data2;
        while(!result.endsWith(EOM)){
            bytesRead = client.read(buffer);
            buffer.flip();
            data2 = new byte[bytesRead];
            buffer.get(data2);
            result+= new String(data2);
            //System.out.println("Byte letti " + bytesRead+" "+ result);
            buffer.clear();
        }
        result = result.replace("_EOM", "");

        System.out.println(GREEN+"Risposta: " + result+RESET);
        return result;
    }

    // riceve la risposta dal server in formato Json
    private JSONArray riceviRispostaClassifica() throws IOException, ParseException {
        String result="";
        int bytesRead = 0;
        byte[] data2;
        while(!result.endsWith(EOM)){
            bytesRead = client.read(buffer);
            buffer.flip();
            data2 = new byte[bytesRead];
            buffer.get(data2);
            result+= new String(data2);
            buffer.clear();
        }
        result = result.replace("_EOM", "");

        //System.out.println(result);
        JSONParser parser = new JSONParser();
        Object obj= parser.parse(result);
        JSONArray classifica = (JSONArray) obj;
        //stampaClassifica(classifica);
        return classifica;
    }

    private void stampaClassifica(JSONArray classifica){
        Iterator<JSONObject> iterator = classifica.iterator();
        JSONObject currentUser = null;
        while (iterator.hasNext()){
            currentUser = iterator.next();
            iterator.remove();
            System.out.println("nickname: "+currentUser.get("nickname")+" ,punteggio: "+currentUser.get("punteggio"));
        }
    }

    // riceve la risposta dal server in formato Json
    private void riceviRispostaListaAmici() throws IOException, ParseException {
        String result="";
        int bytesRead = 0;
        byte[] data2;
        while(!result.endsWith(EOM)){
            bytesRead = client.read(buffer);
            buffer.flip();
            data2 = new byte[bytesRead];
            buffer.get(data2);
            result+= new String(data2);
            buffer.clear();
        }
        result = result.replace("_EOM", "");

        //System.out.println(result);
        JSONParser parser = new JSONParser();
        Object obj= parser.parse(result);
        JSONObject utente = (JSONObject) obj;
        stampaListaAmici(utente);
    }

    // stampa la lista amici dell'utente dato il suo JsonObject
    private void stampaListaAmici(JSONObject utente){
        String nickname = (String) utente.get("nickname");
        JSONArray lista = (JSONArray) utente.get("lista_amici");

        Iterator<JSONObject> iterator = lista.iterator();
        System.out.println(GREEN+"Lista amici "+nickname+": "+RESET);
        while (iterator.hasNext()){
            JSONObject amico = iterator.next();
            System.out.print(GREEN+amico.get("amico")+" "+RESET);
        }
        System.out.println();
    }

    public String getNickname(){
        return this.nickname;
    }
}