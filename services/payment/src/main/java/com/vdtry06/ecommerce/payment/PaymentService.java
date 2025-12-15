package com.vdtry06.ecommerce.payment;


import com.vdtry06.ecommerce.notification.NotificationProducer;
import com.vdtry06.ecommerce.notification.PaymentNotificationRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService {
    PaymentRepository repository;
    PaymentMapper mapper;
    NotificationProducer notificationProducer;

    public Integer createPayment(PaymentRequest request) {
        var payment = repository.save(mapper.toPayment(request));

        // Send notification only if customer exists
        if (request.customer() != null) {
            notificationProducer.sendNotification(
                    new PaymentNotificationRequest(
                            request.orderReference(),
                            request.amount(),
                            request.paymentMethod(),
                            request.customer().firstName(),
                            request.customer().lastName(),
                            request.customer().email()
                    )
            );
        }

        return payment.getId();
    }
}
