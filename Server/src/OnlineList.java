import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class OnlineList{
    private CopyOnWriteArrayList<OnlineUser> list;

    public OnlineList(){
        list = new CopyOnWriteArrayList<>();
    }

    // returna true se ho aggiunto l'user, false altrimenti
    public synchronized boolean addUser(String nickname, SocketChannel userChannel, SelectionKey key){
        OnlineUser newUser = new OnlineUser(nickname, userChannel, key);

        if(containsUser(nickname) == null) {
            list.add(newUser);
            return true;
        }
        return false;
    }

    // returna true se ho rimosso l'utente corrispondente a nickname, false altrimenti
    public synchronized boolean removeUser(String nickname){
        OnlineUser userToRemove;
        if((userToRemove = containsUser(nickname)) != null) {
            list.remove(userToRemove);
            return true;
        }
        return false;
    }

    // returna true se ha rimosso l'utente corrispondente a socketChannel, false altrimenti
    public synchronized boolean removeUser(SocketChannel socketChannel){
        OnlineUser userToRemove;
        if((userToRemove = containsUser(socketChannel)) != null) {
            list.remove(userToRemove);
            return true;
        }
        return false;
    }

    // returna OnlineUser se l' utente è già presente nella lista, null altrimenti
    public synchronized OnlineUser containsUser(String nickName){

        OnlineUser currentUser = null;
        Iterator<OnlineUser> iterator = list.iterator();
        boolean userAlreadyOnline = false;
        while(iterator.hasNext() && !userAlreadyOnline){
            currentUser = iterator.next();
            userAlreadyOnline = currentUser.compare(nickName);
        }
        //System.out.println("userAlreadyOnline: "+userAlreadyOnline);
        if(userAlreadyOnline)
            return currentUser;

        return null;
    }

    //returna OnlineUser se il socketChannel è già presente nella lista, null altrimenti
    private synchronized OnlineUser containsUser(SocketChannel socketChannel){
        OnlineUser currentUser = null;
        Iterator<OnlineUser> iterator = list.iterator();
        boolean userAlreadyOnline = false;
        while(iterator.hasNext() && !userAlreadyOnline){
            currentUser = iterator.next();
            userAlreadyOnline = currentUser.compare(socketChannel);
        }
        //System.out.println("userAlreadyOnline: "+userAlreadyOnline);
        if(userAlreadyOnline)
            return currentUser;

        return null;
    }

    //restituisce l' oggetto di tipo OnlineUser dell' giocatore richiesto
    public OnlineUser getPlayer(String playerToFind){
        int i = 0;
        while(i < list.size()){
            if(list.get(i).nickname.equals(playerToFind))
                return list.get(i);

            i++;
        }
        //da gestire, è il caso in cui un utente va offline tra il momento della
        // ricezione di richiesta di sfida e l' apertura del thread per la sfida
        return null;
    }

    // stampa la lista di utenti Online
    public void printList(){
        System.out.println("\nLista utenti: ");
        for(int i=0;i<list.size();i++)
            list.get(i).printUser();
        System.out.println(":\n");

    }

    //Controlla se l'utente ha effettuato il login
    public boolean checkLogin(String nickName){
        if(containsUser(nickName) != null) return true;
        return false;
    }

    // returna true se l'associazione tra user e socketChannel è corretta, false altrimenti
    public synchronized boolean checkAssociazione(String user, SocketChannel socketChannel){
        OnlineUser utente = containsUser(user);
        if(utente == null)
            return false;
        else
            if(socketChannel.equals(utente.getUserChannel()))
                return true;
            else
                return false;
    }




// classe che rappresenta il singolo utente online
    public class OnlineUser {
    private SocketChannel userChannel;
    private String nickname;
    private SelectionKey serverKey;
    private SelectionKey challengeKey;
    // aggiungere porta UDP che mi dice dove il client vuole che invio i dati in UDP

    public OnlineUser(String nickname, SocketChannel userChannel, SelectionKey serverKey) throws NullPointerException {
        if (nickname == null)
            throw new NullPointerException("nickname =null");
        if (userChannel == null)
            throw new NullPointerException("invalid socketChannel");

        this.nickname = nickname;
        this.userChannel = userChannel;
        this.serverKey = serverKey;

    }

    public void setChallengeKey(SelectionKey key){
        this.challengeKey = key;
    }

    public SelectionKey getChallengeKey(){return challengeKey;}

    public SelectionKey getServerKey(){ return serverKey;}

    // returna il socketChannel dell'utente
    public SocketChannel getUserChannel() {
        return userChannel;
    }

    // returna il nickname dell'utente
    public String getNickname() {
        return nickname;
    }

    // returna true se il nickname corrisponde a quello dell'utente, false altrimenti
    public boolean compare(String nickname) {
        return nickname.equals(this.nickname);
    }

    // returna true se il socketChannel corrisponde a quello dell'utente, false altrimenti
    public boolean compare(SocketChannel socketChannel) {
        return socketChannel.equals(this.userChannel);
    }

    // stampa nickname e socketChannel dell'utente
    public void printUser() {
        System.out.println("Username: " + nickname + " Indirizzo: " + userChannel);
    }
    }
}