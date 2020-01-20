import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
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
    private int UPD_port;
    private boolean login;
    private AtomicBoolean inAMatch;
    private UDP_Listener challengeListener;

    public Client(int registerPort, int tcpPort, int UDP_port) throws SocketException{
        this.registerPort=registerPort;
        this.tcpPort = tcpPort;
        login = false;
        inAMatch = new AtomicBoolean(false);
        challengeListener = new UDP_Listener(UDP_port, inAMatch);
        this.UPD_port = UDP_port;
    }

    // operazione per registrare l'utente
    public boolean registra(String nickname, String password) throws NotBoundException,IOException {
        if(!login) {
            if(controllo_credenziali(nickname,password)) {
                Registry reg = LocateRegistry.getRegistry(registerPort);
                this.nickname = nickname;
                RegistrazioneInterface registra = (RegistrazioneInterface) reg.lookup("iscrizione");
                int risultato = registra.registra_utente(nickname, password);
                if (risultato == 0) {
                    System.out.println("Operazione avvenuta con successo");
                    return true;
                } else if (risultato == 1)
                    System.out.println(RED+"Nickname già presente"+RESET);
                return false;
            }
            return false;
        }
        System.out.println(RED+"Registrazione non permessa, login gia effettuato"+RESET);
        return false;
    }

    // richiesta di login di nickname
    public boolean login(String nickname, String password) throws IOException {
        if(login){
            System.out.println(RED+"Login già effettuato"+RESET);
            return false;
        }
        if(controllo_credenziali(nickname,password)) {
            this.nickname = nickname;
            address = new InetSocketAddress("localhost", tcpPort);
            client = SocketChannel.open(address);
            buffer = ByteBuffer.allocate(CHUNKSIZE);
            String messaggio = "login " + nickname + " " + password + " " + UPD_port + EOM;
            inviaRichiesta(messaggio);
            String risposta = riceviRisposta();
            if (risposta.equals("OK")) {
                login = true;
                return true;
            }
            return false;
        }
        return false;
    }

    // richiesta di logout
    public void logout()throws IOException{
        if(login) {
            String richiesta = "logout " + nickname + EOM;
            inviaRichiesta(richiesta);
            riceviRisposta();
            login = false;

            // la close la devo fare solo quando effettuo l'operazione di logout
            client.close();
        }
        else System.out.println(RED+"Login non effettuato"+RESET);
    }

    // richiesta di stampare la lista amici di nickname
    public void lista_amici(String nickname) throws IOException, ParseException {
        if(login){
            if(controllo_credenziali(nickname)) {
                String richiesta = "lista_amici " + nickname + EOM;
                inviaRichiesta(richiesta);
                riceviRispostaJson();
            }
        }
        else System.out.println(RED+"Login non effettuato"+RESET);
    }

    // richiesta di aggiungere friendNickname alla lista amici di myNickname e viceversa
    public void aggiungi_amico(String myNickname, String friendNickname) throws IOException {
        if(login) {
            if(myNickname.equals(friendNickname)) {
                System.out.println(RED+"Non puoi aggiungere te stesso"+RESET);
                return;
            }
            String richiesta = "aggiungi_amico " + myNickname + " " + friendNickname + EOM;
            inviaRichiesta(richiesta);
            riceviRisposta();
        }
        else System.out.println(RED+"Login non effettuato"+RESET);
    }

    public void sfida(String myNickname, String friendNickname)throws IOException{
        if(login){
            if(myNickname.equals(friendNickname)) {
                System.out.println(RED+"Non puoi inviare una richiesta di sfida a te stesso"+RESET);
                return;
            }
            String richiesta = "sfida "+myNickname+" "+friendNickname+EOM;
            inviaRichiesta(richiesta);
            riceviRisposta();

        }
        else System.out.println(RED+"Login non effettuato"+RESET);
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
    private void inviaRichiesta(String richiesta) throws IOException {
        byte[] data = richiesta.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println(BLUE+"Ho inviato: " + richiesta+RESET);
        buffer.clear();
    }

    // riceve la risposta dal server
    private String riceviRisposta() throws IOException {
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

    // riceve la risposta dal server in formato Json
    private void riceviRispostaJson() throws IOException, ParseException {
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
}
