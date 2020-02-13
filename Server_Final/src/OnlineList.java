import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/*
OVERVIEW: Un oggetto di tipo OnlineList implementa una struttura dati thread-safe per gestire
          gli utenti online registrati al servizio GameQuizzle. Gli elementi appartenenti a questa
          struttura dati, di tipo OnlineUser, sono anche essi definiti in questa classe.
 */

public class OnlineList{
    private CopyOnWriteArrayList<OnlineUser> list;

    public OnlineList(){
        list = new CopyOnWriteArrayList<>();
    }

    // Metodo per aggiungere elementi di tipo OnlineUser alla lista di persone online
    public synchronized boolean addUser(String nickname, SocketChannel userChannel, SelectionKey key){
        OnlineUser newUser = new OnlineUser(nickname, userChannel, key);

        if(containsUser(nickname) == null) {
            list.add(newUser);
            return true;
        }
        return false;
    }

    // Rimozione basata sul nickname di un OnlineUser presente nella struttura
    public synchronized boolean removeUser(String nickname){
        OnlineUser userToRemove;
        if((userToRemove = containsUser(nickname)) != null) {
            list.remove(userToRemove);
            return true;
        }
        return false;
    }

    // Rimozione basata sul socketChannel di un OnlineUser presente nella struttura
    public synchronized boolean removeUser(SocketChannel socketChannel){
        OnlineUser userToRemove;
        if((userToRemove = containsUser(socketChannel)) != null) {
            list.remove(userToRemove);
            return true;
        }
        return false;
    }

    //Cercando un OnlineUser grazie al suo nickName, viene cercato e restituito l' utente richiesto (null altrmenti).
    public synchronized OnlineUser containsUser(String nickName){
        OnlineUser currentUser = null;
        Iterator<OnlineUser> iterator = list.iterator();
        boolean userAlreadyOnline = false;
        while(iterator.hasNext() && !userAlreadyOnline){
            currentUser = iterator.next();
            userAlreadyOnline = currentUser.compare(nickName);
        }
        if(userAlreadyOnline)
            return currentUser;

        return null;
    }

    //Cercando un OnlineUser grazie al suo socketChannel, viene cercato e restituito l' utente richiesto (null altrmenti).
    private synchronized OnlineUser containsUser(SocketChannel socketChannel){
        OnlineUser currentUser = null;
        Iterator<OnlineUser> iterator = list.iterator();
        boolean userAlreadyOnline = false;
        while(iterator.hasNext() && !userAlreadyOnline){
            currentUser = iterator.next();
            userAlreadyOnline = currentUser.compare(socketChannel);
        }
        if(userAlreadyOnline)
            return currentUser;

        return null;
    }

    public OnlineUser getPlayer(String playerToFind){
        int i = 0;
        while(i < list.size()){
            if(list.get(i).nickname.equals(playerToFind))
                return list.get(i);

            i++;
        }
        return null;
    }

    // Verifica per controllare l' associazione <username, socketChannel> di un utente è corretta
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

    // Classe interna che rappresenta gli elementi contenuti in una struttura di tipo OnlineUser
    public class OnlineUser {
        private SocketChannel userChannel;
        private String nickname;
        private SelectionKey serverKey;
        private SelectionKey challengeKey;

        public OnlineUser(String nickname, SocketChannel userChannel, SelectionKey serverKey) throws NullPointerException {
            if (nickname == null)
                throw new NullPointerException("nickname == null");
            if (userChannel == null)
            throw new NullPointerException("Invalid socketChannel");
            this.nickname = nickname;
            this.userChannel = userChannel;
            this.serverKey = serverKey;

        }

        public void setChallengeKey(SelectionKey key){
            this.challengeKey = key;
        }

        public SelectionKey getChallengeKey(){return challengeKey;}

        public SelectionKey getServerKey(){ return serverKey;}

        public SocketChannel getUserChannel() {
            return userChannel;
        }

        public String getNickname() {
            return nickname;
        }

        public boolean compare(String nickname) {
            return nickname.equals(this.nickname);
        }

        public boolean compare(SocketChannel socketChannel) {
            return socketChannel.equals(this.userChannel);
        }

    }
}