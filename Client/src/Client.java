import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Client extends Thread
{

	public final static int Exit = 0, //Code envoy� par le client � la fermeture

				ACK = 1,

				Refuse = 2, //Erreur par d�faut

				TestConnexion = 3, //Teste si le client est toujours actif

				Online = 4,	//Valeur retourn�e par le client pour signaler qu'il est toujours actif

				UsrListe = 7, //Liste des correspondances ID <-> username envoy� par le serveur au client � la connexion

				NewChat = 11, //Demande de cr�ation de chat, suivi du nom des dest.

				NewUse = 15, //Code envoy� par le serveur pour notifier un client de la connexion d'un nouvel utilisateur

				DelUse = 16, //Code envoy� par le serveur pour notifier un client de la d�connexion d'un utilisateur

				TestAuth = 32, //Code envoy� par le client pour s'authentifier, suivi du pseudo sans espace

				NewMsg = 42, //Envoi d'un msg, suivi de l'ID du chat et du message

				MsgOK = 43, //Code envoy� au client par le serveur pour indiquer que le message a �t� transmi.

				MsgErr = 44, //Code envoy� au client par le serveur pour indiquer que le message n'a pas �t� transmi

				NeedAuth = 100, //Demande d'authentification du serveur vers le client

				PseudoInvalide = 101, //Erreur d'authentification retourn�e par le serveur

				AuthOK = 102, //Validation de l'authentification

				IDChatInvalide = 110, //Impossible d'�crire dans ce chat

				RetourIDChat = 111, //Retourne l'ID du chat voulu, suivi

				ContactInvalide = 112 //Erreur : le destinataire voulu n'existe pas, suivi du pseudo invalide

			;

	private Scanner m_sc;
	private Conversation[] m_conv;
	private int m_nbConv;
	private InetAddress m_ia;
	private int m_port;
	private DatagramSocket m_socket;
	private boolean m_continuer;
	private boolean m_close;
	private ArrayList<User> m_users;

	//Constructor

	public Client (String serveur)
	{
		m_continuer = true;
		m_close = false;
		m_sc = new Scanner(System.in);
		m_conv = new Conversation[127];
		try {
			m_ia = InetAddress.getByName(serveur);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_port = 1234;
		m_nbConv = 0;
		try
		{
			m_socket = new DatagramSocket();
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}

		m_users = new ArrayList<User>(128);

	}

	public void run()
	{
		DemandeAuth();
		DatagramPacket dp = Reception();
		if(dp != null)
			Traitement(dp);

		while(m_continuer)
			Action();

		m_socket.close();
		m_close = true;
		m_sc.close();
		System.out.println("Client ferme.");
		DestroyClient();

	}

	protected void Action()
	{

		EnvoyerMessage();


		DatagramPacket dp = Reception();
		if(dp != null)
			Traitement(dp);
	}

	//Destructor
	public void DestroyClient()
	{
		m_continuer = false;

	}

	//Envoi d'un DTG jusqu'� 3 fois, test de r�ception du ACK
    public boolean send(byte[] tab)
    {
    	DatagramPacket dp = new DatagramPacket(tab, tab.length, m_ia, m_port);
    	try
    	{
        	//On s'assure de ne pas �changer une suite interminable de ACK avec le client
    		if(tab[0] != ACK)
    		{
    			int i;
	    		//On essaye d'envoyer le DTG jusqu'� 3 fois si on ne re�oit aucun ACK
	    		for(i = 0; i < 3; i = (ACK() ? 5 : (i + 1)))
	    			m_socket.send(dp);
	    		if(i == 3)
	    			return false;
    		}
    		else m_socket.send(dp);
		}
    	catch (IOException e1)
    	{
			e1.printStackTrace();
		}
    	return true;
    }

    //Retourne "true" � la la r�ception du ACK correspondant au num�ro du DTG,
    //		   "false" si on passe par un Timeout
    public boolean ACK()
    {
    	DatagramPacket packet;
    	try
    	{
    		m_socket.setSoTimeout(500);
    		//On re�oit jusqu'� recevoir un ACK pour le DTG voulu ou avoir un Timeout
    		while(true)
    		{
	    		packet = new DatagramPacket(new byte[512], 512);
	    		m_socket.receive(packet);
	    		//Si on a un ACK pour le DTG, on retourne "true"
	    		if(packet.getData()[0] == ACK)
	    			return true;
    		}
    	}
    	catch(IOException e)
    	{
    		//Timeout : on retourne "false"
    		return false;
    	}
    }

    public void SendACK()
    {
    	DatagramPacket packet;
    	try
    	{
    		byte[] data = new byte[] {ACK};
    		packet = new DatagramPacket(data, data.length, m_ia, m_port);
    		m_socket.send(packet);
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
    }

    //Receptionne un DTG et envoie un ACK
    public DatagramPacket Reception()
    {
    	//m_timeout = false;
    	DatagramPacket packet;
    	try
    	{
    		//On attend jusqu'� 3 secondes pour recevoir une r�ponse, si on ne re�oit qu'un ACK, on recommence
    		do
    		{
	    		m_socket.setSoTimeout(3000);
	    		packet = new DatagramPacket(new byte[512], 512);
	    		m_socket.receive(packet);
	    		if(packet.getData()[0] == ACK) System.out.println("ACK recu");
    		}while(packet.getData()[0] == ACK);
    		//Si le packet n'est pas un ACK, on en retourne un au client
    		SendACK();
    		return packet;
    	}
    	catch(IOException e)
    	{
    		//Timeout
    		if(!(e instanceof SocketTimeoutException))
    			e.printStackTrace();
    		else System.out.println("Timeout :\n> Adresse : " + m_ia.getHostAddress() + "\n> Port : " + m_port);
    	}
    	return null;
    }

    //Traitement d'un datagramPacket
    public void Traitement(DatagramPacket dp)
    {
    	/*byte flag = 100;
    	DatagramPacket dp = new DatagramPacket(new byte[512], 512);
    	dp.setData(ByteBuffer.allocate(512).put((byte)AuthOK).put(" on est mal la".getBytes()).array());*/
    	InetAddress ia = dp.getAddress();
    	byte[] data = dp.getData();
    	int port = dp.getPort();
    	System.out.println("Adresse : " + ia + "\n" +
    					   "Port : " + port + "\n" +
    					   "Donn�es : " + new String(data));
    	byte dpFlag= data[0];
    	switch(dpFlag)
    	{
    		case RetourIDChat:
    			NouvelleConversation(data);

    			break;

    		case PseudoInvalide:
    			System.out.println("Pseudo Invalide");
    			DemandeAuth();
    			break;

    		case Refuse:
    			break;

    		case TestConnexion:
    			ConfirmerActivite();
    			break;

    		case NeedAuth:
    			DemandeAuth();
    			break;

    		case AuthOK:
    			System.out.println("Authentification reussie");
    			//DemandeConv();
    			break;

    		case IDChatInvalide:
    			System.out.println("serveur complet");
    			break;

    		case ContactInvalide:
    			System.out.println("Contact Invalide");
    			DemandeConv();
    			break;

    		case Exit:
    			break;

    		case ACK:
    			break;

    		case NewMsg:
    			//EnregistrerMessage(data);
    			AfficherMessage(data);
    			break;

    		case UsrListe:
    			RentreUtilisateurs(data);
    			break;

    		case NewUse:
    			RentreUtilisateurs(data);
    			break;

    		case DelUse:
    			SuppUtilisateur(data[1]);
    			break;

    		case NewChat:
    			NouveauChat();
    			break;

    		case MsgOK:
    			break;

    		case MsgErr:
    			System.out.println("Votre messsage n'a pas �t� envoy�");
    			break;

    	}
    	affichage(dp);
    }

    private void NouveauChat() {
    	ByteBuffer bbuff = ByteBuffer.allocate(512);
    	System.out.println("Rentrez les id des utilisateurs avec qui vous voulez discuter dans la liste ci-dessous");
    	AfficherListeUsr();
    	String id;
    	int j ;
    	boolean entiers = true;
    	do
		{
    		j= 0;
    		System.out.println("Id utilisateur:");
    		id = m_sc.nextLine();

    		while( j < id.length() && entiers)
    		{
    			if (id.charAt(j) <'0' || id.charAt(j) > '9')
    				{
    					entiers = false;
    				}

    		}


		}while (true);

	}

    private void AfficherListeUsr()
    {
    	for(int i = 0; i < m_users.size(); i++)
    		{
    		System.out.println(m_users.get(i).getUsername() + " : " + m_users.get(i).getId());
    		}
    }

	private void SuppUtilisateur(byte idUsr) {


		User tempUsr = TrouveUser(idUsr);

		if (tempUsr.getId() != ((byte) 0))
		{
			m_users.remove(m_users.indexOf(tempUsr));
		}



	}

	public User TrouveUser(byte idUsr)
	{
    	int i = 0;
		while ( i < m_users.size())
    	{
    		if (m_users.get(i).getId() == idUsr)
    		{
    			return new User(m_users.get(i).getId(),m_users.get(i).getUsername());

    		}
    		else
    		{
    			System.out.println("L'utilisateur n'a pas �t� trouv� dans notre liste");
    		}
    		i++;
    	}
		return new User();
	}


	private void RentreUtilisateurs(byte[] data) {
		// TODO Auto-generated method stub

		byte idUsr = data[1];
		byte[] bytesUsr = Arrays.copyOfRange(data, 2, data.length);

		m_users.add(new User(idUsr,bytesUsr.toString()));

	}

	//Reception du message et enregistrement dans la conversation correspondante
    public void EnregistrerMessage(byte[] data)
    {
    	byte idConvAct = data[1];
    	byte idEmetteur = data[2];
    	int i = getConv(idConvAct);
    	if( i == -1)
    	{
    		NouvelleConversation(data);
    		i = getConv(idConvAct);
    	}

    	Conversation conv = m_conv[i];
    	conv.addMess(data,idEmetteur);


    }

    //Demande du pseudo � l'utilisateur
    public String RentrerPseudo()
    {
    	String pseudo= "";
    	boolean contientEspace;

    	do
		{
    		contientEspace = false;
    		System.out.println("Rentrez votre pseudo");
    		pseudo = m_sc.nextLine();
    		int i = 0;
    		while (i < pseudo.length())
    		{
    			if(pseudo.charAt(i) == ' ')
    			{
    				contientEspace = true;
    			}

    			i++;
    		}

		}while (contientEspace);


    	System.out.println("pseudo rentr�: " + pseudo);
    	return pseudo;
    }

    //Demande authentification au serveur
    public void DemandeAuth()
    {
    	String pseudo = RentrerPseudo();
    	System.out.println("Le pseudo est bien rentr�");

    	ByteBuffer bbuff = ByteBuffer.allocate(512);
    	bbuff.put((byte)TestAuth).put(pseudo.getBytes());

    	if(send(bbuff.array()) == true)
    	{
    		System.out.println("pseudo envoy�");
    	}
    	else
    	{
    		System.out.println("pseudo non envoy�");
    	}
       	DatagramPacket dp = Reception();
       	Traitement(dp);

    }

    //Demande au serveur l'acces a une conversation
    public void DemandeConv()
    {
    	byte section = 0;
    	byte[] flag = new byte[2];
    	flag[0] = 11;
    	flag[1]= 1;


    	ByteBuffer bbuff = ByteBuffer.allocate(512);
		bbuff.put(flag);
    	String temp ="0";
    	int i = 1;
    	while (!temp.isEmpty())
		{
    		temp="";
    		System.out.println("Saisissez le "+ i + " �me destinataire \n Appuyer sur entrer si vous avez fini. \n");
    		temp = m_sc.nextLine();
    		bbuff.put(temp.getBytes());
    		bbuff.put(section);
    		i++;
		}

    	send(bbuff.array());

    	DatagramPacket dp = Reception();
    	Traitement(dp);
    }

    //rentrer un message
    public void EntrerMessage()
    {

    }

    //affichage du message et du propri�taire

    public void AfficherMessage(byte[] data)
    {
    	String name = TrouveUser(data[2]).getUsername();
    	System.out.println(name + " : " + Arrays.copyOfRange(data, 3, data.length).toString());

    }

    //Envoyer un message
    public void EnvoyerMessage()
    {
    	byte section = 0;
    	ByteBuffer bbuff = ByteBuffer.allocate(512);
		bbuff.put((byte)NewMsg);
		bbuff.put((byte) 0);
    	String tempMessage ="0";
    	System.out.println("Saisissez votre message \n Appuyer deux fois sur entrer si vous avez fini. \n");
    	tempMessage = m_sc.nextLine();
		bbuff.put(tempMessage.getBytes());
		bbuff.put(section);


    	send(bbuff.array());

    }

    //Affichage des donn�es
    public void affichage(DatagramPacket dp)
    {
    	System.out.println(new String(dp.getData()));
    }

    //Creation nouvelle conversation en cours
    public void NouvelleConversation(byte[] data)
    {

    	m_conv[m_nbConv]= new Conversation(data[1]);
    	m_nbConv++;
    }

  //Creation nouvelle conversation en cours
    public void SupprimerConversation(byte id)
    {

    }

  //Repondre � la demande d'activit� du serveur
    public void ConfirmerActivite()
    {
    	ByteBuffer bbuff = ByteBuffer.allocate(512)
    			.put((byte) Online);
    	send(bbuff.array());

    	DatagramPacket dp = Reception();
    	Traitement(dp);
    }

    //Getter Setter

    //recup�re l'index de la conversation avec l'id byte
    public int getConv(byte id)
    {
    	int i = 0;
    	boolean trouve = false;
    	while(i < m_conv.length && !trouve)
    	{
    		if (m_conv[i].getIdConv() == id)
    		{
    			trouve = true;
    			return i;
    		}
    		else
    		{
    			i++;
    		}

    	}

    	return -1;
    }
}
