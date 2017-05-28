import java.awt.ScrollPane;
import java.awt.TextField;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Fenetre extends JFrame
{
	protected JTextField m_texte;
	protected MBouton m_valider;
	protected JLabel m_label, m_msg;
	protected Client m_client;
	protected ScrollPane m_scroll;
	public ArrayList<String> m_str;
	protected Box m_v;
	protected JPanel m_p;

	public Fenetre(Client c)
	{
		m_client = c;
	    this.setTitle("Client Chat");
	    this.setSize(800, 500);
	    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    this.setLocationRelativeTo(null);
	    this.m_valider = new MBouton(this);
	    m_texte = new JTextField();
	    m_label = new JLabel();
	    m_msg = new JLabel("");
	    Box b0 = Box.createHorizontalBox();
	    Box b1 = Box.createVerticalBox();
	    Box b2 = Box.createVerticalBox();
	    m_p = new JPanel();
	    m_p.setSize(200, 200);
	    b1.add(m_label);
	    b1.add(m_texte);
	    b1.add(m_valider);
	    b1.add(m_msg);
	    m_p.add(b1);

	    m_scroll = new ScrollPane();
	    m_scroll.setSize(200, 200);
	    m_v = Box.createVerticalBox();
	    m_v.add(new JLabel("Chat global"));
	    m_scroll.add(m_v);
	    b2.add(m_scroll);

	    b0.add(b2);
	    b0.add(m_p);

	    getContentPane().add(b0);
	    this.setVisible(true);
	    m_str = new ArrayList<String>();
  	}

	public void Label(String txt)
	{
		m_label.setText(txt);
	}

	public void Texte(String texte)
	{
		m_msg.setText(texte);
		m_p.repaint();
	}

	public void Ajout()
	{
		m_str.add(m_texte.getText());
		m_texte.setText("");
	}

	public void Msg(String str)
	{
		m_v.add(new JLabel(str));
	}
}