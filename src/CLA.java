import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Vector;
import javax.net.ssl.*;

import Common.Client;
import Common.Server;
import Common.Settings;
import Common.Voter;

public class CLA implements Runnable {
    // constants
    private static final String CLATRUSTSTORE = Settings.KEYLOCATION + "CLATruststore.ks";
    private static final String CLAKEYSTORE   = Settings.KEYLOCATION + "CLAKeystore.ks";
    private static final String CLAPASSWORD   = "password";

    private Vector<Voter> authorizedVoters;

    SSLSocket incoming;
    BufferedReader serverInput, clientInput;
    PrintWriter serverOutput, clientOutput;

    /**
     * Constructor for CLA
     * @param incoming
     */
    public CLA(SSLSocket incoming) {
        this.incoming = incoming;
    }

    /**
     * Setter for authorized voters
     * @param authorizedVoters
     */
    public void setAuthorizedVoters(Vector<Voter> authorizedVoters) {
        this.authorizedVoters = authorizedVoters;
    }

    /**
     * Function to authorize voters
     * @throws Exception
     */
    private void authorizeVoters() throws Exception {
        String str;
        while (!(str = serverInput.readLine()).equals(Settings.Commands.END)) {
            System.out.println("s: " + str);
            // remove 'id=' from string and parse as int
            int id = Integer.parseInt(str.substring(3));
            registerVoter(new Voter(-1, id));
        }
        clientOutput.println(Settings.Commands.TERMINATE);
    }

    /**
     * Register a voter
     * @param v
     * @return
     * @throws Exception
     */
    private String registerVoter(Voter v) throws Exception {
        if (!authorizedVoters.contains(v) && v.getId() > Settings.MIN_AGE) {
            v.setValidationNumber(BigInteger.probablePrime(
                    Settings.VALIDATION_BITLENGTH, new SecureRandom()));
            authorizedVoters.add(v);
            // respond to client and send to CTF
            serverOutput.println(v.fromCTF() + '\n' + Settings.Commands.END);
            sendToCTF(v.fromCTF());
        }

        return v.fromCTF();
    }

    /**
     * Sends the string to CTF
     * @param s
     * @throws Exception
     */
    private void sendToCTF(String s) throws Exception {
        clientOutput.println(Settings.Commands.REGISTER_VALID);
        clientOutput.println(s);
        clientOutput.println(Settings.Commands.END);
    }

    /**
     * Starts the CTF client
     * @param host
     * @param port
     * @throws Exception
     */
    private void startClient(InetAddress host, int port) throws Exception {
        Client client = new Client(CLAKEYSTORE, CLATRUSTSTORE, CLAPASSWORD, host, port);
        SSLSocket c = client.getSocket();
        clientInput = new BufferedReader(new InputStreamReader(c.getInputStream()));
        clientOutput = new PrintWriter(c.getOutputStream(), true);
    }

    /**
     * Function used to run the CTF
     */
    public void run() {
        try {
            // prepare incoming connections
            serverInput = new BufferedReader(
                    new InputStreamReader(incoming.getInputStream()));
            serverOutput = new PrintWriter(incoming.getOutputStream(), true);
            startClient(InetAddress.getLocalHost(), Settings.CTF_PORT);
            String str = serverInput.readLine();
            while (str != null) {
                switch (str) {
                    case Settings.Commands.CLIENT_CTF:
                        authorizeVoters();
                        break;
                    case "": break;
                    default:
                        System.out.println("Unknown command: " + str);
                        break;
                }

                str = serverInput.readLine();
            }
            incoming.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Main function for the CLA
     * @param args
     */
    public static void main(String[] args) {
        try {
            Server s = new Server(CLAKEYSTORE, CLATRUSTSTORE, CLAPASSWORD, Settings.CLA_PORT);
            // shared resource for all threads
            Vector<Voter> voters = new Vector<>();

            while (true) {
                SSLSocket socket = (SSLSocket) s.getServerSocket().accept();
                System.out.println("New client connected");
                CLA c = new CLA(socket);
                c.setAuthorizedVoters(voters);
                Thread t = new Thread(c);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
