package org.yearup.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.mysql.CheckoutOrderDao;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Stripe must call this endpoint directly. Browser redirects never mark orders paid.
 * Rejects unsigned/replayed/old events and production-mode events.
 */
@RestController
@RequestMapping("/checkout/stripe")
public class StripeTestWebhookController
{
    private final CheckoutOrderDao orders;
    private final ObjectMapper json;
    private final String webhookSecret;

    public StripeTestWebhookController(CheckoutOrderDao orders, ObjectMapper json,
                                       @Value("${checkout.stripe.webhook-secret:}") String webhookSecret)
    {
        this.orders = orders;
        this.json = json;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> receive(@RequestBody String body,
                                        @RequestHeader(value = "Stripe-Signature", required = false) String signature)
    {
        if (!verifySignature(body, signature, webhookSecret, Instant.now().getEpochSecond()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        try
        {
            JsonNode event = json.readTree(body);
            if (event.path("livemode").asBoolean(true))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

            String type = event.path("type").asText();
            if (!("checkout.session.completed".equals(type) ||
                  "checkout.session.async_payment_succeeded".equals(type) ||
                  "checkout.session.async_payment_failed".equals(type) ||
                  "checkout.session.expired".equals(type)))
                return ResponseEntity.ok().build();

            JsonNode session = event.path("data").path("object");
            String sessionId = session.path("id").asText("");
            String orderRef = session.path("metadata").path("order_id").asText("");
            if (!sessionId.startsWith("cs_test_") || !session.path("mode").asText("").equals("payment"))
                return ResponseEntity.badRequest().build();

            long orderId = Long.parseLong(orderRef);
            if (!session.path("client_reference_id").asText("").equals(orderRef))
                return ResponseEntity.badRequest().build();

            String status;
            if ("checkout.session.expired".equals(type))
                status = "CANCELED";
            else if ("checkout.session.async_payment_failed".equals(type))
                status = "DECLINED";
            else
            {
                // A completed checkout can still have pending asynchronous payment.
                if (!"paid".equals(session.path("payment_status").asText("")))
                    return ResponseEntity.ok().build();
                status = "PAID";
            }

            long amount = session.path("amount_total").asLong(-1L);
            if (!"usd".equalsIgnoreCase(session.path("currency").asText("")) || amount < 0)
                return ResponseEntity.badRequest().build();

            orders.processStripeEvent(orderId, sessionId, status, amount);
            return ResponseEntity.ok().build();
        }
        catch (ResponseStatusException e)
        {
            // Non-2xx tells Stripe to retry and alerts operators to a data inconsistency.
            return ResponseEntity.status(e.getStatus()).build();
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    public static boolean verifySignature(String payload, String header, String secret, long now)
    {
        if (header == null || secret == null || !secret.startsWith("whsec_"))
            return false;
        try
        {
            String timestamp = null;
            String[] parts = header.split(",");
            for (String part : parts)
            {
                if (part.trim().startsWith("t="))
                    timestamp = part.trim().substring(2);
            }
            if (timestamp == null)
                return false;
            long sentAt = Long.parseLong(timestamp);
            if (Math.abs(now - sentAt) > 300)
                return false;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            for (String part : parts)
            {
                part = part.trim();
                if (!part.startsWith("v1="))
                    continue;
                String hex = part.substring(3);
                if (hex.length() != expected.length * 2)
                    continue;
                byte[] supplied = new byte[expected.length];
                for (int i = 0; i < supplied.length; i++)
                {
                    int hi = Character.digit(hex.charAt(i * 2), 16);
                    int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
                    if (hi < 0 || lo < 0)
                        return false;
                    supplied[i] = (byte) ((hi << 4) | lo);
                }
                if (MessageDigest.isEqual(expected, supplied))
                    return true;
            }
        }
        catch (Exception ignored)
        {
            return false;
        }
        return false;
    }
}
