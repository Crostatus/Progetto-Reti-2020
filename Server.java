import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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

public class Server {
    private  ServerSocketChannel serverSocketChannel;
    private  Selector selector;
    private  OnlineList listaUtentiOnline;

    public Server() throws IOException {
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
    }

    public void startServer()throws IOException{
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
                    System.out.println("Read request!");
                    readRequest(currentKey);
                }
                if(currentKey.isWritable()){
                    System.out.println("Write request!");
                    writeRequest(currentKey);
                }
            }
        }
    }

    private void accept(){
        try {
            SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, new ReadingByteBuffer());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void readRequest(SelectionKey newReadRequest) throws IOException{
        SocketChannel client = (SocketChannel) newReadRequest.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
        attachment.getByteBuffer().clear();
        int num = client.read(attachment.getByteBuffer());
        System.out.println("Numero bytes letti "+num);

        if(num==0)
            System.out.println("Sto leggendo 0 bytes");
        // Entro dentro quando ho finito di leggere
        if(num == -1) {
            System.out.println("SSSSSSSSS");
            newReadRequest.interestOps(SelectionKey.OP_WRITE);
            SocketAddress clientAddress = client.getRemoteAddress();
            String richiesta = attachment.getMessage();
            System.out.println("Messaggio arrivato al server: " + richiesta);
            String risposta = analizzaRichiesta(richiesta,clientAddress);

            byte[] data = risposta.getBytes();
            attachment.getByteBuffer().put(data);

            return;
        }
        attachment.updateOnRead();


        //controllo di sicurezza

        // converto data, cerco su json, controllo pw e user
        //salvare informazioni del client che è online
    }

    private int controllo_credenziali(String nickname, String password){
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

    // analizza il messaggio di richiesta del client e restituisce
    // come stringa la risposta che il server deve mandare al client
    private String analizzaRichiesta(String richiesta, SocketAddress clientAddress){
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
                    return "OK";
                else
                    return "Login già effettuato";

            } else {
                System.out.println("codice errore: "+codice);
                return "Codice errore: "+codice; //Rifiuta richiesta di login e rispondi male.
            }
        }
        return "comando non valido";
    }

    private void writeRequest(SelectionKey currentKey)throws IOException{
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        ByteBuffer msgBuf = attachment.getByteBuffer();
        client.write(msgBuf);

        if(!msgBuf.hasRemaining()){
            currentKey.interestOps(SelectionKey.OP_READ);
        }
    }

    // controllo se i file esistono già, in caso ne mancasse anche solo uno
    // si cancellano tutti gli altri e si ricreano
    public void checkFile() throws IOException {
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
    private void cancellaFile()throws IOException{
        File dataCheck = new File("Credenziali.json");
        if(dataCheck.exists()) dataCheck.delete();

        dataCheck = new File("ClassificaPunti.json");
        if(dataCheck.exists()) dataCheck.delete();

        dataCheck = new File("ListaAmici.json");
        if(dataCheck.exists()) dataCheck.delete();

        System.out.println("Ho cancellato tutti i file!");
    }

    // funzione che genera tutti i file
    private void generaFile() throws IOException{
        File dataCheck = new File("Credenziali.json");
        dataCheck.createNewFile();

        dataCheck = new File("ClassificaPunti.json");
        dataCheck.createNewFile();

        dataCheck = new File("ListaAmici.json");
        dataCheck.createNewFile();

        System.out.println("Ho creato i file!");
    }
}
