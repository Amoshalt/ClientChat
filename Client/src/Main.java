import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Main
{

	public static void main(String[] args)
    {

    	String serveur = "192.168.0.13";
    	Client client = new Client(serveur);
    	client.start();
    	/*client.DemandeAuth();
    	System.out.println("Demande terminée");
		client.DemandeConv();*/







    }

}