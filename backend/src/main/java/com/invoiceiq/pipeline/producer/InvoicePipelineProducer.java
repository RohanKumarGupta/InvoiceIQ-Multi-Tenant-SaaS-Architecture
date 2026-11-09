package com.invoiceiq.pipeline.producer;

import com.invoiceiq.pipeline.config.RabbitMqConfig;
import com.invoiceiq.pipeline.event.InvoiceUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePipelineProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqConfig rabbitMqConfig;

    public void publishInvoiceUploaded(InvoiceUploadedEvent event) {
        log.info("Publishing InvoiceUploadedEvent for invoiceId: {}, tenantId: {} to exchange: {}, routingKey: {}",
                event.invoiceId(), event.tenantId(), rabbitMqConfig.getExchange(), rabbitMqConfig.getRoutingKey());

        rabbitTemplate.convertAndSend(
                rabbitMqConfig.getExchange(),
                rabbitMqConfig.getRoutingKey(),
                event
        );
    }
}
