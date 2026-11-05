package com.backbase.oss.boat.example.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.boat.example.orders.model.Money;
import com.backbase.oss.boat.example.orders.model.Order;
import com.backbase.oss.boat.example.orders.model.OrderStatus;
import com.backbase.oss.boat.example.orders.model.OrderValidatedPayload;
import com.backbase.oss.boat.example.orders.model.PaymentSettledPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
class OrderEventsTest {

    @Autowired
    private OrdersController controller;

    @Autowired
    private OrderEvents events;

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publishesPlacedOrders() throws Exception {
        controller.placeOrder(order("order-1", "10.00"));

        Message<byte[]> message = output.receive(5000, "orders.placed");

        assertNotNull(message);
        assertEquals("order-1", objectMapper.readValue(message.getPayload(), Order.class).getId());
    }

    @Test
    void receivesSettledPayments() throws Exception {
        PaymentSettledPayload payment = new PaymentSettledPayload("order-2", money("10.00"));

        input.send(MessageBuilder.withPayload(objectMapper.writeValueAsBytes(payment)).build(), "payments.settled");

        assertTrue(events.getReceived().stream()
            .anyMatch(received -> received instanceof PaymentSettledPayload
                && "order-2".equals(((PaymentSettledPayload) received).getOrderId())));
    }

    @Test
    void repliesToValidationRequests() throws Exception {
        Message<byte[]> request = MessageBuilder.withPayload(objectMapper.writeValueAsBytes(order("order-3", "-1.00")))
            .setHeader("replyTo", "orders.validation.replies")
            .build();

        input.send(request, "orders.validation.requests");
        Message<byte[]> reply = output.receive(5000, "orders.validation.replies");

        assertNotNull(reply);
        OrderValidatedPayload validated = objectMapper.readValue(reply.getPayload(), OrderValidatedPayload.class);
        assertEquals("order-3", validated.getOrderId());
        assertFalse(validated.getValid());
    }

    private static Order order(String id, String amount) {
        return new Order(id, money(amount), OrderStatus.PLACED);
    }

    private static Money money(String amount) {
        return new Money().amount(amount).currencyCode("EUR");
    }
}
