package server;

import helpers.SSLContextUtil;
import models.CryptoMaterial;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.concurrent.CompletionStage;

import javax.net.ssl.SSLContext;

import org.apache.pekko.http.javadsl.ConnectionContext;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.HttpsConnectionContext;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;
import org.apache.pekko.http.javadsl.ServerBinding;

import org.apache.pekko.actor.typed.ActorSystem;

import org.apache.pekko.actor.typed.javadsl.Behaviors;

import exceptions.ServerConfigException;


/**
 * 
 */
public class PapayaServer extends AllDirectives
{
  private static final boolean USE_TLS = true;

//  public static Route route = get( () -> path(segment("papaya"), () -> complete( "Papaya is a sweet fruit" )));

  private Route createRoute() 
  {
	return concat(path("papaya", () -> get(() -> complete("Pekko-http says that Papaya is a sweet fruit"))));
  }
 
  public void startHttpServer()
   throws ServerConfigException, IOException
  {
    //CryptoMaterial cryptoInfo = CryptoMaterial.getCryptoInfo();
	CryptoMaterial cryptoInfo = null;
	  
    // For use if testing as java app on local machine. ie. running from Eclipse
    //    CryptoMaterial cryptoInfo = CryptoMaterial.getCryptoResources();

    ActorSystem<Void> system = ActorSystem.create( Behaviors.empty(), "routes" );

    final Http http = Http.get(system);
    
    if( USE_TLS )
    {
   	
      CompletionStage<ServerBinding> futureBinding = http.newServerAt( "0.0.0.0", 9443 )
                                                         .enableHttps( createHttpsContext( system, cryptoInfo ))
                                                         .bind( createRoute() );

      futureBinding.whenComplete( ( binding, exception ) ->
                                  {
                                    if( binding != null )
                                    {
                                      InetSocketAddress address = binding.localAddress();
                                      system.log().info( "Server online at https://{}:{}/", address.getHostString(), address.getPort() );
                                    } else
                                    {
                                      system.log().error( "Failed to bind HTTPS endpoint, terminating system", exception );
                                      system.terminate();
                                    }
                                  } );
    } 
    else
    {
      CompletionStage<ServerBinding> futureBinding = http.newServerAt( "0.0.0.0", 8080 ).bind( createRoute() );

      futureBinding.whenComplete( ( binding, exception ) ->
                                  {
                                    if( binding != null )
                                    {
                                      InetSocketAddress address = binding.localAddress();
                                      system.log().info( "\nServer online at http://{}:{}/", address.getHostString(), address.getPort() );
                                    } else
                                    {
                                      system.log().error( "Failed to bind HTTP endpoint, terminating system", exception );
                                      system.terminate();
                                    }
                                  } );
    };
  };

  private static HttpsConnectionContext createHttpsContext( ActorSystem<?> system, CryptoMaterial crypto )
  {
    boolean serverHostnameVerification = false;

    try
    {
      SSLContext sslContext = SSLContextUtil.getSSLContext( crypto.getCaCert(), 
    		                                                crypto.getTlsCert(), 
    		                                                crypto.getTlsPrivateKey(), 
    		                                                crypto.getKeyStorePwd(), 
    		                                                serverHostnameVerification );

      return ConnectionContext.httpsServer( sslContext );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  };

  public static void main( String[] args )
          throws Exception
  {
    PapayaServer svr = new PapayaServer();
    
    svr.startHttpServer();
  }
}
