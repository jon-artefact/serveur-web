import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by SuRvYv0r on 15/06/2015.
 */
public class Serveur extends Thread {

    private ServerSocket ss;

    // Create a new Serveur on the default port : 80
    public Serveur() {
        this(80);
    }

    public Serveur(int _port) {
        try {
            ss = new ServerSocket( _port );
        } catch (IOException ex) {
            System.err.println("Impossible de lancer le serveur sur le port " + _port + " : " + ex.getMessage());
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket sock;
                // Attente de demande de connexion
                sock = ss.accept();

                // Création d'une connexion
                Connexion connexion = new Connexion(sock);
                connexion.start();

            } catch (IOException ex) {
                System.err.println("Problème lors de la connection : " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Serveur s = new Serveur();
        s.run();
    }
}
