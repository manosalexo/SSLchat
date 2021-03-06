package ssl.chat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashSet;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

/**
 * A multi threaded chat room server.  
 * When a client connects the server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until a unique one is received.
 * 
 * After a client submits a unique name, the server acknowledges with "NAMEACCEPTED".
 * 
 * Then all messages from that client will be broadcast to all other clients that have 
 * submitted a unique screen name.  The broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple chat server, 
 * there are a few features that have been left out.
 * 
 * Two are very useful and belong in production code:
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *     2. The server should do some logging.
 */
public class ChatServer {
	private SSLServerSocket sslServerSocket = null;
    private final int PORT = 9001;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use. Generic
     */
    private static HashSet<String> names = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages. Generic
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    //constructor
    ChatServer() throws Exception{
        System.out.println("The chat server is running.");
        prepareServerKeys(PORT);
        SSLSocket sslSocket=null;
        try{
            while(true) {
            	sslSocket= (SSLSocket)sslServerSocket.accept();
            	new Handler(sslSocket).start();
            }
        }finally {
        	sslSocket.close();
        }
    }
    
    
    /**
     * ==============================================================
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     * ==============================================================
     */
    private static class Handler extends Thread {
        private String name;
        private SSLSocket sslSocket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(SSLSocket socket) {
            this.sslSocket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                out= new PrintWriter(sslSocket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while(true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) { return; }
                    synchronized (names) {
                        if(!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) { return; }
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + ": " + input);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if(name != null) { names.remove(name); }
                if(out  != null) { writers.remove(out);}
                try {
                    sslSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
	private void prepareServerKeys(int port) throws Exception{
		//Load server private key================================
		KeyStore serverKeys = KeyStore.getInstance("JKS");
		serverKeys.load(new FileInputStream("keystore/plainserver.jks"), "mypass".toCharArray());
		KeyManagerFactory serverKeyManager = KeyManagerFactory.getInstance("SunX509");
		serverKeyManager.init(serverKeys, "mypass".toCharArray());
		
		//Load client public key=================================
		KeyStore clientPub = KeyStore.getInstance("JKS");
		clientPub.load(new FileInputStream("keystore/clientpub.jks"),"mypass".toCharArray());
		TrustManagerFactory trustManager = TrustManagerFactory.getInstance("SunX509");
		trustManager.init(clientPub);
		
		//Use keys to create SSLSocket===========================
		SSLContext ssl= SSLContext.getInstance("TLS");
		ssl.init(serverKeyManager.getKeyManagers(), trustManager.getTrustManagers(), SecureRandom.getInstance("SHA1PRNG"));
		
		sslServerSocket = (SSLServerSocket)ssl.getServerSocketFactory().createServerSocket(port);
		sslServerSocket.setNeedClientAuth(true);
	}
    
    
    /**
     ****************************************************************
     * The application main method, which just listens on a port and
     * spawns handler threads.
     ****************************************************************/
    public static void main(String[] args) throws Exception {
    	new ChatServer();
    }
    
}