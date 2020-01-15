import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;

public class Client {
    private  String EOM="_EOM";
    private  int CHUNKSIZE = 128;
    private InetSocketAddress address;
    private SocketChannel client;
    private ByteBuffer buffer;
    private String nickname;
    private int registerPort;
    private int tcpPort;
    private boolean login;

    public Client(int registerPort, int tcpPort){
        this.registerPort=registerPort;
        this.tcpPort = tcpPort;
        login = false;

    }

    // operazione per registrare l'utente
    public boolean registra(String nickname, String password) throws NotBoundException,IOException {
        Registry reg = LocateRegistry.getRegistry(registerPort);
        this.nickname = nickname;
        RegistrazioneInterface registra = (RegistrazioneInterface) reg.lookup("iscrizione");
        int risultato=registra.registra_utente(nickname, password);
        if(risultato==0) {
            System.out.println("Operazione avvenuta con successo");
            return true;
        }
        else if(risultato==1)
            System.out.println("Nickname già presente");
        else
            System.out.println("Password non valida, errore: "+risultato);
        return false;

    }

    // richiesta di login di nickname
    public boolean login(String nickname, String password) throws IOException {
        address = new InetSocketAddress("localhost",tcpPort);
        client = SocketChannel.open(address);
        buffer = ByteBuffer.allocate(CHUNKSIZE);
        String messaggio = "login " + nickname + " " + password + EOM;
        inviaRichiesta(messaggio);
        riceviRisposta();
        login = true;
        return true;
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
        else System.out.println("Login non effettuato");
    }

    // richiesta di stampare la lista amici di nickname
    public void lista_amici(String nickname) throws IOException, ParseException {
        if(login){
            String richiesta = "lista_amici "+ nickname+EOM;
            inviaRichiesta(richiesta);
            riceviRispostaJson();
        }
        else System.out.println("Login non effettuato");
    }

    // richiesta di aggiungere friendNickname alla lista amici di myNickname e viceversa
    public void aggiungi_amico(String myNickname, String friendNickname) throws IOException {
        if(login) {
            String richiesta = "aggiungi_amico " + myNickname + " " + friendNickname + EOM;
            inviaRichiesta(richiesta);
            riceviRisposta();
        }
        else System.out.println("Login non effettuato");
    }

    // invia la richiesta al server
    private void inviaRichiesta(String richiesta) throws IOException {
        byte[] data = richiesta.getBytes();
        buffer=ByteBuffer.wrap(data);
        while(buffer.hasRemaining())
            client.write(buffer);
        System.out.println("Ho inviato: " + richiesta);
        buffer.clear();
    }

    // riceve la risposta dal server
    private void riceviRisposta() throws IOException {
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

        System.out.println("Risposta: " + result);
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
        System.out.println("Lista amici "+nickname+": ");
        while (iterator.hasNext()){
            JSONObject amico = iterator.next();
            System.out.print(amico.get("amico")+" ");
        }
        System.out.println();
    }
}
