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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/*
OVERVIEW: Oggetto che implementa la ricezione, elaborazione e invio dell' esito dei comandi ricevuti.
          Alla creazione, crea e registra un oggetto di tipo RegistrazioneIml sulla porta 8080,
          utilizzato dagli utenti per registrarsi tramite RMI. Sulla porta 6666 invece rimane in ascolto
          di richieste TCP per tutte le altre operazioni offerte da questo servizio.
 */

public class Server {
    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\033[0;34m";
    public static final String GREEN = "\u001B[32m";
    private final WordToTraslate dizionario;
    private  ServerSocketChannel serverSocketChannel;
    private  Selector selector;
    private  OnlineList listaUtentiOnline;
    private static String EOM = "_EOM";
    private ThreadPoolExecutor challengePoolExecutor;

    // Alla creazione, controlla se i file necessari per la memorizzazione delle informazioni sono disponibili,
    // creandoli se necessario. In seguito inizializza e rende disponibile l' oggetto condiviso utilizzato
    // tramite RMI per la registrazione e inizializza il selettore per lo scambio di messaggi TCP.
    public Server() throws IOException {
        dizionario = new WordToTraslate();
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

        challengePoolExecutor = new ThreadPoolExecutor(10,30, 40, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    // Ciclio while(true) per la ricezione ed elaborazione delle richieste.
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
                    currentKey.cancel();
                    client.close();
                }
            }
        }
    }

    // Accettazione di una nuova connessione.
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

    // Ricezione ed elaborazione di un nuovo messaggio dal client.
    private void readRequest(SelectionKey newReadRequest) throws IOException, ParseException {
        SocketChannel client = (SocketChannel) newReadRequest.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
        attachment.getByteBuffer().clear();
        int num = client.read(attachment.getByteBuffer());

        //La comunicazione è stata interrotta.
        if(num==-1)throw new IOException();

        // Condizione che risulta essere vera quando il messaggio contenuto dentro al ReadingByteBuffer
        // termina con la flag di fine messaggio "_EOM"
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

    // Elaborazione ed invio di un messaggio ad un client.
    private void writeRequest(SelectionKey currentKey) throws IOException {
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        String risposta = attachment.getMessage();
        //ByteBuffer buffer=attachment.getByteBuffer();
        ByteBuffer buffer;
        buffer = ByteBuffer.wrap(risposta.getBytes());
        while(buffer.hasRemaining())
            client.write(buffer);

        attachment.updateMessagge("");
        System.out.println("Messaggio che invia il server: "+risposta);

        if(risposta.equals("logout effettuato_EOM") || attachment.getCodiceErroreLogin() )
            currentKey.cancel();
        else
            currentKey.interestOps(SelectionKey.OP_READ);
    }

    /* Controllo di validità della coppia <nickname, password>.
        controllo_credenziali returns:
            0 => il controllo è andato a buon fine
            1 => il nickname non è corretto
            2 => la password non è corretta */
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
                if(oggetto.get("password").equals(password))
                    return 0;
                else
                    return 2;
            }
        }
        return 1;
    }

    /* Analizza ed elabora il comando ricevuto dal client.
       Restituisce una stringa contenente l' esito di tale richiesta data la sua validità ed effettivo compimento da parte del server.*/
    private String analizzaRichiesta(String richiesta, SocketChannel clientChannel, SelectionKey currentKey) throws IOException, ParseException {
        StringTokenizer tokenizer = new StringTokenizer(richiesta);
        String token = tokenizer.nextToken();

        switch (token) {
            case "login":{
                String nickname = tokenizer.nextToken();
                String password = tokenizer.nextToken();
                int codice = controllo_credenziali(nickname, password);
                if (codice == 0) {
                    boolean utenteAggiunto = listaUtentiOnline.addUser(nickname, clientChannel, currentKey);
                    if (utenteAggiunto)
                        return "OK";

                    return "Login già effettuato";
                }
                else {
                    ReadingByteBuffer readingByteBuffer = (ReadingByteBuffer) currentKey.attachment();
                    readingByteBuffer.setCodiceErroreLogin();
                    if(codice == 1)
                        return "Errore: utente non esistente";

                    return "Errore: password errata";
                }
            }
            case "logout": {
                String nickname = tokenizer.nextToken();
                if(listaUtentiOnline.removeUser(nickname))
                    return "logout effettuato";

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

                        ChallengeTask sfida = new ChallengeTask(sfidante, sfidato, dizionario.getRandomWord());
                        challengePoolExecutor.submit(sfida);


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
            case "mostra_punteggio":{
                String nickname = tokenizer.nextToken();
                int punteggio = mostraPunteggio(nickname, currentKey);
                if(punteggio == -1)
                    return "Errore: Controllare nickname inserito";

                return String.valueOf(punteggio);
            }
            case "mostra_classifica":{
                String nickname = tokenizer.nextToken();
                JSONObject jsonObject = listaAmici(nickname,currentKey);
                if(jsonObject==null)
                    return "Errore: Controllare nickname inserito";
                return classifica(jsonObject,nickname).toJSONString();
            }
            default:
                return "comando non valido";
        }
    }

    /*Data la lista amici di un utente e il suo username, cerca nel file "ClassificaPunti.json" il punteggio di ogni amico presente nella lista
      e inserisce nel JSONObject  le coppie <amico, punteggio> per poi restituirlo.    */
    private JSONArray classifica(JSONObject listaAmici, String nickname)throws FileNotFoundException,ParseException,IOException{
        JSONParser parser = new JSONParser();
        Object obj = null;

        obj = parser.parse(new FileReader("ClassificaPunti.json"));
        JSONArray listaPunti = (JSONArray) obj;
        Iterator<JSONObject> finalIterator = listaPunti.iterator();
        JSONArray listaAmiciJson = (JSONArray) listaAmici.get("lista_amici");
        Iterator<JSONObject> iteratorFriendsList = listaAmiciJson.iterator();

        JSONObject friend = null;
        String friendNickname = null;
        JSONArray finalResultArray = new JSONArray();

        for(Object currentObj :  listaPunti){
            JSONObject currentjson = (JSONObject) currentObj;
            if(currentjson.get("nickname").equals(nickname)){
                finalResultArray.add(currentjson);
                break;
            }
        }

        while (iteratorFriendsList.hasNext()){
            friend = iteratorFriendsList.next();
            iteratorFriendsList.remove();
            friendNickname = friend.get("amico").toString();
            System.out.print(friendNickname+" + ");
            finalIterator = listaPunti.iterator();
            while (finalIterator.hasNext()){
                JSONObject currentFriend = finalIterator.next();
                if(currentFriend.get("nickname").equals(friendNickname)){
                    finalResultArray.add(currentFriend);
                    break;
                }
            }
        }

        return finalResultArray;
    }

    // Cerca nel file "ClassificaPunti.json" e restituisce il mio punteggio
    private int mostraPunteggio(String nickname, SelectionKey currentKey)throws FileNotFoundException,ParseException,IOException{
        if(nickname==null) throw new NullPointerException("utente di sessione uguale a null");

        if(!checkEsistenza(nickname,currentKey))
            return -1; // Controllo della validità della corrispondenza tra il mio nickname e l'utente che ha inviato la richiesta

        JSONParser parser = new JSONParser();
        FileReader fileR = new FileReader("ClassificaPunti.json");
        Object obj;

        obj = parser.parse(fileR);
        JSONArray classifica = (JSONArray) obj;
        Iterator<JSONObject> iterator = classifica.iterator();
        JSONObject currentUser = null;

        while (iterator.hasNext() ){
            currentUser = iterator.next();
            iterator.remove();
            if(currentUser.get("nickname").equals(nickname))
                return Integer.parseInt(currentUser.get("punteggio").toString());
        }
        return -2;
    }

    /* Controllo della validità della richiesta di sfida ottenuta. setup_sfida() returns:
        0 => controllo andato a buon fine
        1 => la mia richiesta non è valida poichè il nickName dello sfidante non corrisponde al proprietario effettivo di quella chiave
        2 => l' utente sfidato non è presente nella mia lista amici
        3 => l' utente sfidato non è online */
    private int setupSfida(String myNickname, String friendNickname, SelectionKey currentKey) throws FileNotFoundException, ParseException, IOException{
        if(myNickname==null) throw new NullPointerException("Utente di sessione == null");
        if(friendNickname==null) throw new NullPointerException("Nome amico == null");
        if(currentKey==null)throw new NullPointerException("CurrentKey == null");

        boolean trovato = false;
        if(!checkEsistenza(myNickname,currentKey))
            return 1; // Controllo della validità della corrispondenza tra il mio nickname e l'utente che ha inviato la richiesta

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
            return 2;
        }

        OnlineList.OnlineUser OnlineFriend = null;
        if((OnlineFriend = listaUtentiOnline.containsUser(friendNickname)) == null)
            return 3;

        return 0;
    }


    // Restituisce il JSONObject che rappresenta la lista amici dell' utente con username "nickname", null altrimenti
    private JSONObject listaAmici(String nickname, SelectionKey currentKey) throws NullPointerException, IOException, ParseException {
        if(nickname==null) throw new NullPointerException("utente di sessione uguale a null");
        if(currentKey==null)throw new NullPointerException("currentKey uguale a null");
        if(!checkEsistenza(nickname,currentKey))
            return null; // Controllo della validità della corrispondenza tra il mio nickname e l'utente che ha inviato la richiesta

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

    /* Metodo che aggiunge l' utente friendNickname nella lista amici dell' utente "myNickname" e viceversa se vengono superati i controlli di validità
       della richiesta.  aggiungiAmico() returns:
         0 => operazione avvenuta con successo
         1 => la mia richiesta non è valida poichè il nickName dello sfidante non corrisponde al proprietario effettivo di quella chiave
         2 => l'amicizia è già presente
         3 => l'utente da aggiungere non esiste */
    private int aggiungiAmico(String myNickname, String friendNickname, SelectionKey currentKey) throws NullPointerException, IOException {
        if(myNickname==null) throw new NullPointerException("utente di sessione uguale a null");
        if(friendNickname==null) throw new NullPointerException("nome amico uguale a null");
        if(!checkEsistenza(myNickname,currentKey))
            return 1;  // Controllo della validità della corrispondenza tra il mio nickname e l'utente che ha inviato la richiesta
        JSONParser parser = new JSONParser();
        Object obj=null;
        try {
            obj = parser.parse(new FileReader("ListaAmici.json"));
        }catch (Exception e){
            e.printStackTrace();
        }

        JSONArray jsonArray = (JSONArray)obj;
        int controllo = addIfFriendExist(jsonArray,friendNickname, myNickname);
        if(controllo != 0)
            return controllo;

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
        return 0;
    }

    /* Controllo che restituisce:
        true => l' associazione tra il nickname e il socketChannel associato a quel nickname corrispondono
        false => altrimenti */
    private boolean checkEsistenza(String nickname, SelectionKey currentKey){
        if(!listaUtentiOnline.checkAssociazione(nickname, (SocketChannel)currentKey.channel())) return false;
        return  true;

    }

    /* Metodo che aggiunge alle corrispettive liste amici degli utenti chiamati "myNickname" e "friendNickname" il nuovo amico.
        addIfFriendExists() returns:
            0 => operazione avvenuta con successo
            2 => l' amico che si vuole aggiungere appartiene già alla mia lista amici
            3 => l' amico che si vuole aggiungere non esiste */
    private int addIfFriendExist(JSONArray jsonArray, String friendNickname, String myNickname){
        Iterator<JSONObject>  iterator= jsonArray.iterator();
        while(iterator.hasNext()) {
            JSONObject oggetto = iterator.next();
            if (oggetto.get("nickname").equals(friendNickname)){
                JSONArray listaAmici = (JSONArray) oggetto.get("lista_amici");
                if(esistenzaAmicizia(listaAmici, myNickname)) return 2;
                JSONObject nuovoAmico = new JSONObject();
                nuovoAmico.put("amico", myNickname);
                listaAmici.add(nuovoAmico);
                return 0;
            }
        }
        return 3;
    }

    /* Controllo integrità dei file necessari per la memorizzazione dati degli utenti registrati al servizio
     in caso in cui anche un solo file tra "Credenziali.json", "ClassificaPunti.json" o "ListaAmici.json" non fossero presenti,
     vengono ricreati tutti e 3 per evitare inconsistenze nei dati memorizzati */
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
        System.out.println("File necessari presenti");
    }

    /* esistenzaAmicizia() returns:
            true  => l' utente con nome "myNickname" è già presente nella mia lista amici
            false => altrimenti */
    private boolean esistenzaAmicizia(JSONArray listaAmicizia, String myNickname){
        Iterator<JSONObject> iterator = listaAmicizia.iterator();
        while(iterator.hasNext()){
            JSONObject oggetto = iterator.next();
            if(oggetto.get("amico").equals(myNickname)) return true;
        }
        return false;
    }

    // Metodo per cancellare tutti i file .json necessari per memorizzare i dati degli utenti registrati al servizio
    private void cancellaFile()throws IOException{
        File dataCheck = new File("Credenziali.json");
        if(dataCheck.exists()) dataCheck.delete();

        dataCheck = new File("ClassificaPunti.json");
        if(dataCheck.exists()) dataCheck.delete();

        dataCheck = new File("ListaAmici.json");
        if(dataCheck.exists()) dataCheck.delete();

        System.out.println("Ho cancellato tutti i file!");
    }

    // Metodo per creare i file .json necessari per memorizzare i dati degli utenti registrati al servizio
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