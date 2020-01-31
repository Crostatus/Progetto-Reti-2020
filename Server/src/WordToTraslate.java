import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class WordToTraslate {

    private ArrayList<String> dizionario;

    public WordToTraslate() throws IOException {
        dizionario = new ArrayList<>();
        BufferedReader lettore = new BufferedReader(new FileReader("Dizionario.txt"));

        String parola;
        while((parola = lettore.readLine())!= null){
            dizionario.add(parola);
        }

    }

    public ArrayList<String> getRandomWord(){
        Random random = new Random();
        ArrayList<String>  wordList = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            wordList.add(dizionario.get(random.nextInt(dizionario.size())));
        }
        return wordList;
    }
}
