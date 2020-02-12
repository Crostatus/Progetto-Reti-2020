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

public class ChallengeThread extends Thread{
    /*public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\033[0;34m";
    public static final String GREEN = "\u001B[32m";*/
    public static final int parolePerGioco =3;
    private CopyOnWriteArrayList<String> traduzioneSfidante;
    private CopyOnWriteArrayList<String> traduzioneSfidato;
    private CopyOnWriteArrayList<String> paroleTradotte;
    private int indiceSfidante;
    private int indiceSfidato;
    private DatagramSocket DSocket;
    private OnlineList.OnlineUser sfidante;
    private OnlineList.OnlineUser sfidato;
    //private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private  String EOM="_EOM";
    private ArrayList<String> randomWords;

    private boolean finishSfidante;
    private boolean finishSfidato;
    private boolean MandataTraduzioneSfidante;
    private boolean MandataTraduzioneSfidato;
    private int punteggioSfidante;
    private int punteggioSfidato;

    public ChallengeThread(OnlineList.OnlineUser sfidante, OnlineList.OnlineUser sfidato, ArrayList<String> randomWords)throws SocketException {
        this.sfidato = sfidato;
        this.indiceSfidante = 0;
        this.indiceSfidato = 0;
        this.traduzioneSfidante  = new CopyOnWriteArrayList<String>();
        this.traduzioneSfidato = new CopyOnWriteArrayList<String>();
        this.randomWords = randomWords;
        this.sfidante = sfidante;
        int port = sfidante.getUserChannel().socket().getLocalPort();
        //System.out.println(port);
        DSocket = new DatagramSocket(port);
        // controllare caso in cui la porta del datagramPacket è uguale ad un altra porta della sfida
        DSocket.setSoTimeout(10000);
    }

