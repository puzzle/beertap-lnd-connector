package ch.puzzle.lightning.minizeus.invoices.boundary;


import ch.puzzle.lightning.minizeus.invoices.entity.Invoice;
import ch.puzzle.lightning.minizeus.invoices.entity.InvoiceSettledEvent;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;


@Path("invoice")
@ApplicationScoped
public class InvoiceResource {

    @Inject
    LndService lndService;

    @Context
    private Sse sse;

    private volatile SseBroadcaster sseBroadcaster;
    private OutboundSseEvent.Builder eventBuilder;

    @PostConstruct
    public void init(){
        this.sseBroadcaster = sse.newBroadcaster();
        this.eventBuilder = sse.newEventBuilder();
        sseBroadcaster.onClose(sseEventSink -> System.out.println("subscription closed"));
    }

    @GET
    @Produces("application/json")
    public Invoice getInvoice(
            @QueryParam("amount") @DefaultValue("1") int amount,
            @QueryParam("memo") @DefaultValue("default") String memo) {
        return lndService.generateInvoice(amount, memo);
    }

    @GET
    @Path("subscribe")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@Context SseEventSink sseEventSink) {
        System.out.println("new subscription");
        this.sseBroadcaster.register(sseEventSink);
    }

    public void sendMessage(@ObservesAsync InvoiceSettledEvent value) {
        OutboundSseEvent sseEvent = eventBuilder.name("invoice")
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .data(Invoice.class, value.invoice)
                .build();
        this.sseBroadcaster.broadcast(sseEvent);
    }
}