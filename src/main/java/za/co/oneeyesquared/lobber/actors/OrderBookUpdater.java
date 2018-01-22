package za.co.oneeyesquared.lobber.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import za.co.oneeyesquared.lobber.models.LimitOrderBookMessage;
import za.co.oneeyesquared.lobber.models.OrderBookUpdateMessage;
import za.co.oneeyesquared.lobber.models.UpdateCompletedMessage;

import java.util.HashMap;

/**
 * Created by Balitha on 2017-11-22.
 */
public class OrderBookUpdater  extends AbstractActor{
    static public Props props() {
        return Props.create(OrderBookUpdater.class, () -> new OrderBookUpdater());
    }

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private HashMap<Long, OrderBookUpdateMessage> unappliedUpdates;
    private ActorRef limitOrderBookActor;
    public OrderBookUpdater() {
        unappliedUpdates = new HashMap<>();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(LimitOrderBookMessage.class, limitOrderBook -> {
                    log.info("Order Limit Book Received: " + limitOrderBook.getStartingSequence());
                    log.debug(limitOrderBook.toString());
//                    this.limitOrderBook = limitOrderBook;
                    limitOrderBookActor = getContext().getSystem().actorOf(Props.create(LimitOrderBook.class), "LimitOrderBook");
                    limitOrderBookActor.tell(limitOrderBook, getSelf());
                    if(!unappliedUpdates.isEmpty()) {
                        log.debug("The order book has {0} outstanding updates", unappliedUpdates.keySet().size());
                    }
                })
                .match(OrderBookUpdateMessage.class, orderBookUpdateMessage -> {
                    log.info("Order book update received: " + orderBookUpdateMessage.getSequence());
                    log.debug(orderBookUpdateMessage.toString());
                    if(this.limitOrderBookActor == null)
                    {
                        if(unappliedUpdates.size() > 10)
                        {
                            System.exit(9);
                        }

                        log.debug("No order book received yet.");


                        unappliedUpdates.put(orderBookUpdateMessage.getSequence(), orderBookUpdateMessage);
                    }else{
                        limitOrderBookActor.tell(orderBookUpdateMessage, getSelf());
                    }


                })
                .match(UpdateCompletedMessage.class, updateMessage -> {

                    log.info("Completed applying: " + updateMessage.getSequenceID());

                    unappliedUpdates.remove(updateMessage.getSequenceID());
                    if(unappliedUpdates.get(updateMessage.getSequenceID()+1) != null)
                        limitOrderBookActor.tell(unappliedUpdates.get(updateMessage.getSequenceID()+1), getSelf());

                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

}
