import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

    public synchronized int registra_utente(String nickname, String password) throws Exception{
        JSONParser parser = new JSONParser();
        Object obj=null;
        JSONArray ja= new JSONArray();
        /*File file= new File("Credenziali.json");
        FileWriter fileWriter= new FileWriter(file);
        fileWriter.write(ja.toJSONString());
        fileWriter.flush();
        fileWriter.close();*/
        try {
            obj = parser.parse(new FileReader("Credenziali.json"));
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Qui tutto bene");
        JSONArray jsonArray = (JSONArray) obj;
        Iterator<JSONObject>  iterator= jsonArray.iterator();
        System.out.println(((JSONArray) obj).toJSONString());
        while(iterator.hasNext()) {
            JSONObject oggetto = iterator.next();
            System.out.println(oggetto.get("nickname").equals(nickname));
            if(oggetto.get("nickname").equals(nickname)) return 1;
        }
        /*if(password.length()<=5  )
            return 2;*/

        System.out.println("Qui tutto bene1");
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

    private boolean noDigit(String p){
        int count=0;
        for(int i=0;i<p.length();i++)
            if(Character.isDigit(p.charAt(i))) count++;
        if(count<=2) return true;
        return false;
    }

}
