import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;

public class MainClient {


    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, ParseException {
        try{
            Client test = new Client(8080, 6666);

            test.login("luca98", "prova98");

        }catch (IOException e){
            System.out.print("Server non disponibile riprovare più tardi");
        }



    }

}
