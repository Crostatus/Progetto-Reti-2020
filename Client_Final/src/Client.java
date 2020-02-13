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
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
/*
OVERVIEW: Un oggetto di tipo Client implementa l' interazione con il server tramite TCP e RMI. Esso invia e riceve
          i comandi richiesti dall' utente, gestendo anche parte degli eventi dell' interfaccia grafica.
 */

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
    private GamePanel gamePanel;
    private ChallengePanel challengePanel;
    private ResultPanel resultPanel;
    private ClientUI clientUI;


    public Client(int registerPort, int tcpPort, ClientUI clientUI) throws SocketException{
        this.registerPort=registerPort;
        this.tcpPort = tcpPort;
        this.clientUI = clientUI;
        this.login = false;
        inAMatch = new AtomicBoolean(false);
    }

    public void setChallengePanel(ChallengePanel challengePanel){
        this.challengePanel = challengePanel;
    }

    public void setGamePanel(GamePanel gamePanel){
        this.gamePanel = gamePanel;
    }

    public void setResultPanel(ResultPanel resultPanel){
        this.resultPanel = resultPanel;
    }

    //Metodo per resettare i valori di default delle variabili "login" e "inAMatch" lato client, in caso il server non è raggiungibile.
    public void forceLogout(){
        this.login = false;
        setInAMatch(false);
    }

    public void setInAMatch(boolean val){
        inAMatch.set(val);
    }

    // Operazione di registrazione di un utente con inclusi opportuni controlli di validità della coppia <nickname, password>,
    // utilizzando l' oggetto condiviso tramite RMI dal server per memorizzare le credenziali.
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

    // Operazione di login per un utente che avviene con successo se e solo se il nickname corrisponde ad un utente registrato al servizio
    // e la password combacia con quella impostata al momento della registrazione
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
            challengeListener = new UDP_Listener(inAMatch,port,nickname, client,clientUI);
            challengeListener.start();

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

    // Operazione di logout richiesta da un utente per segnalare al server di essere rimosso dalla lista delle persone online.
    // Logout, per avere successo, richiede che il client debba già aver effettuato il login.
    public void logout()throws IOException{
        if(login && !inAMatch.get()) {
            String richiesta = "logout " + nickname + EOM;
            inviaRichiesta(richiesta);
            riceviRisposta();
            login = false;
            challengeListener.interrupt();
            client.close();
        }
        else
            System.out.println(RED+"Login non effettuato"+RESET);
    }

    // Metodo per richiedere al server la propria lista amici
    public void lista_amici(String nickname) throws IOException, ParseException {
        if(login && !inAMatch.get()){
            if(controllo_credenziali(nickname)) {
                String richiesta = "lista_amici " + nickname + EOM;
                inviaRichiesta(richiesta);
                riceviRispostaListaAmici();
            }
        }
        else
            System.out.println(RED+"Login non effettuato"+RESET);
    }

    // Metodo per richiedere al server il proprio punteggio
    public int mostra_punteggio(String nickname) throws IOException, ParseException{
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

    // Metodo per ottenere la classifica composta dagli utenti con cui sono amico
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

    // Metodo per aggiungere un utente alla propria lista amici. L' aggiunta va a buon fine se l' utente di nome "friendNickname"
    // sia registrato al servizio e non appartenga già alla mia lista amici. L' amicizia non richiede di essere confermata da parte
    // dell' utente che riceve la richiesta.
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

    //Metodo per inviare una richiesta di sfida all utente chiamato "friendNickname". L' utente sfidato per poter ricevere la richiesta di
    //sfida deve appartenere alla mia lista di amici.
    public void sfida(String myNickname, String friendNickname) throws IOException, InterruptedException {
        if(login && !inAMatch.get()){
            if(myNickname.equals(friendNickname)) {
                System.out.println(RED+"Non puoi inviare una richiesta di sfida a te stesso"+RESET);
                challengePanel.setMinorErrorText("Non puoi sfidare te stesso!");
                return;
            }
            String richiesta = "sfida "+myNickname+" "+friendNickname+EOM;
            inAMatch.set(true);
            inviaRichiesta(richiesta);
            String risposta = riceviRisposta();
            if(risposta.contains("Via alla sfida di traduzione:")){
                // switch to game Page
                clientUI.switchToGamePage();
                gamePanel.setInfo(nickname,friendNickname);
                risposta = risposta.replace("Via alla sfida di traduzione:","");
                resultPanel.setStartTimer();
                StringTokenizer token = new StringTokenizer(risposta);
                risposta = token.nextToken();
                gamePanel.setNewWord(risposta);
                //startTime = System.currentTimeMillis();
                return;
            }
            else{
                inAMatch.set(false);
                challengePanel.setMinorErrorText(risposta);
                challengePanel.setEnabletToTrue();
                return;
            }
        }
        else {
            System.out.println(RED+"Login non effettuato"+RESET);
            challengePanel.setMinorErrorText("Ops, qualcosa è andato storto :(");
            return;
        }
    }

    /* Controllo di validità della coppia <nickname, password>.
      controllo_credenziali = true => { nickname : (3 <= nickname.length <= 15) and (nickname != null)                                           }
                                      { password : (3 <= password.length <= 15) and (password != null) and (password contiene almeno due numeri) }
                            = false => altrimenti          */
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

    /* Controllo di validità del nickname di un utente
      controllo_credenziali = true => { nickname : (3 <= nickname.length <= 15) and (nickname != null) }                                          }
                            = false => altrimenti */
    private boolean controllo_credenziali(String nickname){
        if (nickname == null || nickname.length() <= 3 || nickname.length() >= 15){
            System.out.println(RED+"Nickname non valido"+RESET);
            return false;
        }
        return true;
    }

    // Metodo che restituisce false se la stringa in input contiene almeno 2 cifre, true altrimenti
    private boolean noDigit(String p){
        int count=0;
        for(int i=0;i<p.length();i++)
            if(Character.isDigit(p.charAt(i)))
                count++;

        if(count < 2)
            return true;

        return false;
    }

    // Metodo che invia al server la stringa data in input tramite TCP
    public void inviaRichiesta(String richiesta) throws IOException {
        byte[] data = richiesta.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println(BLUE+"Ho inviato: " + richiesta+RESET);
        buffer.clear();
    }

    // Metodo utilizzato durante una sfida per inviare la propria traduzione e ricevere la conseguente risposta da parte del server,
    // la quale potrebbe essere un' altra parola da tradurre o il messaggio di fine partita
    public void inviaRichiestaSfida(String richiesta) throws IOException {
        inviaRichiesta(richiesta);
        String receiveWord = riceviRisposta();
        //Ho ricevuto il messaggio finale contenente sia il mio punteggio che l' esito di pareggio/vittoria/sconfitta
        if(receiveWord.contains("Hai guadagnato")){
            String first = receiveWord.substring(0,92);
            String second = receiveWord.substring(93,receiveWord.length());
            clientUI.switchToResult();
            resultPanel.setOnlyMyScore(first);
            resultPanel.setResultString(second);
            inAMatch.set(false);
            return;
        }
        // Ho ricevuto il messaggio contenente solo il mio punteggio ma senza l' esito della partita, poichè l' avversario non ha ancora finito.
        else
            if(receiveWord.contains("Hai tradotto correttamente")){
                clientUI.switchToResult();
                resultPanel.setMyScore(receiveWord);
            }
            else {
                gamePanel.setNewWord(receiveWord);
            }
    }

    // Metodo che restituisce la stringa ricevuta sul SocketChannel dell' utente
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
            buffer.clear();
        }
        result = result.replace("_EOM", "");

        System.out.println(GREEN+"Risposta: " + result+RESET);
        return result;
    }

    // Metodo che restituisce il JSONArray ricevuto dal server contenente la classifica con i punteggi degli utenti presenti nella mia lista amici
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

        JSONParser parser = new JSONParser();
        Object obj= parser.parse(result);
        JSONArray classifica = (JSONArray) obj;
        return classifica;
    }

    // Metodo che, una volta ottenuta la mia lista amici in formato JSON, invoca "stampaListaAmici" per stamparla su terminale.
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

        JSONParser parser = new JSONParser();
        Object obj= parser.parse(result);
        JSONObject utente = (JSONObject) obj;
        stampaListaAmici(utente);
    }

    // Funzione che formatta e stampa il JSONObject corrispondente alla lista amici dell' utente.
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