    public void run(){
        String richiesta = sfidante.getNickname()+" ti vuole sfidare! Vuoi accettare, si o no?";
        System.out.println("Thread Sfida invia: "+richiesta);
        byte[] dati= richiesta.getBytes();

        int porta = sfidato.getUserChannel().socket().getPort();
        DatagramPacket packet = new DatagramPacket(dati, dati.length, sfidato.getUserChannel().socket().getInetAddress(), porta);
        String risposta="";
        try{
            DSocket.send(packet);
            DSocket.receive(packet);
        }
        catch (SocketTimeoutException e){
            // controllo che vada tutto bene
            System.out.println("Timeout scaduto");
            DSocket.close();
            try {
                responseAndTerminate();
            }catch (IOException z){
                z.printStackTrace();
            }
            Thread.currentThread().interrupt();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        dati = packet.getData();
        risposta = new String(dati, 0, packet.getLength());
        System.out.println("Pacchetto ricevuto: "+risposta);
        DSocket.close();

        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(risposta.equals("si")){
            risposta = "Via alla sfida di traduzione:\n"+randomWords.get(0)+EOM;
            SocketChannel channelSfidato = sfidato.getUserChannel();
            SelectionKey keySfidato = sfidato.getServerKey();
            keySfidato.interestOps(0);
            try {
                try {
                    traduci();
                }catch (IOException e){
                    String error = "Il servizio di traduzione non è disponibile" + EOM;
                    SocketChannel channelSfidante = sfidante.getUserChannel();
                    ByteBuffer buffer=ByteBuffer.wrap(error.getBytes());
                    while(buffer.hasRemaining())
                        channelSfidante.write(buffer);
                    buffer.clear();
                    buffer = ByteBuffer.wrap(error.getBytes());
                    while(buffer.hasRemaining())
                        channelSfidato.write(buffer);
                    buffer.clear();

                    sfidante.getServerKey().interestOps(SelectionKey.OP_READ);
                    sfidato.getServerKey().interestOps(SelectionKey.OP_READ);
                    sfidato.getServerKey().selector().wakeup();

                    return;
                }

                channelSfidato.register(selector, SelectionKey.OP_WRITE, new ReadingByteBuffer(risposta));
                SocketChannel channelSfidante = sfidante.getUserChannel();
                channelSfidante.register(selector,SelectionKey.OP_WRITE,new ReadingByteBuffer(risposta));
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Task sfida = new Task();
                Future<String> future = executor.submit(sfida);
                try{
                    future.get(18, TimeUnit.SECONDS);
                }catch (TimeoutException | InterruptedException | ExecutionException e){
                    //e.printStackTrace();
                    future.cancel(true);
                   /* String toSfidante = "";
                    String toSfidato = "";
                    //System.out.println("DIM sfidato: "+traduzioneSfidato.size()+" ,DIM sfidante: "+traduzioneSfidante.size());
                    if(finishSfidante && finishSfidato){
                        System.out.println("Hanno finito entrambi");
                        toSfidante = calcolaVincitore("sfidante");
                        toSfidato = calcolaVincitore("sfidato");
                    }else {
                        if (finishSfidato) {
                            // ha finito solo lo sfidato
                            System.out.println("ha finito solo lo sfifato");
                            toSfidante = calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                            toSfidato += calcolaVincitore("sfidato");
                        } else if(finishSfidante){
                            // ha finito solo lo sfidante
                            System.out.println("ha finito solo lo sfidante");
                            toSfidante += calcolaVincitore("sfidante");
                            toSfidato = calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");
                        }else {
                            // non ha finito nessuno dei due
                            System.out.println("Sfidante:");
                            toSfidante = calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                            System.out.println("Sfidato:");
                            toSfidato = calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");

                        }
                    }

                    SelectionKey key = sfidante.getChallengeKey();
                    ReadingByteBuffer buffer =(ReadingByteBuffer) key.attachment();
                    buffer.clear();
                    buffer.updateMessagge(toSfidante+EOM);

                    writeRequestFinal(key);

                    key = sfidato.getChallengeKey();
                    buffer = (ReadingByteBuffer) key.attachment();
                    buffer.clear();
                    buffer.updateMessagge(toSfidato+EOM);

                    writeRequestFinal(key);*/

                    System.out.println("timeout scaduto");
                }

                // calcolo chi ha vinto e mando il risultato
                String toSfidante = "";
                String toSfidato = "";
                //System.out.println("DIM sfidato: "+traduzioneSfidato.size()+" ,DIM sfidante: "+traduzioneSfidante.size());
                if(finishSfidante && finishSfidato){
                    System.out.println("Hanno finito entrambi");
                    if(MandataTraduzioneSfidato){
                        toSfidato = calcolaVincitore("sfidato");
                        toSfidante = calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                    }
                    else {
                        toSfidato = calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");
                        toSfidante = calcolaVincitore("sfidante");
                    }
                }else {
                    if (finishSfidato) {
                        // ha finito solo lo sfidato
                        System.out.println("ha finito solo lo sfifato");
                        toSfidante = calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                        toSfidato += calcolaVincitore("sfidato");
                    } else if(finishSfidante){
                        // ha finito solo lo sfidante
                        System.out.println("ha finito solo lo sfidante");
                        toSfidante += calcolaVincitore("sfidante");
                        toSfidato = calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");
                    }else {
                        // non ha finito nessuno dei due
                        System.out.println("Sfidante:");
                        toSfidante = calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                        System.out.println("Sfidato:");
                        toSfidato = calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");

                    }
                }
                //toSfidato = calcolaPunteggio(traduzioneSfidato,sfidato.getNickname()) + calcolaVincitore("sfidato");
                //toSfidante = calcolaPunteggio(traduzioneSfidante,sfidante.getNickname()) + calcolaVincitore("sfidante");
                //System.out.println("Punteggio sfidante: "+punteggioSfidante+ " Punteggio sfidato: "+punteggioSfidato);
                aggiornaClassifica();

                SelectionKey key = sfidante.getChallengeKey();
                ReadingByteBuffer buffer =(ReadingByteBuffer) key.attachment();
                buffer.clear();
                buffer.updateMessagge(toSfidante+EOM);

                writeRequestFinal(key);

                key = sfidato.getChallengeKey();
                buffer = (ReadingByteBuffer) key.attachment();
                buffer.clear();
                buffer.updateMessagge(toSfidato+EOM);

                writeRequestFinal(key);

                sfidante.getServerKey().interestOps(SelectionKey.OP_READ);
                sfidato.getServerKey().interestOps(SelectionKey.OP_READ);
                sfidato.getServerKey().selector().wakeup();

                Thread.interrupted();
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

        }else {
            try {
                responseAndTerminate();
            } catch (IOException e) {

                e.printStackTrace();
            }
            return;
        }
    }

    // aggiorno il messaggio che il threadSfida deve mandare ai due sfidanti
    // e li metto in write
    private void setFinal(String toSfidante, String toSfidato)throws IOException{
        int i=0;
        SelectionKey key =sfidante.getChallengeKey();
        key.interestOps(SelectionKey.OP_WRITE);
        System.out.println(key.isWritable());
        selector.wakeup();
        key = sfidato.getChallengeKey();
        key.interestOps(SelectionKey.OP_WRITE);
        System.out.println(key.isWritable());
        selector.wakeup();
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        System.out.println("Ci sono "+selectedKeys.size()+" chiavi nel selettore");

        selector.select();
        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        while (iterator.hasNext()){
            SelectionKey currentKey = iterator.next();
            iterator.remove();

            SocketChannel client = (SocketChannel) currentKey.channel();
            ReadingByteBuffer buffer = (ReadingByteBuffer) currentKey.attachment();
            System.out.println("Messaggio "+buffer.getMessage());
            buffer.clear();
            if(client.equals(sfidante.getUserChannel()))
                buffer.updateMessagge(toSfidante);
            else
                buffer.updateMessagge(toSfidato);
            currentKey.interestOps(SelectionKey.OP_WRITE);
            writeRequestFinal(currentKey);
        }
        System.out.println("Distruggo il ThreadSfida");
        selector.close();
        Thread.interrupted();

    }

    // informo il client sfidante che lo sfidato ha rifiutato la sfida
    // e riporto la chiave dello sfidante in read nel server
    private void responseAndTerminate()throws IOException{
        String risposta = "Richiesta di sfida non accettata"+EOM;
        SocketChannel channelSfidante = sfidante.getUserChannel();

        sfidante.getServerKey().interestOps(SelectionKey.OP_READ);
        // sveglio il selettore del server e gli dico che ha una nuova
        // chiave pronta per una lattura
        sfidante.getServerKey().selector().wakeup();
        System.out.println(" interestOps messo a READ " + sfidante.getServerKey().isReadable());
        ByteBuffer buffer = ByteBuffer.wrap(risposta.getBytes());
        channelSfidante.write(buffer);
    }

    private void writeRequestFinal(SelectionKey currentKey)throws IOException{
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        String risposta=attachment.getMessage();
        /*System.out.println("Parola da mandare: " + nuovaParola);
        System.out.println("BB: " + attachment.getByteBuffer());*/
        ByteBuffer buffer=attachment.getByteBuffer();

        buffer=ByteBuffer.wrap(risposta.getBytes());
        while(buffer.hasRemaining()){
            client.write(buffer);
        }

        attachment.updateMessagge("");
        attachment.clear();

        System.out.println("Messaggio finale che invia il ThreadSfida: "+risposta);
        currentKey.cancel();
    }

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

                if(finishSfidante && finishSfidato) break;
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

    private void readRequest(SelectionKey newReadRequest) throws IOException, InterruptedException {
        SocketChannel client = (SocketChannel) newReadRequest.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) newReadRequest.attachment();
        attachment.getByteBuffer().clear();
        int num = client.read(attachment.getByteBuffer());
        System.out.println("numero byte letti: "+num);

        //vedo quando il client termina improvvisamente
        if(num==-1)throw new IOException();

        // Entro dentro quando ho finito di leggere
        if(attachment.updateOnRead()) {
            String risposta = attachment.getMessage();
            attachment.clear();
            System.out.println("Messaggio arrivato al server: " + risposta );
            // per riconoscere chi mi ha scritto, devo confrontare i canali!!!
            if(sfidante.getUserChannel().equals(client)) {
                    if (indiceSfidante == 2) {
                        finishSfidante = true;
                        System.out.println("Lo sfidante ha finito");
                        attachment.updateMessagge(calcolaPunteggio(traduzioneSfidante, sfidante.getNickname())+EOM);
                    }
                    if (indiceSfidante < 2) {
                        traduzioneSfidante.add(risposta);
                        indiceSfidante++;
                        attachment.updateMessagge(randomWords.get(indiceSfidante) + EOM);
                    }
            }
            else {
                    if (indiceSfidato == 2) {
                        finishSfidato = true;
                        System.out.println("lo sfidato ha finito");
                        attachment.updateMessagge(calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + EOM);

                    }
                    if (indiceSfidato < 2) {
                        traduzioneSfidato.add(risposta);
                        indiceSfidato++;
                        attachment.updateMessagge(randomWords.get(indiceSfidato) + EOM);
                    }
            }

            newReadRequest.interestOps(SelectionKey.OP_WRITE);
            attachment.clear();
        }

    }

    private void writeRequest(SelectionKey currentKey)throws IOException{
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        String nuovaParola=attachment.getMessage();
        /*System.out.println("Parola da mandare: " + nuovaParola);
        System.out.println("BB: " + attachment.getByteBuffer());*/
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
            if(nuovaParola.equals("Hai tradotto correttamente"))
                MandataTraduzioneSfidante = true;
            if(finishSfidante)
                currentKey.interestOps(0);
        }

        if(client.equals(sfidato.getUserChannel())){
            if(nuovaParola.equals("Hai tradotto correttamente"))
                MandataTraduzioneSfidato = true;
            if(finishSfidato)
                currentKey.interestOps(0);
        }

    }

