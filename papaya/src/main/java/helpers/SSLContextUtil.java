package helpers;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLContextUtil
{
  /**
   * Create an SslContext using PEM encrypted certificate files. Mutual SSL
   * Authentication is supported.
   *
   * @param caCrtFile                  CA certificate of remote server.
   * @param crtFile                    certificate file of client.
   * @param keyFile                    key file of client.
   * @param password                   password of key file.
   * @param serverHostnameVerification Enable/disable verification of server certificate DNS and hostname.
   * @return
   * @throws CertificateException
   * @throws IOException
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   * @throws UnrecoverableKeyException
   */
  public static SSLContext getSSLContext( final byte[] caCrt, final byte[] tlsCrt,
                                          final byte[] key,   final String password,
                                          boolean serverHostnameVerification )
    throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException
  {
    Security.addProvider( new BouncyCastleProvider() );
    JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");

    X509CertificateHolder caCertHolder = (X509CertificateHolder) readPEMBytes( caCrt );
    X509Certificate       caCert       = certificateConverter.getCertificate( caCertHolder );
    X509CertificateHolder certHolder   = (X509CertificateHolder) readPEMBytes( tlsCrt );
    X509Certificate       cert         = certificateConverter.getCertificate(certHolder);

    // Private Key
    Object keyObject = readPEMBytes( key );

    JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");
    PrivateKey         privateKey   = null;

    if( keyObject instanceof PEMEncryptedKeyPair )
    {
      PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build( password.toCharArray() );
      KeyPair keyPair = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
      privateKey = keyPair.getPrivate();
    }
    else if( keyObject instanceof PEMKeyPair )
    {
      KeyPair keyPair = keyConverter.getKeyPair( (PEMKeyPair) keyObject );
      privateKey = keyPair.getPrivate();
    }
    else if( keyObject instanceof PrivateKeyInfo )
    {
      privateKey = keyConverter.getPrivateKey( (PrivateKeyInfo) keyObject );
    }
    else
    {
      throw new IOException(String.format( "Unsupported type of keyFile %s", key ));
    }

    // CA certificate is used to authenticate server
    KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    caKeyStore.load(null, null);
    caKeyStore.setCertificateEntry("ca-certificate", caCert);

    /**
     * Client key and certificates are sent to server so it can authenticate the
     * client. (server send CertificateRequest message in TLS handshake step).
     */
    KeyStore clientKeyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
    clientKeyStore.load(null, null);
    clientKeyStore.setCertificateEntry("certificate", cert);
    clientKeyStore.setKeyEntry("private-key", privateKey, password.toCharArray(), new Certificate[] { cert });

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
    keyManagerFactory.init( clientKeyStore, password.toCharArray() );

    /**
     * Create SSL socket factory
     */
    SSLContext context = SSLContext.getInstance("TLS");
    context.init( keyManagerFactory.getKeyManagers(),
                  serverHostnameVerification ? getTrustManagers(caKeyStore) : getUnsafeTrustManagers(caKeyStore), null);

    return context;
  }

  /**
   * Create an SslContext using PEM encrypted certificate files. Mutual SSL
   * Authentication is supported.
   *
   * @param caCrtFile                  CA certificate of remote server.
   * @param crtFile                    certificate file of client.
   * @param keyFile                    key file of client.
   * @param password                   password of key file.
   * @param serverHostnameVerification Enable/disable verification of server certificate DNS and hostname.
   * @return
   * @throws CertificateException
   * @throws IOException
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws KeyManagementException
   * @throws UnrecoverableKeyException
   */
  public static SSLContext getSSLContext( final String caCrtFile, final String crtFile,
                                          final String keyFile,   final String password,
                                          boolean serverHostnameVerification )
    throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException
  {
    Security.addProvider( new BouncyCastleProvider() );

    JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");

    X509CertificateHolder caCertHolder = (X509CertificateHolder) readPEMFile( caCrtFile );
    X509Certificate       caCert       = certificateConverter.getCertificate( caCertHolder );
    X509CertificateHolder certHolder   = (X509CertificateHolder) readPEMFile( crtFile );
    X509Certificate       cert         = certificateConverter.getCertificate(certHolder);

    // Private Key
    Object keyObject = readPEMFile( keyFile );

    JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");
    PrivateKey         privateKey   = null;

    if( keyObject instanceof PEMEncryptedKeyPair )
    {
      PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build( password.toCharArray() );
      KeyPair keyPair = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
      privateKey = keyPair.getPrivate();
    }
    else if( keyObject instanceof PEMKeyPair )
    {
      KeyPair keyPair = keyConverter.getKeyPair( (PEMKeyPair) keyObject );
      privateKey = keyPair.getPrivate();
    }
    else if( keyObject instanceof PrivateKeyInfo )
    {
      privateKey = keyConverter.getPrivateKey( (PrivateKeyInfo) keyObject );
    }
    else
    {
      throw new IOException(String.format( "Unsupported type of keyFile %s", keyFile ));
    }

    // CA certificate is used to authenticate server
    KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    caKeyStore.load(null, null);
    caKeyStore.setCertificateEntry("ca-certificate", caCert);

    /**
     * Client key and certificates are sent to server so it can authenticate the
     * client. (server send CertificateRequest message in TLS handshake step).
     */
    KeyStore clientKeyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
    clientKeyStore.load(null, null);
    clientKeyStore.setCertificateEntry("certificate", cert);
    clientKeyStore.setKeyEntry("private-key", privateKey, password.toCharArray(), new Certificate[] { cert });

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
    keyManagerFactory.init( clientKeyStore, password.toCharArray() );

    /**
     * Create SSL socket factory
     */
    SSLContext context = SSLContext.getInstance("TLS");
    context.init( keyManagerFactory.getKeyManagers(),
                  serverHostnameVerification ? getTrustManagers(caKeyStore) : getUnsafeTrustManagers(caKeyStore), null);

     return context;
  }

  private static Object readPEMFile( String filePath )
   throws IOException
  {
    try( PEMParser reader = new PEMParser( new FileReader( filePath )))
    {
      return reader.readObject();
    }
  }

  private static Object readPEMBytes( byte[] pemBytes )
   throws IOException
  {
    InputStream bStream = new ByteArrayInputStream( pemBytes );
    PEMParser   reader  = new PEMParser( new InputStreamReader( bStream ));	  

    return reader.readObject();
  }

  private static TrustManager[] getTrustManagers( KeyStore caKeyStore )
   throws NoSuchAlgorithmException, KeyStoreException
  {
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
    trustManagerFactory.init(caKeyStore);
    return trustManagerFactory.getTrustManagers();
  }

  /**
   * This method checks server and client certificates but overrides server hostname verification.
   * @param caKeyStore
   * @return
   * @throws NoSuchAlgorithmException
   * @throws KeyStoreException
 ' */
  private static TrustManager[] getUnsafeTrustManagers(KeyStore caKeyStore)
   throws NoSuchAlgorithmException, KeyStoreException
  {
    X509TrustManager standardTrustManager = (X509TrustManager) getTrustManagers(caKeyStore)[0];
    return
      new TrustManager[]
      {
        new X509ExtendedTrustManager()
        {
          @Override
          public void checkClientTrusted(X509Certificate[] chain, String authType)
           throws CertificateException
          {
            standardTrustManager.checkClientTrusted(chain, authType);
          }

          @Override
          public void checkServerTrusted( X509Certificate[] chain, String authType )
           throws CertificateException
          {
            standardTrustManager.checkServerTrusted(chain, authType);
          }

          @Override
          public X509Certificate[] getAcceptedIssuers()
          {
            return standardTrustManager.getAcceptedIssuers();
          }

          @Override
          public void checkClientTrusted( X509Certificate[] chain, String authType, Socket socket )
           throws CertificateException
          {
            standardTrustManager.checkClientTrusted(chain, authType);
          }

          @Override
          public void checkServerTrusted( X509Certificate[] chain, String authType, Socket socket )
           throws CertificateException
          {
            standardTrustManager.checkServerTrusted(chain, authType);
          }

          @Override
          public void checkClientTrusted( X509Certificate[] chain, String authType, SSLEngine engine )
           throws CertificateException
          {
            standardTrustManager.checkClientTrusted(chain, authType);
          }

          @Override
          public void checkServerTrusted( X509Certificate[] chain, String authType, SSLEngine engine )
           throws CertificateException
          {
            standardTrustManager.checkServerTrusted(chain, authType);
          }
      } };
  }
}





