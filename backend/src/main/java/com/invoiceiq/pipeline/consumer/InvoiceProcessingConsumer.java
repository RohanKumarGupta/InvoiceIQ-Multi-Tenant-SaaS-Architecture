package com.invoiceiq.pipeline.consumer;

import com.invoiceiq.extraction.service.AiExtractionService;
import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.invoice.model.InvoiceStatus;
import com.invoiceiq.invoice.repository.InvoiceRepository;
import com.invoiceiq.pipeline.event.InvoiceUploadedEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceProcessingConsumer {

    private final InvoiceRepository invoiceRepository;
    private final AiExtractionService aiExtractionService;

    @RabbitListener(queues = "${app.pipeline.queue:invoice.process.queue}", ackMode = "MANUAL")
    @Transactional
    public void processInvoice(InvoiceUploadedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("Received processing task for invoiceId: {}, tenantId: {}, fileKey: {}",
                event.invoiceId(), event.tenantId(), event.fileKey());

        try {
            Optional<Invoice> optionalInvoice = invoiceRepository.findByIdAndTenantId(event.invoiceId(), event.tenantId());
            if (optionalInvoice.isEmpty()) {
                log.warn("Invoice {} not found for tenant {}. Dropping message.", event.invoiceId(), event.tenantId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            Invoice invoice = optionalInvoice.get();

            // Idempotency check: if already processed or past UPLOADED status, acknowledge and skip
            if (invoice.getStatus() != InvoiceStatus.UPLOADED && invoice.getStatus() != InvoiceStatus.FAILED) {
                log.info("Invoice {} is already in status {}. Skipping redundant processing.", invoice.getId(), invoice.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Transition status to PROCESSING
            invoice.setStatus(InvoiceStatus.PROCESSING);
            invoiceRepository.save(invoice);
            log.info("Invoice {} status transitioned to PROCESSING. Starting Claude AI extraction...", invoice.getId());

            // Run AI extraction pipeline
            aiExtractionService.processInvoiceExtraction(event.invoiceId(), event.tenantId());

            // Acknowledge successfully handled queue event
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Error processing invoice pipeline event for invoiceId: {}: {}", event.invoiceId(), e.getMessage(), e);
            // Requeue false routes the failed payload straight to the dead-letter exchange (DLQ)
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
