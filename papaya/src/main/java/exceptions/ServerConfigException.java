package exceptions;

public class ServerConfigException extends Exception
{
  private static final long serialVersionUID = 4757120132833816243L;

  public ServerConfigException( String errMsg )
  {
    super( errMsg );
  }
}
