package za.co.oneeyesquared.lobber;

import akka.actor.ActorSystem;
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
import za.co.oneeyesquared.lobber.models.LimitOrderBook;
import za.co.oneeyesquared.lobber.models.OrderBookUpdateMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AkkaQuickstart {
    static int messagesParsed = 0;
  public static void main(String[] args) {
    final ActorSystem system = ActorSystem.create("za-co-lobber-readers-luno");
      Materializer materializer = ActorMaterializer.create(system);
      Http http = Http.get(system);
      Config appConfig = ConfigFactory.load();


        String apiAuth = "{\"api_key_id\":\""+appConfig.getString("LUNO_KEY_ID")+"\",\"api_key_secret\": \"" + appConfig.getString("LUNO_SECRET") +"\"}";
        String wsUrl = "wss://ws.luno.com/api/1/stream/XBTZAR";


      final Source<Message, CompletableFuture<Optional<Message>>> source =
              Source.from(Arrays.<Message>asList(TextMessage.create(apiAuth)))
                      .concatMat(Source.maybe(), Keep.right());

      final Flow<Message, Message, CompletableFuture<Optional<Message>>> flow =
              Flow.fromSinkAndSourceMat(
                      Sink.foreach((message) ->{

                          if(message.asTextMessage().isStrict())
                              doSomething(message.asTextMessage().getStrictText(), materializer);

                          else
                          {
                              final CompletionStage<List<String>> strings = message.asTextMessage().getStreamedText()
                                      .runWith(Sink.seq(), materializer);

                              strings.thenAcceptAsync(c -> doSomething(String.join("", c), materializer), system.dispatcher());

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

    private static void doSomething(String message, Materializer mat){

        if(messagesParsed++ == 0)
        {
            //this is the initial order book state message
            Unmarshaller<ByteString, LimitOrderBook> unmarshaller = Jackson.byteStringUnmarshaller(LimitOrderBook.class);

            final CompletionStage<LimitOrderBook> serialised = unmarshaller.unmarshal(ByteString.fromString(message), mat);
            serialised.thenAcceptAsync(orderbook -> System.out.println(orderbook));

        }
        if(messagesParsed > 0) {
            Unmarshaller<ByteString, OrderBookUpdateMessage> unmarshaller = Jackson.byteStringUnmarshaller(OrderBookUpdateMessage.class);

            final CompletionStage<OrderBookUpdateMessage> serialised = unmarshaller.unmarshal(ByteString.fromString(message), mat);
            serialised.thenAcceptAsync(order -> System.out.println(order));
        }

    }

}
