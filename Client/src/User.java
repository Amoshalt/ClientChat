
public class User {
	
	private byte m_id;
	private String m_username;
	
	public User()
	{
		m_id = 0;
		m_username = "";
	}
	
	public User(byte id, String username)
	{
		m_id = id;
		m_username = username;
	}

	public byte getId()
	{
		return m_id;
	}
	
	public String getUsername()
	{
		return m_username;
	}
}
