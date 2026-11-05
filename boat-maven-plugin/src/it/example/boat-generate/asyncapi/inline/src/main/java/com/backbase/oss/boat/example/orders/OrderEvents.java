package com.backbase.oss.boat.example.orders;

import com.backbase.oss.boat.example.orders.events.OrdersHandler;
import com.backbase.oss.boat.example.orders.events.PaymentsHandler;
import com.backbase.oss.boat.example.orders.model.Order;
import com.backbase.oss.boat.example.orders.model.OrderStatusChangedPayload;
import com.backbase.oss.boat.example.orders.model.OrderValidatedPayload;
import com.backbase.oss.boat.example.orders.model.PaymentSettledPayload;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
public class OrderEvents implements OrdersHandler, PaymentsHandler {

    private final List<Object> received = new CopyOnWriteArrayList<>();

    public List<Object> getReceived() {
        return received;
    }

    @Override
    public void onOrderStatusChanged(OrderStatusChangedPayload payload, MessageHeaders headers) {
        received.add(payload);
    }

    @Override
    public OrderValidatedPayload validateOrder(Order payload, MessageHeaders headers) {
        received.add(payload);
        boolean valid = payload.getTotal() != null && !payload.getTotal().getAmount().startsWith("-");
        OrderValidatedPayload reply = new OrderValidatedPayload().orderId(payload.getId()).valid(valid);
        return valid ? reply : reply.addReasonsItem("Negative total");
    }

    @Override
    public void onPaymentSettled(PaymentSettledPayload payload, MessageHeaders headers) {
        received.add(payload);
    }
}
