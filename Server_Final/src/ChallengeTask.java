import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.*;
/*
OVERVIEW: Un task di tipo ChallengeThread viene creato al momento della ricezione di una richiesta di sfida valida da parte dello sfidante.
          E' incaricato di inviare la sfida tramite UDP allo sfidato e, in caso di risposta affermativa, richiedere tramite HTTP la traduzione in inglese di 3
          parole italiane al servizio MyMemoryTranslated, per poi inviarle sequenzialmente ai client che stanno giocando  fino allo scadere
          del timeout o quando gli utenti hanno terminato la sfida. Quando la partita è terminata per entrambi gli utenti, invia l' esito
          finale e termina.
 */

public class ChallengeTask implements Runnable{
    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\033[0;34m";
    public static final String GREEN = "\u001B[32m";

    public static final int parolePerGioco =3;
    private ArrayList<String> traduzioneSfidante;
    private ArrayList<String> traduzioneSfidato;
    private ArrayList<String> paroleTradotte;
    private int indiceSfidante;
    private int indiceSfidato;
    private DatagramSocket DSocket;
    private OnlineList.OnlineUser sfidante;
    private OnlineList.OnlineUser sfidato;
    private Selector selector;
    private  String EOM="_EOM";
    private ArrayList<String> randomWords;
    private boolean finishSfidante;
    private boolean finishSfidato;
    private boolean MandatoRisultatoSfidante;
    private boolean MandatoRisultatoSfidato;
    private int punteggioSfidante;
    private int punteggioSfidato;

    //Inizializzazione
    public ChallengeTask(OnlineList.OnlineUser sfidante, OnlineList.OnlineUser sfidato, ArrayList<String> randomWords)throws SocketException {
        this.sfidato = sfidato;
        this.indiceSfidante = 0;
        this.indiceSfidato = 0;
        this.traduzioneSfidante  = new ArrayList<String>(parolePerGioco);
        this.traduzioneSfidato = new ArrayList<String>(parolePerGioco);
        this.randomWords = randomWords;
        this.sfidante = sfidante;
        int port = sfidante.getUserChannel().socket().getLocalPort();
        DSocket = new DatagramSocket(port);
        DSocket.setSoTimeout(10000);
    }

    public void run(){
        String richiesta = sfidante.getNickname()+" ti vuole sfidare! Vuoi accettare, si o no?";
        System.out.println("Thread Sfida invia: "+richiesta);
        byte[] dati= richiesta.getBytes();

        int porta = sfidato.getUserChannel().socket().getPort();
        DatagramPacket packet = new DatagramPacket(dati, dati.length, sfidato.getUserChannel().socket().getInetAddress(), porta);
        String risposta="";
        //Invio della sfida tramite UDP al utente sfidato con tempo massimo di attesa risposta di 10 secondi.
        try{
            DSocket.send(packet);
            DSocket.receive(packet);
        }
        catch (SocketTimeoutException e){
            //Lo sfidato non ha risposto alla sfida entro 10 secondi, dunque deve essere avvisato lo sfidante dell' esito negativo e terminare questo task.
            System.out.println("Timeout di ricezione risposta UDP");
            DSocket.close();
            try {
                responseAndTerminate();
            }catch (IOException z){
                z.printStackTrace();
            }
            return;
        }
        catch (IOException e){
            e.printStackTrace();
        }
        //Ho ricevuto un datagramma UDP da parte dell' utente sfidato
        dati = packet.getData();
        risposta = new String(dati, 0, packet.getLength());
        System.out.println("Pacchetto ricevuto: "+risposta);
        DSocket.close();

        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!risposta.equals("si")) {
            //Lo sfidato non ha accettato la richiesta di sfida, il task deve avvisare l' utente sfidante e terminare
            try {
                responseAndTerminate();
            } catch (IOException e) {

                e.printStackTrace();
            }
            return;
        }

        //La partita può cominciare
        risposta = "Via alla sfida di traduzione:\n"+randomWords.get(0)+EOM;
        SocketChannel channelSfidato = sfidato.getUserChannel();
        SelectionKey keySfidato = sfidato.getServerKey();
        keySfidato.interestOps(0);
        try {
            try {
                //Richiesta HTTP al servizio MyMemoryTranslated della traduzione delle parole italiane scelte a caso dal file Dizionario.txt
                traduci();
            }
            catch (IOException e){
                //Il servizio di traduzione non è disponibile dunque la partita non può avere inizio, il task deve avvisare gli utenti
                //dell' insuccesso e terminare
                String error = "Il servizio di traduzione non è disponibile" + EOM;
                SocketChannel channelSfidante = sfidante.getUserChannel();
                ByteBuffer buffer=ByteBuffer.wrap(error.getBytes());
                //Invio del messaggio di errore "Il servizio di traduzione non è disponibile" allo sfidante
                while(buffer.hasRemaining())
                    channelSfidante.write(buffer);
                buffer.clear();

                buffer = ByteBuffer.wrap(error.getBytes());
                //Invio del messaggio di errore "Il servizio di traduzione non è disponibile" allo sfidato
                while(buffer.hasRemaining())
                    channelSfidato.write(buffer);
                buffer.clear();

                //Prima di terminare il task imposta le chiavi in lettura e notifica il selettore del server di tale cambiamento
                sfidante.getServerKey().interestOps(SelectionKey.OP_READ);
                sfidato.getServerKey().interestOps(SelectionKey.OP_READ);
                sfidato.getServerKey().selector().wakeup();
                return;
            }
            //Registrazione nel selettore di questo task la chiave dell' utente sfidato
            channelSfidato.register(selector, SelectionKey.OP_WRITE, new ReadingByteBuffer(risposta));
            //Registrazione nel selettore di questo task la chiave dell' utente sfidante
            SocketChannel channelSfidante = sfidante.getUserChannel();
            channelSfidante.register(selector,SelectionKey.OP_WRITE,new ReadingByteBuffer(risposta));

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Task sfida = new Task();
            Future<String> future = executor.submit(sfida);
            try{
                future.get(18, TimeUnit.SECONDS);
            }
            catch (TimeoutException | InterruptedException | ExecutionException e){
                //Termine partita in caso in cui almeno uno degli utenti non è riuscito a tradurre le 3 parole
                future.cancel(true);
                System.out.println("Timeout partita scaduto");
            }

            // Vari controlli per capire lo stato di terminazione della partita da parte dei due utenti e mandare i rispettivi risultati e/o esiti
            String toSfidante = "";
            String toSfidato = "";
            if(finishSfidante && finishSfidato){
                //Hanno finito entrambi"
                if(MandatoRisultatoSfidato){
                    //Ho già inviato il numero di risposte corrette allo sfidato
                    toSfidato = calcolaVincitore("sfidato");
                    toSfidante = calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                }
                else {
                    //Ho già inviato il numero di risposte corrette allo sfidante
                    toSfidato = calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");
                    toSfidante = calcolaVincitore("sfidante");
                }
            }
            else {
                if (finishSfidato) {
                    // Solo lo sfidato ha finito
                    toSfidante = calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                    toSfidato += calcolaVincitore("sfidato");
                }
                else if(finishSfidante){
                    // Ha finito solo lo sfidante
                    toSfidante += calcolaVincitore("sfidante");
                    toSfidato = calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");
                }
                else {
                    // Nessuno dei due utenti ha finito la partita
                    System.out.println("Sfidante:");
                    toSfidante = calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                    System.out.println("Sfidato:");
                    toSfidato = calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");
                }
            }

            // Aggiornamento dei punteggi degli utenti che hanno partecipato alla sfida con i relativi punti vinti.
            aggiornaClassifica();

            //Aggiornamento messaggio da inviare nel ReadingByteBuffer associato alla key dello sfidante
            SelectionKey key = sfidante.getChallengeKey();
            ReadingByteBuffer buffer =(ReadingByteBuffer) key.attachment();
            buffer.clear();
            buffer.updateMessagge(toSfidante+EOM);
            writeRequestFinal(key);

            //Aggiornamento messaggio da inviare nel ReadingByteBuffer associato alla key dello sfidato
            key = sfidato.getChallengeKey();
            buffer = (ReadingByteBuffer) key.attachment();
            buffer.clear();
            buffer.updateMessagge(toSfidato+EOM);

            //Invio del messaggio di conclusione partita
            writeRequestFinal(key);

            //Prima di terminare il task imposta le chiavi in lettura e notifica il selettore server di tale cambiamento
            sfidante.getServerKey().interestOps(SelectionKey.OP_READ);
            sfidato.getServerKey().interestOps(SelectionKey.OP_READ);
            sfidato.getServerKey().selector().wakeup();
            System.out.println("ChallengeThread terminato");

        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (InterruptedException e){
            e.printStackTrace();
        } catch (ParseException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /*Metodo per inviare l' esito negativo della sfida da lui richiesta */
    private void responseAndTerminate()throws IOException{
        String risposta = "Richiesta di sfida non accettata"+EOM;
        SocketChannel channelSfidante = sfidante.getUserChannel();
        ByteBuffer buffer = ByteBuffer.wrap(risposta.getBytes());
        while(buffer.hasRemaining())
            channelSfidante.write(buffer);

        sfidante.getServerKey().interestOps(SelectionKey.OP_READ);
        sfidante.getServerKey().selector().wakeup();
    }

    /*Metodo per concludere la partita, il quale invia l' esito della partita sul SocketChannel relativo alla chiave passata come parametro
      e rimuovere tale chiave dal selettore utilizzato in questo task. */
    private void writeRequestFinal(SelectionKey currentKey)throws IOException{
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        String risposta=attachment.getMessage();
        ByteBuffer buffer=attachment.getByteBuffer();

        buffer=ByteBuffer.wrap(risposta.getBytes());
        while(buffer.hasRemaining()){
            client.write(buffer);
        }

        attachment.updateMessagge("");
        attachment.clear();

        System.out.println("Messaggio finale che invia il ThreadSfida: " + risposta);
        currentKey.cancel();
    }

    /*Oggetto di tipo Task che continua a controllare le chiavi selezionate dal selettore finchè entrambi i giocatori non hanno finito la partita */
    class Task implements Callable<String> {
        @Override
        public String call() throws Exception {
            boolean save = false;
            while( !(finishSfidato && finishSfidante)){
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                if(!save) {
                    saveAndSet(selectedKeys.iterator());
                    save = true;
                }
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                if(finishSfidante && finishSfidato)
                    break;

                while(iterator.hasNext()){
                    SelectionKey currentKey = iterator.next();
                    iterator.remove();
                    try {
                        if (!currentKey.isValid())
                            continue;
                        if (currentKey.isReadable()) {
                            System.out.println("------READ REQUEST------");
                            readRequest(currentKey);
                        }
                        if (currentKey.isWritable()) {
                            System.out.println("-------WRITE REQUEST------");
                            writeRequest(currentKey);
                        }
                    }catch (IOException  e){

                        System.out.println("------TERMINE CONNESSIONE------");
                        SocketChannel client = (SocketChannel) currentKey.channel();
                        currentKey.cancel();
                        client.close();
                    }
                }
            }
            return "TIMEOUT!";
        }

        // salvo le chiavi nella struttura dei due sfidanti
        public void saveAndSet(Iterator<SelectionKey> iterator)throws IOException{
            while (iterator.hasNext()) {
                SelectionKey currentKey = iterator.next();
                iterator.remove();
                if (currentKey.channel().equals(sfidante.getUserChannel()))
                    sfidante.setChallengeKey(currentKey);

                else
                    sfidato.setChallengeKey(currentKey);
            }

        }
    }

    //Metodo di lettura ed elaborazione nuove risposte da parte dei client in partita
    private void readRequest(SelectionKey newReadRequest) throws IOException, InterruptedException {
        SocketChannel client = (SocketChannel) newReadRequest.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
        attachment.getByteBuffer().clear();

        int num = client.read(attachment.getByteBuffer());
        if(num==-1) //Caso di arresto anomalo da parte del client
            throw new IOException();

        // Controllo che restituisce true se la stringa contenuta nel ByteBuffer allegato alla chiave passata come argomento finisce con "_EOM"
        if(attachment.updateOnRead()) {
            String risposta = attachment.getMessage();
            attachment.clear();
            System.out.println("Messaggio arrivato al server: " + risposta );
            if(sfidante.getUserChannel().equals(client)) {
                //La chiave newReadRequest è dello sfidante
                    if (indiceSfidante == 2) {
                        // Lo sfidante ha tradotto tutte le parole
                        finishSfidante = true;
                        attachment.updateMessagge(calcolaPunteggio(traduzioneSfidante, sfidante.getNickname())+EOM);
                    }
                    if (indiceSfidante < 2) {
                        // Lo sfidante ha ancora parole da tradurre
                        traduzioneSfidante.add(risposta);
                        indiceSfidante++;
                        attachment.updateMessagge(randomWords.get(indiceSfidante) + EOM);
                    }
            }
            else {
                //La chiave newReadRequest è dello sfidato
                if (indiceSfidato == 2) {
                    //Lo sfidato ha tradotto tutte le parole
                    finishSfidato = true;
                    attachment.updateMessagge(calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + EOM);
                }
                if (indiceSfidato < 2) {
                    //Lo sfidato ha ancora parole da tradurre
                    traduzioneSfidato.add(risposta);
                    indiceSfidato++;
                    attachment.updateMessagge(randomWords.get(indiceSfidato) + EOM);
                }
            }

            newReadRequest.interestOps(SelectionKey.OP_WRITE);
            attachment.clear();
        }

    }

    //Metodo di scrittura messaggio contenuto nel ReadingByteBuffer allegato alla chiave currentKey
    private void writeRequest(SelectionKey currentKey)throws IOException{
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        String nuovaParola=attachment.getMessage();
        ByteBuffer buffer=attachment.getByteBuffer();

        buffer=ByteBuffer.wrap(nuovaParola.getBytes());
        while(buffer.hasRemaining()){
            client.write(buffer);
        }

        attachment.updateMessagge("");
        attachment.clear();

        System.out.println("Messaggio che invia il ThreadSfida: "+nuovaParola);
        currentKey.interestOps(SelectionKey.OP_READ);

        if(client.equals(sfidante.getUserChannel())){
            if(nuovaParola.contains("Hai tradotto correttamente"))
                MandatoRisultatoSfidante = true;
            if(finishSfidante)
                currentKey.interestOps(0);
        }
        if(client.equals(sfidato.getUserChannel())){
            if(nuovaParola.contains("Hai tradotto correttamente"))
                MandatoRisultatoSfidato = true;
            if(finishSfidato)
                currentKey.interestOps(0);
        }

    }

    /*Metodo che restituisce la stringa contente il numero di parole tradotte correttamente/sbagliate/non tradotte dell' utente user,
      memorizzate nella corrispettiva struttura dati */
    private String calcolaPunteggio(ArrayList<String> risposteUser,String user) throws InterruptedException {
        int i;
        int punteggio = 0;
        int sbagliate =0;
        int giuste = 0;
        for(i=0; i < risposteUser.size(); i++){
            try {
                if (risposteUser.get(i).equalsIgnoreCase(paroleTradotte.get(i)))
                    giuste++;
                else
                    sbagliate++;
            }catch (NullPointerException e){
                e.printStackTrace();
            }catch (IndexOutOfBoundsException e){
                e.printStackTrace();
            }
        }

        if(user.equals(sfidante.getNickname())) {
            punteggio = (giuste*2) + (sbagliate*-1);
            if(punteggio<=0)
                punteggio = 0;
            punteggioSfidante = punteggio;
        }
        else {
            punteggio = (giuste*2) + (sbagliate*-1);
            if(punteggio<=0)
                punteggio = 0;
            punteggioSfidato = punteggio;
        }
        return  "Hai tradotto correttamente " + giuste + " ,ne hai sbagliate " + sbagliate +
                " e non risposto a " + (parolePerGioco-i) + ".\nHai totalizzato " + punteggio +" punti.\n";
    }

    //Restituisce la stringa finale contenente l' esito della partita dell user
    private String calcolaVincitore(String user){
        String result = "";
        if(user.equals("sfidato")){
            result = "Il tuo avversario ha totalizzato " + punteggioSfidante + " punti\n";
            if(punteggioSfidato > punteggioSfidante) {
                punteggioSfidato += 3;
                result += "Congratulazioni, hai vinto! Hai guadagnato " + punteggioSfidato + " punti!";
            }
            else
                if(punteggioSfidato<punteggioSfidante)
                    result += "Peccato, hai perso.......  Hai guadagnato "+ punteggioSfidato+ " punti!";
                else
                    result += "Pareggio, poteva andare meglio! Hai guadagnato " + punteggioSfidato + " punti!";
            return  result;
        }
        else{
            result = "Il tuo avversario ha totalizzato " + punteggioSfidato + " punti\n";
            if(punteggioSfidante > punteggioSfidato) {
                punteggioSfidante += 3;
                result += "Congratulazioni, hai vinto! Hai guadagnato" + punteggioSfidante + " punti!";
            }
            else
                if(punteggioSfidante < punteggioSfidato)
                    result +="Peccato, hai perso.......  Hai guadagnato "+ punteggioSfidante + " punti!";
                else
                    result += "Pareggio, poteva andare meglio! Hai guadagnato "+punteggioSfidante+" punti!";
            return result;
        }
    }

    /*Metodo synchronized per evitare problemi di concorrenza con altri Thread che, avendo terminato la propria partita, intendono aprire
      il file ClassificaPunti.json contemporaneamente. Funzione utilizzata aggiornare il punteggio degli utenti che hanno partecipato a questa
      partita, aggiungendo i rispettivi punti guadagnati.*/
    private synchronized void aggiornaClassifica()throws IOException{
        JSONParser parser = new JSONParser();
        Object obj=null;
        try {
            obj = parser.parse(new FileReader("ClassificaPunti.json"));
        }catch (Exception e){
            e.printStackTrace();
        }
        JSONArray jsonArray = (JSONArray) obj;
        boolean trovato = false;

        for(int i=0; i<jsonArray.size(); i++){
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            if(jsonObject.get("nickname").equals(sfidante.getNickname())){
                obj = jsonObject.get("punteggio");
                int punteggio = Integer.parseInt(obj.toString()) + punteggioSfidante;
                jsonArray.remove(jsonObject);
                i--;
                JSONObject nuovoValore= new JSONObject();
                nuovoValore.put("nickname",sfidante.getNickname());
                nuovoValore.put("punteggio",punteggio);
                jsonArray.add(nuovoValore);
                if(trovato==true)
                    break;
                trovato = true;
            }
            if(jsonObject.get("nickname").equals(sfidato.getNickname())){
                obj = jsonObject.get("punteggio");
                int punteggio = Integer.parseInt(obj.toString()) + punteggioSfidato;
                jsonArray.remove(jsonObject);
                i--;
                JSONObject nuovoValore= new JSONObject();
                nuovoValore.put("nickname",sfidato.getNickname());
                nuovoValore.put("punteggio",punteggio);
                jsonArray.add(nuovoValore);
                System.out.println("Punteggio: "+punteggio);
                if(trovato==true)
                    break;
                trovato = true;
            }
        }

        File file= new File("ClassificaPunti.json");
        FileWriter fileWriter= new FileWriter(file);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();
    }

    // Metodo per richiedere tramite HTTP al servizio MyMemoryTranslated la traduzione di parole italiane.
    private void traduci()throws ParseException,IOException {
        paroleTradotte = new ArrayList<String>();
        BufferedReader in = null;
        URL url = null;
        HttpURLConnection connection = null;
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;

        for(int i = 0; i < parolePerGioco; i++){
            System.out.println("Parola inviata: " + randomWords.get(i));
            url = new URL("https://api.mymemory.translated.net/get?q="+ randomWords.get(i) + "&langpair=it|en");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.close();
            in = new BufferedReader( new InputStreamReader(connection.getInputStream()));
            String inputLine,risposta="";
            while((inputLine = in.readLine()) != null) {
                risposta +=inputLine;
                System.out.println(inputLine);
            }
            jsonObject = (JSONObject) parser.parse(risposta);
            jsonObject = (JSONObject) jsonObject.get("responseData");
            risposta = (String) jsonObject.get("translatedText");
            paroleTradotte.add(risposta);
        }
        in.close();
        connection.disconnect();

    }

}