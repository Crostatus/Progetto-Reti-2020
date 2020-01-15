import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.rmi.NotBoundException;

public class MainClient {


    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, ParseException {
        Client kostantino = new Client(8080, 6666);

        Client andrea = new Client(8080, 6666);
        Client alessandro = new Client(8080, 6666);
        kostantino.registra("kostantino", "puppasedani69");
        alessandro.registra("alessandro", "puppasedani69");
        andrea.registra("andrea", "puppasedani69");

        kostantino.login("kostantino", "puppasedani69");
        andrea.login("andrea", "puppasedani69");
        kostantino.aggiungi_amico("kostantino", "alessandro");
        kostantino.aggiungi_amico("andrea", "alessandro");
        alessandro.aggiungi_amico("alessandro", "andrea");
        kostantino.aggiungi_amico("kostantino", "luca");
        kostantino.lista_amici("kostantino");
        kostantino.logout();



    }

}
