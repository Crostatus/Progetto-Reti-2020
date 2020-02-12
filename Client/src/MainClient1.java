import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.rmi.NotBoundException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainClient1 {


    public static void main(String[] args) throws Exception {


        ClientUI clientUI = new ClientUI();

        /*Client prova = new Client(8080,6666);

        prova.login("andrea", "prova98");
        //prova.aggiungi_amico("andrea","alessandro");
        prova.sfida("andrea", "alessandro","");
        //Thread.sleep(10000);
        prova.lista_amici("andrea");*/
        /*boolean logout = false;
        AtomicBoolean inAMatch;
        Client test1 = new Client(8080, 6666);
        Scanner scanner = new Scanner(System.in);
        String richiesta;

        stampaComandi();
        test1.login("alessandro","prova98",scanner);
        test1.sfida("alessandro","andrea");
        while (!logout) {
            if(test1.getBoolean() == true) Thread.sleep(1111);
            //synchronized (lock) {
                richiesta = scanner.nextLine();
                StringTokenizer tokenizer = new StringTokenizer(richiesta);
                String comando = "";
                try {
                    comando = tokenizer.nextToken();
                } catch (NoSuchElementException e) {
                    System.out.println("Non è stato inserito nessun comando");
                }
                switch (comando) {
                    case "registra": {
                        try {
                            String nome = tokenizer.nextToken();
                            String pass = tokenizer.nextToken();
                            test1.registra(nome, pass);
                            break;
                        } catch (NoSuchElementException e) {
                            System.out.println("Controllare i dati inseriti");
                            break;
                        }
                    }
                    case "login": {
                        try {
                            String nome = tokenizer.nextToken();
                            String pass = tokenizer.nextToken();
                            test1.login(nome, pass, scanner);
                            break;
                        } catch (NoSuchElementException e) {
                            System.out.println("ERRORE: controllare i dati inseriti");
                            break;
                        }
                    }
                    case "aggiungi_amico": {
                        String nome = tokenizer.nextToken();
                        String amico = tokenizer.nextToken();
                        test1.aggiungi_amico(nome, amico);
                        break;
                    }
                    case "logout": {
                        test1.logout();
                        logout = true;
                        break;
                    }
                    case "lista_amici": {
                        String nome = tokenizer.nextToken();
                        test1.lista_amici(nome);
                        break;
                    }
                    case "sfida": {
                        try {
                            String sfidante = tokenizer.nextToken();
                            String sfidato = tokenizer.nextToken();
                            test1.sfida(sfidante, sfidato);
                            break;
                        } catch (NoSuchElementException e) {
                            System.out.println("ERRORE: controllare i dati inseriti");
                            break;
                        }
                    }
                    case "--help": {
                        stampaComandi();
                        break;
                    }
                    default: {
                        System.out.println("Comando non valido");
                        break;
                    }
                }


            //}
        }
    }
    private static void stampaComandi(){
        System.out.println("Comands:");
        System.out.println("registra_utente <utente> <password>  registral'utente");
        System.out.println("login <nickUtente > <password > effettua il login");
        System.out.println("logout effettua il logout");
        System.out.println("aggiungi_amico <nickAmico> crea relazione di amicizia con nickAmico");
        System.out.println("lista_amici mostra la lista dei propri amici");
        System.out.println("sfida <nickAmico > richiesta di una sfida a nickAmico");
        System.out.println("mostra_punteggio mostra il punteggio dell’utente");
        System.out.println("mostra_classifica mostra una classifica degli amici dell’utente");
*/
    }

}