    // termina il thread e mette le chiavi dei due giocatori nel server in read
    private void terminateThread() throws IOException {
        selector.close();
        sfidante.getServerKey().interestOps(SelectionKey.OP_READ);
        sfidato.getServerKey().interestOps(SelectionKey.OP_READ);
        Thread.interrupted();
    }

    // restituisce la stringa parziale finale per user (il rislutato della propria sfida)
    private String calcolaPunteggio(CopyOnWriteArrayList<String> risposteUser,String user) throws InterruptedException {
        int i;
        int punteggio = 0;
        int sbagliate =0;
        int giuste = 0;
        //System.out.println("Dimensione array user: "+risposteUser.size()+" ,dimensione array paroletradote: "+paroleTradotte.size());
        for(i=0; i < risposteUser.size(); i++){
            try {
                System.out.print(risposteUser.get(i)+" VS "+paroleTradotte.get(i));
                if (risposteUser.get(i).equalsIgnoreCase(paroleTradotte.get(i))) {
                    System.out.println("    ->TRUE");
                    giuste++;
                }
                else {
                    System.out.println("    ->FALSE");
                    sbagliate++;
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }catch (IndexOutOfBoundsException e){
                //System.out.println("Indice: "+i);
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
        //System.out.println("NUOVO punteggio: "+punteggio);
        return  "Hai tradotto correttamente " + giuste + " ,ne hai sbagliate " + sbagliate +
                " e non risposto a " + (parolePerGioco-i) + ".\nHai totalizzato " + punteggio +" punti.\n";
    }

    // restituisce la stringa finale (risultato avversario e partita ), aggiorna il punteggio(locale) in base al vincitore
    private String calcolaVincitore(String user){
        String result = "";
        if(user.equals("sfidato")){
            result = "Il tuo avversario ha totalizzato " + punteggioSfidante + " punti\n";
            if(punteggioSfidato>punteggioSfidante) {
                punteggioSfidato += 3;
                result += "Congratulazioni, hai vinto! Hai guadagnato " + punteggioSfidato + " punti!";
            }
            else
                if(punteggioSfidato<punteggioSfidante)
                    result += "Peccato, hai perso.......  Hai guadagnato "+ punteggioSfidato+ " punti!";
                else
                    result += "Pareggio, poteva andare meglio! Hai guadagnato "+punteggioSfidato+" punti!";
            return  result;
        }
        else{
            result = "Il tuo avversario ha totalizzato " + punteggioSfidato + " punti\n";
            if(punteggioSfidante>punteggioSfidato) {
                punteggioSfidante += 3;
                result += "Congratulazioni, hai vinto! Hai guadagnato" + punteggioSfidante + " punti!";
            }
            else
                if(punteggioSfidante<punteggioSfidato)
                    result +="Peccato, hai perso.......  Hai guadagnato "+ punteggioSfidante + " punti!";
                else
                    result += "Pareggio, poteva andare meglio! Hai guadagnato "+punteggioSfidante+" punti!";
            return result;
        }
    }

    // aggiorna la classifica punti dei due sfidanti
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
                //System.out.println("Punteggio: "+punteggio);
                if(trovato==true)break;
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
                if(trovato==true)break;
                trovato = true;
            }
        }

        File file= new File("ClassificaPunti.json");
        FileWriter fileWriter= new FileWriter(file);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();
    }

    // richiesta al sito delle traduzioni di alcune parole
    private void traduci()throws ParseException,IOException {
        paroleTradotte = new CopyOnWriteArrayList<String>();
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