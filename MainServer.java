import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainServer {
    public static void main(String[] args) throws Exception{
        checkFile();

        RegistrazioneInterface registra = new RegistrazioneImpl();
        LocateRegistry.createRegistry(8080);
        Registry reg = LocateRegistry.getRegistry(8080);
        reg.rebind("iscrizione", registra );



    }

    // controllo se i file esistono già, in caso ne mancasse anche solo uno
    // si cancellano tutti gli altri e si ricreano
    public static void checkFile() throws Exception{
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

    // funzione per cancellare tutti i file
    private static void cancellaFile()throws Exception{
        File dataCheck = new File("Credenziali.json");
        if(dataCheck.exists()) dataCheck.delete();

        dataCheck = new File("ClassificaPunti.json");
        if(dataCheck.exists()) dataCheck.delete();

        dataCheck = new File("ListaAmici.json");
        if(dataCheck.exists()) dataCheck.delete();

        System.out.println("Ho cancellato tutti i file!");
    }

    // funzione che genera tutti i file
    private static void generaFile() throws Exception{
        File dataCheck = new File("Credenziali.json");
        dataCheck.createNewFile();

        dataCheck = new File("ClassificaPunti.json");
        dataCheck.createNewFile();

        dataCheck = new File("ListaAmici.json");
        dataCheck.createNewFile();

        System.out.println("Ho creato i file!");
    }

}
