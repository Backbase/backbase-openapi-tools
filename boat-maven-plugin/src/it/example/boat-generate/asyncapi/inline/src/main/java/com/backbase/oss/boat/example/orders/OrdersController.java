package com.backbase.oss.boat.example.orders;

import com.backbase.oss.boat.example.orders.api.OrdersApi;
import com.backbase.oss.boat.example.orders.events.OrdersPublisher;
import com.backbase.oss.boat.example.orders.model.Order;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST request body and the event payload are the same {@link Order} class.
 */
@RestController
public class OrdersController implements OrdersApi {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final OrdersPublisher publisher;

    public OrdersController(OrdersPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public ResponseEntity<Order> getOrder(String orderId) {
        Order order = orders.get(orderId);
        return order == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(order);
    }

    @Override
    public ResponseEntity<Order> placeOrder(Order order) {
        orders.put(order.getId(), order);
        publisher.publishOrderPlaced(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
