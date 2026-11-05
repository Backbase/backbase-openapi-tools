package com.backbase.oss.boat.example.orders;

import com.backbase.oss.boat.example.orders.api.OrdersApi;
import com.backbase.oss.boat.example.orders.events.OrdersHandler;
import com.backbase.oss.boat.example.orders.events.OrdersPublisher;
import com.backbase.oss.boat.example.orders.events.PaymentsHandler;
import com.backbase.oss.boat.example.orders.events.model.OrderStatusChangedPayload;
import com.backbase.oss.boat.example.orders.events.model.OrderValidatedPayload;
import com.backbase.oss.boat.example.orders.events.model.PaymentSettledPayload;
import com.backbase.oss.boat.example.orders.model.Order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class OrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }

    /**
     * The REST request body is the payload of the event; the events model package has no copy of it.
     */
    @RestController
    public static class OrdersController implements OrdersApi {

        private final OrdersPublisher publisher;

        public OrdersController(OrdersPublisher publisher) {
            this.publisher = publisher;
        }

        @Override
        public ResponseEntity<Order> getOrder(String orderId) {
            return ResponseEntity.notFound().build();
        }

        @Override
        public ResponseEntity<Order> placeOrder(Order order) {
            publisher.publishOrderPlaced(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        }
    }

    @Component
    public static class OrderEvents implements OrdersHandler, PaymentsHandler {

        @Override
        public void onOrderStatusChanged(OrderStatusChangedPayload payload, MessageHeaders headers) {
            // no-op
        }

        @Override
        public OrderValidatedPayload validateOrder(Order payload, MessageHeaders headers) {
            return new OrderValidatedPayload().orderId(payload.getId()).valid(true);
        }

        @Override
        public void onPaymentSettled(PaymentSettledPayload payload, MessageHeaders headers) {
            // no-op
        }
    }
}
