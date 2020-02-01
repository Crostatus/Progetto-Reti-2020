import javafx.concurrent.Task;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

public class ChallengeThread extends Thread{
    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\033[0;34m";
    public static final String GREEN = "\u001B[32m";
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
    private long startTime;

    private boolean finishSfidante;
    private boolean finishSfidato;
    private int punteggioSfidante;
    private int punteggioSfidato;

    // inizzializza l'array delle traduzioni dei partecipanti al gioco
    private void inizzializzaArray(CopyOnWriteArrayList<String> traduzioni){
        int i;
        for(i=0; i<parolePerGioco; i++){
            traduzioni.add(i,"");
        }
    }

    public ChallengeThread(OnlineList.OnlineUser sfidante, OnlineList.OnlineUser sfidato, ArrayList<String> randomWords)throws SocketException {
        this.sfidato = sfidato;
        this.indiceSfidante = 0;
        this.indiceSfidato = 0;
        this.traduzioneSfidante  = new CopyOnWriteArrayList<String>();
        this.traduzioneSfidato = new CopyOnWriteArrayList<String>();
        inizzializzaArray(traduzioneSfidante);
        inizzializzaArray(traduzioneSfidato);
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
        //System.out.println("     Porta: "+porta);
        DatagramPacket packet = new DatagramPacket(dati, dati.length, sfidato.getUserChannel().socket().getInetAddress(), porta);
        try{
            DSocket.send(packet);
            DSocket.receive(packet);
        }
        catch (SocketTimeoutException e){
            // controllo che vada tutto bene
            System.out.println("Timeout scaduto");
            Thread.currentThread().interrupt();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        String risposta=null;
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
            SelectionKey keySfidato = sfidato.getKey();
            keySfidato.interestOps(0);
            try {
                traduci();
                channelSfidato.register(selector, SelectionKey.OP_WRITE, new ReadingByteBuffer(risposta));
                SocketChannel channelSfidante = sfidante.getUserChannel();
                channelSfidante.register(selector,SelectionKey.OP_WRITE,new ReadingByteBuffer(risposta));
                startTime = System.currentTimeMillis();
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<String> future = executor.submit(new Task());
                try{
                    future.get(10, TimeUnit.SECONDS);
                }catch (TimeoutException | InterruptedException | ExecutionException e){
                    future.cancel(true);
                    System.out.println("timeout scaduto");
                }

                // calcolo chi ha vinto e mando il risultato
                String toSfidante = null;
                String toSfidato = null;
                System.out.println("DIM sfidato: "+traduzioneSfidato.size()+" ,DIM sfidante: "+traduzioneSfidante.size());
                if(finishSfidante && finishSfidato){
                    toSfidante = calcolaVincitore("sfidante");
                    toSfidato = calcolaVincitore("sfidato");
                }else {
                    if (finishSfidato) {
                        // ha finito solo lo sfidato
                        toSfidante += calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                        toSfidato += calcolaVincitore("sfidato");
                    } else if(finishSfidato){
                        // ha finito solo lo sfidante
                        toSfidante += calcolaVincitore("sfidante");
                        toSfidato += calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");
                    }else {
                        // non ha finito nessuno dei due
                        System.out.println("Sfidante:");
                        toSfidante += calcolaPunteggio(traduzioneSfidante, sfidante.getNickname()) + calcolaVincitore("sfidante");
                        System.out.println("Sfidato:");
                        toSfidato += calcolaPunteggio(traduzioneSfidato, sfidato.getNickname()) + calcolaVincitore("sfidato");

                    }
                }
                System.out.println("Punteggio sfidante: "+punteggioSfidante+ " Punteggio sfidato: "+punteggioSfidato);
                aggiornaClassifica();
                //channelSfidato.register(selector, SelectionKey.OP_WRITE, new ReadingByteBuffer(toSfidato));
                //channelSfidante.register(selector,SelectionKey.OP_WRITE, new ReadingByteBuffer(toSfidante));
                //sendResults();

                // salvare su JSON il punteggio dei due giocatori
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

    private void responseAndTerminate()throws IOException{
        String risposta = "Richiesta di sfida non accettata"+EOM;
        SocketChannel channelSfidante = sfidante.getUserChannel();

        sfidante.getKey().interestOps(SelectionKey.OP_READ);
        // sveglio il selettore del server e gli dico che ha una nuova
        // chiave pronta per una lattura
        sfidante.getKey().selector().wakeup();
        System.out.println(" interestOps messo a READ " + sfidante.getKey().isReadable());
        ByteBuffer buffer = ByteBuffer.wrap(risposta.getBytes());
        channelSfidante.write(buffer);
    }

    // ciclo multipexing per mandare i risultati finali
    private void sendResults()throws IOException{

        while(true){
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            int finish = 0;
            while(iterator.hasNext()){
                SelectionKey currentKey = iterator.next();
                iterator.remove();
                try {
                    if (!currentKey.isValid())
                        continue;
                    if (currentKey.isReadable()) {
                        System.out.println("------READ REQUEST------");
                        readRequestFinal(currentKey);
                    }
                    if (currentKey.isWritable()) {
                        System.out.println("-------WRITE REQUEST------");
                        writeRequestFinal(currentKey);
                        currentKey.cancel();
                        if(finish==2)
                            terminateThread();
                        finish++;
                    }
                }catch (IOException  e){
                    System.out.println("------TERMINE CONNESSIONE------");
                    SocketChannel client = (SocketChannel) currentKey.channel();
                    currentKey.cancel();
                    client.close();
                }
            }
        }
    }

    private void readRequestFinal(SelectionKey currentKey)throws IOException{
        SocketChannel client = (SocketChannel) currentKey.channel();
        ReadingByteBuffer attachment = (ReadingByteBuffer) currentKey.attachment();
        attachment.getByteBuffer().clear();
        int num = client.read(attachment.getByteBuffer());
        //System.out.println("numero byte letti: "+num);

        //vedo quando il client termina improvvisamente
        if(num==-1)throw new IOException();

        // Entro dentro quando ho finito di leggere
        if(attachment.updateOnRead()) {
            String risposta = attachment.getMessage();
            attachment.clear();
            System.out.println(GREEN+"Messaggio arrivato al server: " + risposta +RESET);
            // per riconoscere chi mi ha scritto, devo confrontare i canali!!!

            currentKey.interestOps(SelectionKey.OP_WRITE);
            attachment.clear();
        }
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
            //System.out.println("-----sto scrivendo e sono il thread");
            client.write(buffer);
        }

        attachment.updateMessagge("");
        attachment.clear();

        System.out.println(BLUE+"Messaggio che invia il ThreadSfida: "+risposta+RESET);
        currentKey.channel();
    }

    class Task implements Callable<String> {
        @Override
        public String call() throws Exception {
            while( !(finishSfidato && finishSfidante)){
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
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
            System.out.println(GREEN+"Messaggio arrivato al server: " + risposta +RESET);
            // per riconoscere chi mi ha scritto, devo confrontare i canali!!!
            if(sfidante.getUserChannel().equals(client)) {
                indiceSfidante++;
                if(indiceSfidante == 3){
                    finishSfidante = true;
                    attachment.updateMessagge(calcolaPunteggio(traduzioneSfidante, sfidante.getNickname())+EOM);
                }
                if(indiceSfidante < 3) {
                    traduzioneSfidante.add(indiceSfidante-1,risposta);
                    attachment.updateMessagge(randomWords.get(indiceSfidante) + EOM);
                }
            }
            else {
                indiceSfidato++;
                if(indiceSfidato == 3){
                    finishSfidato = true;
                    attachment.updateMessagge(calcolaPunteggio(traduzioneSfidato, sfidato.getNickname())+EOM);

                }
                if(indiceSfidato < 3) {
                    traduzioneSfidato.add(indiceSfidato-1,risposta);
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
            //System.out.println("-----sto scrivendo e sono il thread");
            client.write(buffer);
        }

        attachment.updateMessagge("");
        attachment.clear();

        if(client.equals(sfidante.getUserChannel()) && finishSfidante)
            currentKey.interestOps(0);

        if(client.equals(sfidato.getUserChannel()) && finishSfidato)
            currentKey.interestOps(0);


        System.out.println(BLUE+"Messaggio che invia il ThreadSfida: "+nuovaParola+RESET);
        currentKey.interestOps(SelectionKey.OP_READ);
    }

    // termina il thread e mette le chiavi dei due giocatori nel server in read
    private void terminateThread() throws IOException {
        selector.close();
        sfidante.getKey().interestOps(SelectionKey.OP_READ);
        sfidato.getKey().interestOps(SelectionKey.OP_READ);
        Thread.interrupted();
    }

    // restituisce la stringa parziale finale per user (il rislutato della propria sfida)
    private String calcolaPunteggio(CopyOnWriteArrayList<String> risposteUser,String user) throws InterruptedException {
        int i;
        int sbagliate =0;
        int punteggio = 0;
        System.out.println("Dimensione array user: "+risposteUser.size()+" ,dimensione array paroletradote: "+paroleTradotte.size());
        for(i=0; i<parolePerGioco; i++){
            try {
                System.out.print(risposteUser.get(i)+" VS "+paroleTradotte.get(i));
                if (risposteUser.get(i).equalsIgnoreCase(paroleTradotte.get(i))) {
                    System.out.println("    ->TRUE");
                    punteggio++;
                }
                else {
                    System.out.println("    ->FALSE");
                    sbagliate++;
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }catch (IndexOutOfBoundsException e){
                System.out.println("Indice: "+i);
                e.printStackTrace();
                Thread.sleep(2000);
            }
        }
        if(user.equals(sfidante.getNickname()))
            punteggioSfidante = punteggio;
        else
            punteggioSfidato = punteggio;
        System.out.println("NUOVO punteggio: "+punteggio);
        return  "Hai tradotto correttamente " + punteggio + " ,ne hai sbagliate " + sbagliate +
                " e non risposto a " + (parolePerGioco-i) + ".\nHai totalizzato " + punteggio +" punti.\n";
    }

    // restituisce la stringa finale (risultato avversario e partita ), aggiorna il punteggio(locale) in base al vincitore
    private String calcolaVincitore(String user){
        String result = "";
        if(user.equals("sfidato")){
            result = "Il tuo avversario ha totalizzato " + punteggioSfidato + "punti\n";
            if(punteggioSfidato>punteggioSfidante) {
                punteggioSfidato += 3;
                result += "Congratulazioni, hai vinto! Hai guadagnato 3 punti extra, per un totale di " + punteggioSfidato + " punti!";
            }
            else
                result +="Peccato, hai perso.......  Hai guadagnato "+ punteggioSfidato+ " punti!";
            return  result;
        }
        else{
            result = "Il tuo avversario ha totalizzato " + punteggioSfidante + "punti\n";
            if(punteggioSfidante>punteggioSfidato) {
                punteggioSfidante += 3;
                result += "Congratulazioni, hai vinto! Hai guadagnato 3 punti extra, per un totale di " + punteggioSfidante + " punti!";
            }
            else
                result +="Peccato, hai perso.......  Hai guadagnato "+ punteggioSfidante + " punti!";
            return result;
        }
    }

    // aggiorna la classifica punti dei due sfidanti
    private void aggiornaClassifica()throws IOException{
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
                System.out.println("Punteggio: "+punteggio);
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
    private void traduci()throws IOException,InterruptedException, ParseException {
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