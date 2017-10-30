package com.lightbend.akka.sample;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.lightbend.akka.sample.Greeter.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AkkaQuickstart {
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
                          if(message.isText())
                              doSomething("Got TEXT message");
                          else
                              doSomething("Got non TEXT message");
                          if(message.asTextMessage().isStrict())
                              doSomething("Got message: " + message.asTextMessage().getStrictText());

                          else
                          {
                              doSomething("Got streamed message");
                              final CompletionStage<List<String>> strings = message.asTextMessage().getStreamedText()
                                      .runWith(Sink.seq(), materializer);

                              strings.thenAcceptAsync(c -> doSomething(String.join("", c)), system.dispatcher());

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

    private static void doSomething(String message){
        System.out.println("MSG" + message);
    }
}
