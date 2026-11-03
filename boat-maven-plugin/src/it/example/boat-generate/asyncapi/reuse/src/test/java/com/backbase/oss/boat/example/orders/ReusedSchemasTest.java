package com.backbase.oss.boat.example.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.backbase.oss.boat.example.orders.events.model.OrderStatusChangedPayload;
import com.backbase.oss.boat.example.orders.model.Money;
import com.backbase.oss.boat.example.orders.model.Order;
import com.backbase.oss.boat.example.orders.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
class ReusedSchemasTest {

    private static final String EVENTS_MODEL = "com.backbase.oss.boat.example.orders.events.model.";

    @Autowired
    private OrdersApplication.OrdersController controller;

    @Autowired
    private OutputDestination output;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sharedSchemasAreNotGeneratedAgain() {
        for (String shared : new String[]{"Order", "Money", "OrderStatus"}) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(EVENTS_MODEL + shared));
        }
        assertEquals(OrderStatus.class, new OrderStatusChangedPayload().status(OrderStatus.SHIPPED).getStatus().getClass());
    }

    @Test
    void publishesTheRestModel() throws Exception {
        controller.placeOrder(new Order("order-1", new Money().amount("1.00").currencyCode("EUR"), OrderStatus.PLACED));

        Message<byte[]> message = output.receive(5000, "orders.placed");

        assertNotNull(message);
        assertEquals("order-1", objectMapper.readValue(message.getPayload(), Order.class).getId());
    }
}
