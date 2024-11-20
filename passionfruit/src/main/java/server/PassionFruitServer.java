package server;

import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletionStage;


public class PassionFruitServer extends AllDirectives
{
  public static final int PORT = 9080;
  
  public static void main(String[] args) 
   throws Exception 
  {
    ActorSystem<Void> system = ActorSystem.create( Behaviors.empty(), "routes" );

    final Http http = Http.get(system);

    // In order to access all directives we need an instance where the routes are define.
    PassionFruitServer app = new PassionFruitServer();

    final CompletionStage<ServerBinding> futureBinding =
        http.newServerAt("0.0.0.0", PORT ).bind( app.createRoute() );

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

  private Route createRoute() {
    return concat(path("passion", () -> get(() -> complete("<h1>Pekko-http loves passion fruit</h1>"))));
  }
}
