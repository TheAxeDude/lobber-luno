package za.co.oneeyesquared.lobber;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import za.co.oneeyesquared.lobber.actors.LimitOrderBook;
import za.co.oneeyesquared.lobber.actors.OrderBookUpdater;
import za.co.oneeyesquared.lobber.models.LimitOrderBookMessage;
import za.co.oneeyesquared.lobber.models.OrderBookUpdateMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Lobber extends AbstractActor {
    static int messagesParsed = 0;
    static ActorRef orderBookUpdater;

    static private LoggingAdapter log;
    public static void main(String[] args) {


    final ActorSystem system = ActorSystem.create("za-co-lobber-readers-luno");
      Materializer materializer = ActorMaterializer.create(system);

      Http http = Http.get(system);
      Config appConfig = ConfigFactory.load();
      ActorRef lobber = system.actorOf(Props.create(Lobber.class), "Lobber-main-class");
      orderBookUpdater = system.actorOf(Props.create(OrderBookUpdater.class), "OrderBook");


      String apiAuth = "{\"api_key_id\":\""+appConfig.getString("LUNO_KEY_ID")+"\",\"api_key_secret\": \"" + appConfig.getString("LUNO_SECRET") +"\"}";
//      String wsUrl = "wss://ws.luno.com/api/1/stream/ETHXBT";
      String wsUrl = "wss://ws.luno.com/api/1/stream/XBTZAR";

      final Source<Message, CompletableFuture<Optional<Message>>> source =
              Source.from(Arrays.<Message>asList(TextMessage.create(apiAuth)))
                      .concatMat(Source.maybe(), Keep.right());

      final Flow<Message, Message, CompletableFuture<Optional<Message>>> flow =
              Flow.fromSinkAndSourceMat(
                      Sink.foreach((message) ->{

                          if(message.asTextMessage().isStrict())
                              doSomething(message.asTextMessage().getStrictText(), materializer, lobber);

                          else
                          {
                              final CompletionStage<List<String>> strings = message.asTextMessage().getStreamedText()
                                      .runWith(Sink.seq(), materializer);

                              strings.thenAcceptAsync(c -> doSomething(String.join("", c), materializer, lobber), system.dispatcher());

                          }
                      }),
                      source,
                      Keep.right());



      final Pair<CompletionStage<WebSocketUpgradeResponse>, CompletableFuture<Optional<Message>>> pair =
              http.singleWebSocketRequest(
                      WebSocketRequest.create(wsUrl),
                      flow,
                      materializer);

  }

    private static void doSomething(String message, Materializer mat, ActorRef actorRef){

        if(messagesParsed++ == 0)
        {
            //this is the initial order book state message
            Unmarshaller<ByteString, LimitOrderBookMessage> unmarshaller = Jackson.byteStringUnmarshaller(LimitOrderBookMessage.class);

            final CompletionStage<LimitOrderBookMessage> serialised = unmarshaller.unmarshal(ByteString.fromString(message), mat);


            serialised.thenAcceptAsync(orderbook -> {
                System.out.println("Got the limit order book from luno: " + orderbook);
                orderBookUpdater.tell(orderbook, actorRef);
            });

        }
        if(messagesParsed > 0) {

            Unmarshaller<ByteString, OrderBookUpdateMessage> unmarshaller = Jackson.byteStringUnmarshaller(OrderBookUpdateMessage.class);
            System.out.println("Got the update from luno:" + message);
            final CompletionStage<OrderBookUpdateMessage> serialised = unmarshaller.unmarshal(ByteString.fromString(message), mat);
            serialised.thenAcceptAsync(order -> orderBookUpdater.tell(order, actorRef));
        }

    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
