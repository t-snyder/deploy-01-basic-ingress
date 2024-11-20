package models;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import exceptions.ServerConfigException;

/**
 * This class is a utility
 */
public class CryptoMaterial
{
  private static final int MIN_PATH_LEN = 5;
  private static final int MIN_PWD_LEN  = 5;

  private byte[] caCert         = null;
  private byte[] tlsCert        = null;
  private byte[] tlsPrivateKey  = null;
  private String keyStorePwd    = null;
  private String serverHostName = null;

  public CryptoMaterial( byte[] caCert, byte[] tlsCert, byte[] privateKey, String keyStorePwd, String serverHostName )
  {
     this.caCert         = caCert;
     this.tlsCert        = tlsCert;
     this.tlsPrivateKey  = privateKey;
     this.keyStorePwd    = keyStorePwd;
     this.serverHostName = serverHostName;
  }

  /**
   * Note - Environmental variable names need to be the same as provided within the tls_server.yaml
   *
   * @return
   */
  public static CryptoMaterial getCryptoInfo()
   throws ServerConfigException
  {
    String caCert         = System.getenv( "SERVER_TLS_CERT"        );
    String tlsCert        = System.getenv( "TLS_ROOTCERT"           );
    String tlsPrivateKey  = System.getenv( "SERVER_TLS_PRIVATE_KEY" );
    String keyStorePwd    = System.getenv( "KEYSTORE_PWD"           );
    String serverHostName = System.getenv( "SERVER_HOST_NAME"       );

    if( caCert == null )
      throw new ServerConfigException( "System configuration error - Could not obtain caCert" );

    if( tlsCert == null )
      throw new ServerConfigException( "System configuration error - Could not obtain tlsCert" );

    if( tlsPrivateKey == null )
      throw new ServerConfigException( "System configuration error - Could not obtain tlsPrivateKeyPath" );

    if( keyStorePwd == null || keyStorePwd.length() < MIN_PWD_LEN )
      throw new ServerConfigException( "System configuration error - Could not obtain keyStorePwd" );

    if( serverHostName == null || serverHostName.length() < MIN_PATH_LEN )
      throw new ServerConfigException( "System configuration error - Could not obtain serverHostName" );

    return new CryptoMaterial( caCert.getBytes(), tlsCert.getBytes(), tlsPrivateKey.getBytes(), keyStorePwd, serverHostName );
  }

  /**
   * Note - Environmental variable names need to be the same as provided within the tls_server.yaml
   *
   * @return
   */
  public static CryptoMaterial getCryptoIFile()
   throws ServerConfigException
  {
    String caCert         = System.getenv( "SERVER_TLS_CERTFILE"        );
    String tlsCert        = System.getenv( "TLS_ROOTCERT_FILE"          );
    String tlsPrivateKey  = System.getenv( "SERVER_TLS_PRIVATE_KEYFILE" );
    String keyStorePwd    = System.getenv( "KEYSTORE_PWD"               );
    String serverHostName = System.getenv( "SERVER_HOST_NAME"           );

    if( caCert == null )
      throw new ServerConfigException( "System configuration error - Could not obtain caCert" );

    if( tlsCert == null )
      throw new ServerConfigException( "System configuration error - Could not obtain tlsCert" );

    if( tlsPrivateKey == null )
      throw new ServerConfigException( "System configuration error - Could not obtain tlsPrivateKeyPath" );

    if( keyStorePwd == null || keyStorePwd.length() < MIN_PWD_LEN )
      throw new ServerConfigException( "System configuration error - Could not obtain keyStorePwd" );

    if( serverHostName == null || serverHostName.length() < MIN_PATH_LEN )
      throw new ServerConfigException( "System configuration error - Could not obtain serverHostName" );

    return new CryptoMaterial( caCert.getBytes(), tlsCert.getBytes(), tlsPrivateKey.getBytes(), keyStorePwd, serverHostName );
  }

  public static CryptoMaterial getCryptoResources()
   throws IOException
  {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream isCa        = classloader.getResourceAsStream("ca.crt");
    InputStream isTLS       = classloader.getResourceAsStream("tls.crt");
    InputStream isKey       = classloader.getResourceAsStream("tls.key");
    
    return new CryptoMaterial( isCa.readAllBytes(), isTLS.readAllBytes(), isKey.readAllBytes(), "1234567", "pserver.foo.com" );
  }
  
  public byte[] getCaCert()         { return caCert;         }
  public byte[] getTlsCert()        { return tlsCert;        }
  public byte[] getTlsPrivateKey()  { return tlsPrivateKey;  }
  public String getKeyStorePwd()    { return keyStorePwd;    }
  public String getServerHostName() { return serverHostName; }
}
