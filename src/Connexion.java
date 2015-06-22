import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Created by SuRvYv0r on 15/06/2015.
 */
public class Connexion extends Thread {

    private final String VERSION_HTTP = "1.1";

    private final int TIMEOUT = 60000; // TimeOut de 1 minute
    private final String ROOT = "C:/Users/" + System.getProperty("user.name") + "/Desktop/www";

    private Socket socket;
    private PrintWriter out;

    HashMap<Integer, String> errors = new HashMap<>();

    private void setErrors() {
        errors.put(100, "Continue");
        errors.put(200, "OK");
        errors.put(400, "Bad Request");
        errors.put(403, "Forbidden");
        errors.put(404, "Not Found");
        errors.put(408, "Request Time-out");
        errors.put(500, "Internal Server Error");
        errors.put(501, "Not Implemented");
        errors.put(505, "HTTP Version not supported");
    }

    private void httpHeader(int errorCode) {
        out.println("HTTP/" + VERSION_HTTP + " " + errorCode + " " + errors.get(errorCode));
        out.println("Server: Jon/1.0.0");
        out.println("Date: " + new Date());
        out.println("Connection: close");
    }

    private void writeError(int errorCode) {
        httpHeader(errorCode);
        out.println("Content-Type: text/html");
        out.println();
        out.println("<HTML>");
        out.println("<HEAD><TITLE>" + errors.get(errorCode) + "</TITLE></HEAD>");
        out.println("<BODY>");
        out.println("<H2>Error " + errorCode + ": " + errors.get(errorCode) + ". </H2>");
        out.println("</BODY>");
        out.println ("</HTML>");
        out.flush();
    }

    class Requete {
        String methode;
        String fichier;

        /**
         * Cut the client's command
         *
         * @param line client's command read
         */
        public Requete (String line) {
            try {
                if (line != null) {
                    // On parse la requete
                    StringTokenizer parse = new StringTokenizer(line);
                    // On recupère la méthode choisie
                    methode = parse.nextToken().toUpperCase();
                    // On recupère le fichier demandé
                    fichier = parse.nextToken().toLowerCase();
                }
            } catch (NoSuchElementException e) {
                System.err.println("Erreur : " + e.getMessage());
            }
        }
    }

    public Connexion(Socket _socket) {
        socket = _socket;
        try {
            socket.setSoTimeout(TIMEOUT);
        } catch (SocketException ex) {
            System.out.println("Erreur socket time-out : " + ex.getMessage());
        }

        setErrors();
    }

    @Override
    public void run() {
        BufferedReader in = null;
        try {
            // Declaration des input et output
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());

            // On recupère la premiere ligne de la requete client
            String request = in.readLine();
            Requete req = new Requete(request);
            System.out.println(request);

            if(req.methode.equals("GET")) {
                if (req.fichier.endsWith("/")) {
                    // On envoie le index.html
                    req.fichier += "index.html";
                }

                // On récupère le fichier demandé
                File fichier = new File(ROOT, req.fichier);

                // On prépare l'enregistrement des données du fichier
                byte[] fileData = new byte[(int) fichier.length()];

                try {
                    //on ouvre le fichier et on le lit
                    FileInputStream fileIn = new FileInputStream(fichier);
                    fileIn.read(fileData);

                    close(fileIn); //close file input stream

                    httpHeader(200);
                    out.println("Content-Type: "+ getContentType(req.fichier));
                    out.println("Content-length: " + fileData.length);
                    out.println(); // Ligne supplémentaire entre l'entête et le contenu
                    out.flush();

                    // On envoie le fichier
                    BufferedOutputStream outData = new BufferedOutputStream(socket.getOutputStream());
                    outData.write(fileData, 0, fileData.length);
                    outData.flush();
                    close(outData);
                } catch (FileNotFoundException ex) {
                    writeError(404);
                } catch (IOException ex) {
                    writeError(500);
                }
            } else {
                // Les autres méthodes ne sont pas implémentées
                // On envoie donc un 501 : Not Implemented
                if (req.methode.equals("HEAD")
                        || req.methode.equals("PUT")
                        || req.methode.equals("POST")
                        || req.methode.equals("DELETE"))
                    writeError(501);
                else
                    writeError(400);
                // Sinon Bad Request
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Time Out : " + e.getMessage());
            writeError(408);
        } catch (IOException ex) {
            System.err.println("Erreur : " + ex.getMessage());
            writeError(500);
        } finally {
            close(in);
            close(out);
            close(socket);
        }
    }

    /**
     * Return the (MIME) content type of the file.
     *
     * @param file file prompt
     * @return MIME of file prompt
     */
    public String getContentType(String file) {
        if (file.endsWith(".htm") || file.endsWith(".html")) {
            return "text/html";
        } else if (file.endsWith(".gif")) {
            return "image/gif";
        } else if (file.endsWith(".jpg") || file.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (file.endsWith(".png")) {
            return "image/png";
        } else if (file.endsWith(".class") || file.endsWith(".jar")) {
            return "applicaton/octet-stream";
        }else if(file.endsWith(".pdf")){
            return "application/pdf";
        } else {
            return "text/plain";
        }
    }

    /**
     * Close a stream
     *
     * @param stream stream need to be close
     */
    public void close(Object stream) {
        if (stream == null) {
            return;
        }
        try {
            if (stream instanceof Reader) {
                ((Reader) stream).close();
            } else if (stream instanceof Writer) {
                ((Writer) stream).close();
            } else if (stream instanceof InputStream) {
                ((InputStream) stream).close();
            } else if (stream instanceof OutputStream) {
                ((OutputStream) stream).close();
            } else if (stream instanceof Socket) {
                ((Socket) stream).close();
            } else {
                System.err.println("Unable to close object: " + stream);
            }
        } catch (Exception e) {
            System.err.println("Error closing stream: " + e);
        }
    }
}
