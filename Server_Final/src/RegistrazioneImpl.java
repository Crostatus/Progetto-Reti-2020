import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;

/*
    OVERVIEW: Un oggetto di tipo RegistrazioneImpl implementa metodi per la scrittura nei file
              .json necessari per la memorizzazione di un nuovo utente che intente registrarsi al servizio
              GameQuizzle. Esso verrà infatti condiviso dal server tramite RMI sulla porta 8080.

*/
public class RegistrazioneImpl extends UnicastRemoteObject implements RegistrazioneInterface {

    public RegistrazioneImpl() throws RemoteException {
        super();
    }

    public synchronized int registra_utente(String nickname, String password) throws IOException {
        int controllo = setupCredenziali(nickname, password);
        if(controllo > 0)
            return controllo;

        setupPunteggi(nickname);
        setupAmici(nickname);

        return 0;
    }

    // Inizializzazione record dell' user con username "nickname" nel file "ClassificaPunti.json"
    private void setupPunteggi(String nickname) throws IOException{
        setupIfEmpty("ClassificaPunti.json");
        JSONParser parser = new JSONParser();
        Object obj=null;
        try {
            obj = parser.parse(new FileReader("ClassificaPunti.json"));
        }catch (Exception e){
            e.printStackTrace();
        }
        JSONArray jsonArray = (JSONArray) obj;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nickname",nickname);
        jsonObject.put("punteggio", 0);
        jsonArray.add(jsonObject);
        File file= new File("ClassificaPunti.json");
        FileWriter fileWriter= new FileWriter(file);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();
    }

    // Inizializzazione lista amici di un utente
    private void setupAmici(String nickname)throws IOException{
        setupIfEmpty("ListaAmici.json");
        JSONParser parser = new JSONParser();
        Object obj=null;

        try {
            obj = parser.parse(new FileReader("ListaAmici.json"));
        }catch (Exception e){
            e.printStackTrace();
        }
        JSONArray jsonArray = (JSONArray) obj;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nickname",nickname);
        jsonObject.put("lista_amici", new JSONArray());
        jsonArray.add(jsonObject);
        File file= new File("ListaAmici.json");
        FileWriter fileWriter= new FileWriter(file);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();
    }

    // Metodo per l' inizializzazione di un utente con annessi controlli.
    // Returns: 0 => registrazione avvenuta con successo
    //          1 => il nickname è già stato utilizzato
    private int setupCredenziali(String nickname, String password) throws IOException {
        JSONParser parser = new JSONParser();
        Object obj=null;
        setupIfEmpty("Credenziali.json");
        try {
            obj = parser.parse(new FileReader("Credenziali.json"));
        }catch (Exception e){
            e.printStackTrace();
        }
        JSONArray jsonArray = (JSONArray) obj;
        Iterator<JSONObject>  iterator= jsonArray.iterator();
        while(iterator.hasNext()) {
            JSONObject oggetto = iterator.next();
            if(oggetto.get("nickname").equals(nickname))
                return 1;
        }
        JSONObject jsonObject =new JSONObject();
        jsonObject.put("nickname",nickname);
        jsonObject.put("password",password);
        jsonArray.add(jsonObject);
        File file= new File("Credenziali.json");
        FileWriter fileWriter= new FileWriter(file);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();

        return 0;
    }

    private void setupIfEmpty(String fileName) throws IOException{
        File file = new File(fileName);
        JSONArray ja = new JSONArray();

        if((file.length()==0)) {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(ja.toJSONString());
            fileWriter.flush();
            fileWriter.close();
        }
    }
}
