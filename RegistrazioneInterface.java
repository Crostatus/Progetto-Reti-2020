import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrazioneInterface  extends Remote {

    int registra_utente(String nickname, String password) throws IOException;
}
