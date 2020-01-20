import java.io.IOException;
import java.rmi.NotBoundException;

public class MainClient {


    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException {
        try{
            Client test = new Client(8080, 6666);

            test.registra("alessandro","prova98");
            test.login("alessandro", "prova98");

        }catch (IOException e){
            System.out.print("Server non disponibile riprovare più tardi");
        }



    }

}
