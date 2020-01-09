import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MainClient {
    public static void main(String[] args) throws Exception, NotBoundException {
        Registry reg = LocateRegistry.getRegistry(8080);
        RegistrazioneInterface registra = (RegistrazioneInterface) reg.lookup("iscrizione");
        int risultato=registra.registra_utente("bastianich", "puppasedani69");
        if(risultato==0)
            System.out.println("Operazione avvenuta con successo");
        else if(risultato==1)
            System.out.println("Nickname già presente");
        else
            System.out.println("Password non valida, errore: "+risultato);

    }
}
