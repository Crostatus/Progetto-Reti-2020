import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainServer {
    private static ServerSocketChannel serverSocketChannel;
    private static Selector selector;
    private static OnlineList listaUtentiOnline;


    public static void main(String[] args) throws Exception{
        checkFile();

        RegistrazioneInterface registra = new RegistrazioneImpl();
        LocateRegistry.createRegistry(8080);
        Registry reg = LocateRegistry.getRegistry(8080);
        reg.rebind("iscrizione", registra );

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(6666));
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, null);
        listaUtentiOnline = new OnlineList();

        while(true){
            try{
                selector.select();
            }
            catch(IOException e){
                //caso davvero brutto, fare qualcosa di emergenza!
                e.printStackTrace();
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while(iterator.hasNext()){
                SelectionKey currentKey = iterator.next();
                iterator.remove();

                if(!currentKey.isValid())
                    continue;

                if(currentKey.isAcceptable()){
                    accept();
                    System.out.println("Connessione accettata!");
                }
                if(currentKey.isReadable()){
                    readRequest(currentKey);
                }
                if(currentKey.isWritable()){
                    writeRequest(currentKey);
                }
            }




        }


    }

    // controllo se i file esistono già, in caso ne mancasse anche solo uno
    // si cancellano tutti gli altri e si ricreano
    public static void checkFile() throws Exception{
        File dataCheck = new File("Credenziali.json");
        if(!dataCheck.exists()){
            cancellaFile();
            generaFile();
            return;
        }

        dataCheck = new File("ClassificaPunti.json");
        if(!dataCheck.exists()){
            cancellaFile();
            generaFile();
            return;
        }

        dataCheck = new File("ListaAmici.json");
        if(!dataCheck.exists()){
            cancellaFile();
            generaFile();
            return;
        }

        System.out.println("I file ci sono già!");
    }

    // funzione per cancellare tutti i file
    private static void cancellaFile()throws Exception{
        File dataCheck = new File("Credenziali.json");
        if(dataCheck.exists()) dataCheck.delete();

        dataCheck = new File("ClassificaPunti.json");
        if(dataCheck.exists()) dataCheck.delete();

        dataCheck = new File("ListaAmici.json");
        if(dataCheck.exists()) dataCheck.delete();

        System.out.println("Ho cancellato tutti i file!");
    }

    // funzione che genera tutti i file
    private static void generaFile() throws Exception{
        File dataCheck = new File("Credenziali.json");
        dataCheck.createNewFile();

        dataCheck = new File("ClassificaPunti.json");
        dataCheck.createNewFile();

        dataCheck = new File("ListaAmici.json");
        dataCheck.createNewFile();

        System.out.println("Ho creato i file!");
    }

    private static void accept(){
        try {
            SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, new ReadingByteBuffer());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*private static void readRequest(SelectionKey newReadRequest) throws IOException{
        System.out.println("Read request!");
        SocketChannel client = (SocketChannel) newReadRequest.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
        attachment.getByteBuffer().clear();
        client.read(attachment.getByteBuffer());

        if(attachment.updateOnRead()) {
            //newReadRequest.interestOps(SelectionKey.OP_WRITE);
            SocketAddress clientAddress = client.getRemoteAddress();
            String richiesta = attachment.getMessage();
            System.out.println("Messaggio arrivato al server: " + richiesta);
            StringTokenizer tokenizer = new StringTokenizer(richiesta);
            String token = tokenizer.nextToken();
            System.out.println("Token arrivato al server: " + token);
            if(token.equals("login")) {
                String nickname = tokenizer.nextToken();
                String password = tokenizer.nextToken();
                int codice = controllo_credenziali(nickname, password);
                if (codice == 0) {
                    //Aggiungi alla lista persone online
                    System.out.println("PUPPASEDANIIIIIIII");
                } else {
                    System.out.println(codice);
                }
            }
        }
        //controllo di sicurezza

        // converto data, cerco su json, controllo pw e user
        //salvare informazioni del client che è online
    }
*/
    private static void readRequest(SelectionKey newReadRequest) throws IOException{
        SocketChannel client = (SocketChannel) newReadRequest.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
        attachment.getByteBuffer().clear();
        int num = client.read(attachment.getByteBuffer());

        if(num == -1) {
            newReadRequest.interestOps(SelectionKey.OP_WRITE);
            SocketAddress clientAddress = client.getRemoteAddress();
            String richiesta = attachment.getMessage();
            System.out.println("Messaggio arrivato al server: " + richiesta);
            StringTokenizer tokenizer = new StringTokenizer(richiesta);
            String token = tokenizer.nextToken();
            System.out.println("Token arrivato al server: " + token);
            if(token.equals("login")) {
                String nickname = tokenizer.nextToken();
                String password = tokenizer.nextToken();
                int codice = controllo_credenziali(nickname, password);
                System.out.println("Codice richiesta di login: " + codice);
                if (codice == 0) {
                    boolean utenteAggiunto = listaUtentiOnline.addUser(nickname, clientAddress);
                    System.out.println("utente inserito: "+utenteAggiunto);
                    listaUtentiOnline.printList();
                    if(utenteAggiunto)
                        attachment.updateMessagge("OK");
                    else
                        attachment.updateMessagge("Login già effettuato");

                } else {
                    System.out.println("codice errore: "+codice);
                    attachment.updateMessagge("Codice errore: "+codice); //Rifiuta richiesta di login e rispondi male.
                }
            }
            return;
        }
        attachment.updateOnRead();


        //controllo di sicurezza

        // converto data, cerco su json, controllo pw e user
        //salvare informazioni del client che è online
    }

    private static int controllo_credenziali(String nickname, String password){
        JSONParser parser = new JSONParser();
        Object obj=null;

        try {
            obj = parser.parse(new FileReader("Credenziali.json"));
        }catch (Exception e){
            e.printStackTrace();
        }
        JSONArray jsonArray = (JSONArray)obj;
        Iterator<JSONObject>  iterator= jsonArray.iterator();

        while(iterator.hasNext()) {
            JSONObject oggetto = iterator.next();
            if(oggetto.get("nickname").equals(nickname)){
                if(oggetto.get("password").equals(password)) return 0;
                else return 2; //2 significa password errata
            }
        }return 1; // 1 significa utente non esistente
    }


    private static void writeRequest(SelectionKey currentKey){
        System.out.println("Write request!");
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        ByteBuffer msgBuf = attachment.getByteBuffer();
        try {
            client.write(msgBuf);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        if(!msgBuf.hasRemaining()){
            currentKey.interestOps(SelectionKey.OP_READ);
        }
    }

}





