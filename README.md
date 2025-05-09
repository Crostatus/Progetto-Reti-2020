# 🎮 TranslateRush — Gioco Multiplayer di Traduzione in Tempo Reale  
> Progetto per il corso di Reti di Calcolatori (A.A. 2020)

TranslateRush è un videogioco client-server sviluppato in Java in cui due giocatori si sfidano in una gara 1 vs 1: vince chi riesce a tradurre più parole nel minor tempo possibile. Il progetto sfrutta socket TCP per la comunicazione e include un’interfaccia grafica per il client.

## 🕹️ Modalità di gioco

- Due client si connettono al server e avviano una partita.
- A ogni turno, viene mostrata una parola da tradurre.
- Il giocatore più veloce e corretto ottiene il punto.
- Dopo un numero configurabile di parole, viene dichiarato il vincitore.

## 🧱 Struttura del progetto

```
TranslateRush/
├── client/            # Codice client con interfaccia grafica (Swing)
│   ├── GameClient.java
│   └── ...
├── server/            # Codice del server TCP
│   ├── GameServer.java
│   └── ...
├── dizionario.txt     # Lista di parole e traduzioni
├── README.md          # Documentazione
└── ...
```

## 🛠️ Requisiti

- Java SE 8 o superiore
- Compilatore `javac`
- Ambiente desktop (per l’interfaccia Swing del client)

## 🚀 Avvio del gioco

### 1. Avvia il server

```bash
javac server/GameServer.java
java server.GameServer
```

### 2. Avvia i client (su due terminali distinti)

```bash
javac client/GameClient.java
java client.GameClient
```

Il client mostrerà una GUI dove il giocatore potrà inserire le traduzioni.

## ⚙️ Configurazione

- Il server ascolta su una porta TCP configurabile (default: `12345`)
- Il dizionario delle parole è contenuto nel file `dizionario.txt`, con righe del tipo:
  ```
  dog;cane
  house;casa
  apple;mela
  ```

## 📡 Tecnologie utilizzate

- Socket TCP per la comunicazione client-server
- Java Swing per l’interfaccia grafica
- Thread per la gestione simultanea dei client
- Strutture dati per dizionario e punteggi

## 📄 Documentazione

Tutti i dettagli tecnici e organizzativi sono contenuti in `README.md` e nel codice sorgente commentato.  
Il progetto è stato realizzato come esercitazione pratica per consolidare concetti di comunicazione in rete, concorrenza e GUI in Java.

## 📜 Licenza

Distribuito a fini didattici.  
