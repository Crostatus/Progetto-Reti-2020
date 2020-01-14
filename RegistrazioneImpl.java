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

public class RegistrazioneImpl extends UnicastRemoteObject implements RegistrazioneInterface {
    private JSONObject Credenziali;
    private JSONObject ClassificaPunti;
    private JSONObject ListaAmici;

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

    // aggiunge l'utente nel file dei punteggi
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

    // aggiunge l'utente e la lista amici vuota
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
    //          2 => la password non è valida
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
        System.out.println(((JSONArray) obj).toJSONString());
        while(iterator.hasNext()) {
            JSONObject oggetto = iterator.next();
            if(oggetto.get("nickname").equals(nickname))
                return 1;
        }
        if(password == null || password.length()<=5 || !noDigit(password) )
            return 2;

        JSONObject jsonObject =new JSONObject();
        jsonObject.put("nickname",nickname);
        jsonObject.put("password",password);
        jsonArray.add(jsonObject);
        File file= new File("Credenziali.json");
        FileWriter fileWriter= new FileWriter(file);
        fileWriter.write(jsonArray.toJSONString());
        fileWriter.flush();
        fileWriter.close();
        System.out.println("Utente: " + nickname + "Pass: " + password);
        return 0;
    }

    // restituisce true se la password contiene meno di due cifre, false altrimenti
    private boolean noDigit(String p){
        int count=0;
        for(int i=0;i<p.length();i++)
            if(Character.isDigit(p.charAt(i))) count++;
        if(count<=2) return true;
        return false;
    }

    // controllo se il file ha dimensione 0, in quel caso lo inizializzo
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
