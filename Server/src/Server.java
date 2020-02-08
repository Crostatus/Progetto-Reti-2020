import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

public class Server {
    /*public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\033[0;34m";
    public static final String GREEN = "\u001B[32m";*/
    private final WordToTraslate dizionario;
    private  ServerSocketChannel serverSocketChannel;
    private  Selector selector;
    private  OnlineList listaUtentiOnline;
    private static int CHUNKSIZE = 64;
    private static String EOM = "_EOM";

    public Server() throws IOException {
        dizionario = new WordToTraslate();
        //System.out.println(dizionario.getRandomWord());
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

    // ciclo while del server
    public void startServer() throws IOException, ParseException {
        while(true){
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while(iterator.hasNext()){
                SelectionKey currentKey = iterator.next();
                iterator.remove();
                try {
                    if (!currentKey.isValid())
                        continue;
                    if (currentKey.isAcceptable()) {
                        accept();
                        System.out.println("Connessione accettata!");
                    }
                    if (currentKey.isReadable()) {
                        System.out.println("------READ REQUEST------");
                        readRequest(currentKey);
                    }
                    if (currentKey.isWritable()) {
                        System.out.println("-------WRITE REQUEST------");
                        writeRequest(currentKey);
                    }
                }
                catch (IOException  e){
                    System.out.println("------TERMINE CONNESSIONE------");
                    SocketChannel client = (SocketChannel) currentKey.channel();
                    listaUtentiOnline.removeUser(client);
                    //listaUtentiOnline.printList();
                    currentKey.cancel();
                    client.close();
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
        catch(ClosedChannelException e ) {
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private void readRequest(SelectionKey newReadRequest) throws IOException, ParseException {
        SocketChannel client = (SocketChannel) newReadRequest.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
        attachment.getByteBuffer().clear();
        int num = client.read(attachment.getByteBuffer());

        //vedo quando il client termina improvvisamente
        if(num==-1)throw new IOException();

        // Entro dentro quando ho finito di leggere
        if(attachment.updateOnRead()) {
            String richiesta = attachment.getMessage();
            System.out.println("Messaggio arrivato al server: " + richiesta);
            String risposta = analizzaRichiesta(richiesta,client,newReadRequest)+EOM;


            if(risposta.contains("Questa è una sfida")){
                newReadRequest.interestOps(0);
                attachment.updateMessagge("");
                attachment.clear();
                return;
            }
            newReadRequest.interestOps(SelectionKey.OP_WRITE);
            attachment.updateMessagge(risposta);
            attachment.clear();
        }
    }

    private void writeRequest(SelectionKey currentKey) throws IOException {
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        String risposta=attachment.getMessage();
        ByteBuffer buffer=attachment.getByteBuffer();

        buffer=ByteBuffer.wrap(risposta.getBytes());
        while(buffer.hasRemaining())
            client.write(buffer);
        attachment.updateMessagge("");

        System.out.println("Messaggio che invia il server: "+risposta);
        if(risposta.equals("logout effettuato_EOM") || attachment.getCodiceErroreLogin() )
            currentKey.cancel();
        else
            currentKey.interestOps(SelectionKey.OP_READ);
    }

    // restituisce 0 se il controllo nickname e password è andato a buon fine,
    // 2 se la password è sbagliata e 1 se il nickname non è corretto
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
    private String analizzaRichiesta(String richiesta, SocketChannel clientChannel, SelectionKey currentKey) throws IOException, ParseException {
        StringTokenizer tokenizer = new StringTokenizer(richiesta);
        String token = tokenizer.nextToken();
        //System.out.println("Token arrivato al server: " + token);
        switch (token) {
            case "login":{
                String nickname = tokenizer.nextToken();
                String password = tokenizer.nextToken();
                // controllare meglio il passaggio di informazione al livello superiore
                //int udpPort = Integer.parseInt(tokenizer.nextToken());
                int codice = controllo_credenziali(nickname, password);
                //System.out.println("Codice richiesta di login: " + codice);
                if (codice == 0) {
                    boolean utenteAggiunto = listaUtentiOnline.addUser(nickname, clientChannel, currentKey);
                    //System.out.println("utente inserito: " + utenteAggiunto);
                    //listaUtentiOnline.printList();
                    if (utenteAggiunto)
                        return "OK";
                    return "Login già effettuato";

                } else {
                    ReadingByteBuffer readingByteBuffer = (ReadingByteBuffer) currentKey.attachment();
                    readingByteBuffer.setCodiceErroreLogin();
                    // System.out.println("codice errore: " + codice);
                    if(codice == 1)
                        return "Errore: utente non esistente";


                    return "Errore: password errata"; //Rifiuta richiesta di login e rispondi male.
                }
            }
            case "logout": {
                String nickname = tokenizer.nextToken();
                if(listaUtentiOnline.removeUser(nickname)) {
                    //listaUtentiOnline.printList();
                    return "logout effettuato";
                }
                return "logout non effettuato";
            }
            case "aggiungi_amico":{
                String myNickname = tokenizer.nextToken();
                String friendNickname = tokenizer.nextToken();
                int codice = aggiungiAmico(myNickname,friendNickname,currentKey);
                switch (codice){
                    case 0:
                        return "Utente aggiunto correttamente";
                    case 1:
                        return "Errore: nome utente di sessione sbagliato";
                    case 2:
                        return "Errore: amicizia gia esistente";
                    case 3:
                        return "Errore: amico non esistente";
                }
            }
            case "lista_amici": {
                String nickname = tokenizer.nextToken();
                JSONObject jsonObject = listaAmici(nickname,currentKey);
                if(jsonObject==null)
                    return "Errore: Controllare nickname inserito";
                else
                    return jsonObject.toJSONString();
            }
            case "sfida": {
                String myNickname = tokenizer.nextToken();
                String friendNickname = tokenizer.nextToken();
                int codice = setupSfida(myNickname, friendNickname, currentKey);
                switch (codice){
                    case 0: {

                        OnlineList.OnlineUser sfidante = listaUtentiOnline.getPlayer(myNickname);
                        OnlineList.OnlineUser sfidato = listaUtentiOnline.getPlayer(friendNickname);

                        ChallengeThread sfida = new ChallengeThread(sfidante, sfidato, dizionario.getRandomWord());
                        sfida.start();

                        //return myNickname + " :Richiesta di sfida correttamente inviata";
                        return "Questa è una sfida";
                    }
                    case 1:
                        return "Errore: nome utente di sessione sbagliato";
                    case 2:
                        return "Errore: amicizia non esistente";
                    case 3:
                        return "Errore: amico non online";
                }

            }
            default:
                return "comando non valido";
        }
    }

    private int setupSfida(String myNickname, String friendNickname, SelectionKey currentKey) throws FileNotFoundException, ParseException, IOException{
        if(myNickname==null) throw new NullPointerException("utente di sessione uguale a null");
        if(friendNickname==null) throw new NullPointerException("nome amico uguale a null");
        if(currentKey==null)throw new NullPointerException("currentKey uguale a null");
        boolean trovato = false;

        if(!checkEsistenza(myNickname,currentKey))
            return 1; // errore nome utente di sessione

        JSONParser parser = new JSONParser();
        FileReader fileR = new FileReader("ListaAmici.json");
        Object obj = null;

        obj = parser.parse(fileR);

        JSONArray ListaAmicizie = (JSONArray) obj;
        Iterator<JSONObject> iterator = ListaAmicizie.iterator();
        JSONObject currentUser = null;
        while (iterator.hasNext() && !trovato){
            currentUser = iterator.next();
            if(currentUser.get("nickname").equals(myNickname)){
                trovato = true;
            }
        }
        if(!esistenzaAmicizia((JSONArray) currentUser.get("lista_amici"), friendNickname)){
            return 2; // errore l' user non è mio amico
        }

        OnlineList.OnlineUser OnlineFriend = null;
        if((OnlineFriend = listaUtentiOnline.containsUser(friendNickname)) == null)
            return 3; // errore amico non online

        //InetAddress friendAddress = OnlineFriend.getUserChannel().socket().getInetAddress();
        return 0;
        /** Creare un Thread dedicato alla sfida e mandare sulla
         * porta UDP del client(amico) la richiesta di sfida,
         * il quale rispondera al server se è stata accettata
         * oppure no. In caso affermativo comunicheranno su
         * quel thread per il resto della sfida */

    }

    // restituisce il JsonObject che rappresenta nickname e la sua lista amici
    // se tutti i controlli sono superati con successo, null altrimenti
    private JSONObject listaAmici(String nickname, SelectionKey currentKey) throws NullPointerException, IOException, ParseException {
        if(nickname==null) throw new NullPointerException("utente di sessione uguale a null");
        if(currentKey==null)throw new NullPointerException("currentKey uguale a null");
        if(!checkEsistenza(nickname,currentKey))
            return null; // errore nome utente di sessione

        JSONParser parser = new JSONParser();
        Object obj=null;
        obj = parser.parse(new FileReader("ListaAmici.json"));

        JSONArray listaAmicizie= (JSONArray) obj;
        Iterator<JSONObject> iterator = listaAmicizie.iterator();
        while (iterator.hasNext()){
            JSONObject listaAmici =(JSONObject) iterator.next();
            if(listaAmici.get("nickname").equals(nickname)){
                return listaAmici;
            }
        }
        return null;
    }

    // aggiunge myNickname alla lista amici di friendNickname e aggiunge friendNickname
    // alla lista amici di myNickname restituendo 0, altrimenti un codice di errore
    // univoco per ogni tipo di errore
    private int aggiungiAmico(String myNickname, String friendNickname, SelectionKey currentKey) throws NullPointerException, IOException {
        if(myNickname==null) throw new NullPointerException("utente di sessione uguale a null");
        if(friendNickname==null) throw new NullPointerException("nome amico uguale a null");
        if(!checkEsistenza(myNickname,currentKey))
            return 1; // errore nome utente di sessione
        JSONParser parser = new JSONParser();
        Object obj=null;

        try {
            obj = parser.parse(new FileReader("ListaAmici.json"));
        }catch (Exception e){
            e.printStackTrace();
        }
        JSONArray jsonArray = (JSONArray)obj;
        int controllo = addIfFriendExist(jsonArray,friendNickname, myNickname);

        if(controllo != 0) return controllo;

        Iterator<JSONObject>  iterator= jsonArray.iterator();
        boolean aggiunto = false;
        while(iterator.hasNext() && !aggiunto) {
            JSONObject oggetto = iterator.next();
            if (oggetto.get("nickname").equals(myNickname)){
                JSONObject nuovoAmico = new JSONObject();
                nuovoAmico.put("amico", friendNickname);
                JSONArray listaAmici = (JSONArray) oggetto.get("lista_amici");
                listaAmici.add(nuovoAmico);
                File file= new File("ListaAmici.json");
                FileWriter fileWriter= new FileWriter(file);
                fileWriter.write(jsonArray.toJSONString());
                fileWriter.flush();
                fileWriter.close();
                aggiunto = true;
            }
        }
        return 0;// amicizia creata con successo
    }

    // controlla se l'associazione tra il nickname e il socketchannel associato a quel nickname corrispondono
    private boolean checkEsistenza(String nickname, SelectionKey currentKey){
        if(!listaUtentiOnline.checkAssociazione(nickname, (SocketChannel)currentKey.channel())) return false;
        return  true;

    }

    // aggiunge alla lista amici di friendNickname myNickname restituendo 0 se
    // tutti i controlli sono passati, altrimenti un codice di errore
    // univoco per ogni tipo di errore
    private int addIfFriendExist(JSONArray jsonArray, String friendNickname, String myNickname){
        Iterator<JSONObject>  iterator= jsonArray.iterator();
        while(iterator.hasNext()) {
            JSONObject oggetto = iterator.next();
            if (oggetto.get("nickname").equals(friendNickname)){
                JSONArray listaAmici = (JSONArray) oggetto.get("lista_amici");
                if(esistenzaAmicizia(listaAmici, myNickname)) return 2; // amicizia gia esistente
                JSONObject nuovoAmico = new JSONObject();
                nuovoAmico.put("amico", myNickname);
                listaAmici.add(nuovoAmico);
                return 0; //amicizia aggiunta con successo nella lista di amici del mio amico
            }
        }
        return 3; // amico non esistente
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

    // restituiva true se l'amicizia esiste già, false altrimenti
    private boolean esistenzaAmicizia(JSONArray listaAmicizia, String myNickname){
        Iterator<JSONObject> iterator = listaAmicizia.iterator();
        while(iterator.hasNext()){
            JSONObject oggetto = iterator.next();
            if(oggetto.get("amico").equals(myNickname)) return true;
        }
        return false;

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
        JSONArray jsonArray = new JSONArray();
        File dataCheck = new File("Credenziali.json");
        dataCheck.createNewFile();
        FileWriter fileWriter= new FileWriter(dataCheck);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();

        dataCheck = new File("ClassificaPunti.json");
        dataCheck.createNewFile();
        fileWriter= new FileWriter(dataCheck);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();

        dataCheck = new File("ListaAmici.json");
        dataCheck.createNewFile();
        fileWriter= new FileWriter(dataCheck);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();

        System.out.println("Ho creato i file!");
    }
}