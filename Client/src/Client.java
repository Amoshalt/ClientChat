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

	public final static int Exit = 0, //Code envoyé par le client à la fermeture

				ACK = 1,

				Refuse = 2, //Erreur par défaut

				TestConnexion = 3, //Teste si le client est toujours actif

				Online = 4,	//Valeur retournée par le client pour signaler qu'il est toujours actif

				UsrListe = 7, //Liste des correspondances ID <-> username envoyé par le serveur au client à la connexion

				NewChat = 11, //Demande de création de chat, suivi du nom des dest.

				NewUse = 15, //Code envoyé par le serveur pour notifier un client de la connexion d'un nouvel utilisateur

				DelUse = 16, //Code envoyé par le serveur pour notifier un client de la déconnexion d'un utilisateur

				TestAuth = 32, //Code envoyé par le client pour s'authentifier, suivi du pseudo sans espace

				NewMsg = 42, //Envoi d'un msg, suivi de l'ID du chat et du message

				MsgOK = 43, //Code envoyé au client par le serveur pour indiquer que le message a été transmi.

				MsgErr = 44, //Code envoyé au client par le serveur pour indiquer que le message n'a pas été transmi

				NeedAuth = 100, //Demande d'authentification du serveur vers le client

				PseudoInvalide = 101, //Erreur d'authentification retournée par le serveur

				AuthOK = 102, //Validation de l'authentification

				IDChatInvalide = 110, //Impossible d'écrire dans ce chat

				RetourIDChat = 111, //Retourne l'ID du chat voulu, suivi

				ContactInvalide = 112 //Erreur : le destinataire voulu n'existe pas, suivi du pseudo invalide

			;

	private Conversation[] m_conv;
	private int m_nbConv;
	private InetAddress m_ia;
	private int m_port;
	private DatagramSocket m_socket;
	private boolean m_continuer;
	private boolean m_connecte;
	private ArrayList<User> m_users;
	protected Fenetre m_fenetre;
	protected ThreadRecep m_thread;

	//Constructor

	public Client (String serveur)
	{
		m_continuer = true;
		m_connecte = false;
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
		m_fenetre = new Fenetre(this);
		m_users = new ArrayList<User>(128);
		m_thread = new ThreadRecep(m_socket, m_ia, m_port);
	}

	public void run()
	{
		DatagramPacket dp =null;
		while(m_connecte == false)
		{
			DemandeAuth();
			dp = Reception();

			if(dp != null)
				Traitement(dp);
			else
				System.out.println("Erreur d'authentification.Veuillez recommencer \n\n");
		}
		if(m_connecte)
			System.out.println("Vous etes connectés");
		m_thread.start();

		while(m_continuer)
			Action();

		m_socket.close();
		System.out.println("Client ferme.");


	}

	protected void Action()
	{

		EnvoyerMessage();

		while(m_thread.m_msg.size() > 0)
			Traitement(m_thread.m_msg.remove(0));
	}

	//Destructor
	public void CloseClient()
	{
		send(new byte[]  {(byte) Exit});
		m_thread.Stop();
		m_continuer = false;

	}

	//Envoi d'un DTG jusqu'à 3 fois, test de réception du ACK
    public boolean send(byte[] tab)
    {
    	DatagramPacket dp = new DatagramPacket(tab, tab.length, m_ia, m_port);
    	try
    	{
        	//On s'assure de ne pas échanger une suite interminable de ACK avec le client
    		if(tab[0] != ACK)
    		{
    			int i;
	    		//On essaye d'envoyer le DTG jusqu'à 3 fois si on ne reçoit aucun ACK
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

    //Retourne "true" à la la réception du ACK correspondant au numéro du DTG,
    //		   "false" si on passe par un Timeout
    public boolean ACK()
    {
    	DatagramPacket packet;
    	try
    	{
    		m_socket.setSoTimeout(500);
    		//On reçoit jusqu'à recevoir un ACK pour le DTG voulu ou avoir un Timeout
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
    		//On attend jusqu'à 3 secondes pour recevoir une réponse, si on ne reçoit qu'un ACK, on recommence
    		do
    		{
	    		m_socket.setSoTimeout(3000);
	    		packet = new DatagramPacket(new byte[512], 512);
	    		m_socket.receive(packet);
	    		if(packet.getData()[0] == ACK) System.out.println("ACK recu");
    		}while(packet.getData()[0] == ACK);
    		m_port = packet.getPort();
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
    	if(dp != null)
    	{

	    	byte[] data = dp.getData();


	    	byte dpFlag= data[0];
	    	switch(dpFlag)
	    	{
	    		case RetourIDChat:
	    			NouvelleConversation(data);

	    			break;

	    		case PseudoInvalide:
	    			m_fenetre.Texte("Pseudo Invalide");
	    			DemandeAuth();
	    			break;

	    		case Refuse:
	    			break;

	    		case TestConnexion:
	    			ConfirmerActivite();
	    			break;

	    		case NeedAuth:
	    			m_fenetre.Texte("Vous n'êtes pas authentifiés");
	    			DemandeAuth();
	    			break;

	    		case AuthOK:
	    			m_connecte = true;
	    			m_fenetre.Texte("Authentification reussie");
	    			//DemandeConv();
	    			break;

	    		case IDChatInvalide:
	    			m_fenetre.Texte("serveur complet");
	    			break;

	    		case ContactInvalide:
	    			m_fenetre.Texte("Contact Invalide");
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
	    			m_fenetre.Texte("Les utilisateurs sont reçus");
	    			RentreUtilisateurs(data);
	    			break;

	    		case NewUse:
	    			RentreUtilisateurs(data);
	    			break;

	    		case DelUse:
	    			SuppUtilisateur(data[1]);
	    			break;

	    		case NewChat:
	    			DemandeConv();
	    			break;

	    		case MsgOK:
	    			break;

	    		case MsgErr:
	    			m_fenetre.Texte("Votre messsage n'a pas été envoyé");
	    			break;

	    	}
    	}
    }



    private void AfficherListeUsr()
    {
    	System.out.println("\nListe des utilisateurs :");
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
    			System.out.println("L'utilisateur n'a pas été trouvé dans notre liste");
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

    //Demande du pseudo à l'utilisateur
    public String RentrerPseudo()
    {
    	String pseudo= "";
    	boolean contientEspace;

    	do
		{
    		contientEspace = false;
    		//System.out.println("Rentrez votre pseudo");
    		m_fenetre.Label("Rentrez Pseudo");
    		while(m_fenetre.m_str.size() == 0)
    		{
    			try
    			{
    				Thread.sleep(100);
    			}
    			catch(Exception e)
    			{
    				e.printStackTrace();
    			}
    		}
    		pseudo = m_fenetre.m_str.remove(0);
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


    	return pseudo;
    }

    //Demande authentification au serveur
    public void DemandeAuth()
    {
    	String pseudo = RentrerPseudo();

    	ByteBuffer bbuff = ByteBuffer.allocate(512);
    	bbuff.put((byte)TestAuth).put(pseudo.getBytes());

    	send(bbuff.array());

    }

    //Demande au serveur l'acces a une conversation
    public void DemandeConv()
    {
    	boolean entier= true;
    	int j = 0;
    	ByteBuffer bbuff = ByteBuffer.allocate(512);
		bbuff.put((byte) NewChat);
    	String temp ="0";
    	int i = 1;
    	System.out.println("Voici les utilisateurs connectés:");
    	AfficherListeUsr();
    	while (!temp.isEmpty())
		{
    		temp="";
    		System.out.println("\nSaisissez le "+ i + " ème destinataire \nAppuyer sur entrer si vous avez fini. \n");
    		//temp = m_sc.nextLine();

    		//A FAIRE

    		//
    		while(j < temp.length() && entier)
    		{
    			if(temp.charAt(j) >'9' && temp.charAt(j) < '0')
    			{
    				entier = false;
    			}
    		}
    		if(entier)
    		{
    			bbuff.put((byte)Integer.parseInt(temp));
    			i++;
    		}
    		else
    		{
    			System.out.println("Vous n'avez pas rentré un entier");
    		}

		}

    	send(bbuff.array());

    }


    //affichage du message et du propriétaire

    public void AfficherMessage(byte[] data)
    {
    	/*
    	String name = TrouveUser(data[1]).getUsername();
    	String str = (name + " : " + Arrays.copyOfRange(data, 3, data.length).toString());
    	*/
    	String str = new String(Arrays.copyOfRange(data, 2, data.length));
    	System.out.println("AffMsg " + str);
    	m_fenetre.Msg(str);

    }

    //Envoyer un message
    public void EnvoyerMessage()
    {
    	byte section = 0;
    	ByteBuffer bbuff = ByteBuffer.allocate(512);
		bbuff.put((byte)NewMsg);
		bbuff.put((byte) 0).slice();
    	String tempMessage ="0";
    	m_fenetre.Label("Saisissez votre message.");
		while(m_fenetre.m_str.size() != 0)
		{
			tempMessage = m_fenetre.m_str.remove(0);
			bbuff.put(tempMessage.getBytes());
			bbuff.put(section);

			if(tempMessage.equals("quitter"))
				CloseClient();
			else
				send(bbuff.array());
		}

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

  //Repondre à la demande d'activité du serveur
    public void ConfirmerActivite()
    {
    	ByteBuffer bbuff = ByteBuffer.allocate(512)
    			.put((byte) Online);
    	send(bbuff.array());

    }

    //Choisir entre chat global et discussion
    public int choix()
    {

    	String tempLine= "";
    	while(true)
    	{
    		System.out.println("Donnez le numero correspondant à votre attente:\n1:Chat avec tout les utilisateurs\2:CHat avec certains utilisateurs");

    		//tempLine = m_sc.nextLine();

    		//A FAIRE

    		if(tempLine.equals("1"))
    			return 1;
    		else
    			if(tempLine.equals("2"))
    			return 2;

    	}

    }
    //Getter Setter

    //recupère l'index de la conversation avec l'id byte
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
