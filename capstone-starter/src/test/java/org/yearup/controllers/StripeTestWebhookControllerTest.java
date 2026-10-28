package org.yearup.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.yearup.data.mysql.CheckoutOrderDao;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripeTestWebhookControllerTest
{
    private static final String SECRET = "whsec_unit_test_signing_secret";
    private CheckoutOrderDao dao;
    private StripeTestWebhookController controller;

    @BeforeEach
    void setUp()
    {
        dao = mock(CheckoutOrderDao.class);
        controller = new StripeTestWebhookController(dao, new ObjectMapper(), SECRET);
    }

    @Test
    void validSignedPaidEventFinalizesMatchingOrder() throws Exception
    {
        String body = event("checkout.session.completed", "paid", false);
        ResponseEntity<Void> response = controller.receive(body, sign(body, Instant.now().getEpochSecond()));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dao).processStripeEvent(42L, "cs_test_example", "PAID", 1499L);
    }

    @Test
    void pendingPaymentDoesNotMarkOrderPaid() throws Exception
    {
        String body = event("checkout.session.completed", "unpaid", false);
        assertEquals(HttpStatus.OK, controller.receive(body, sign(body, Instant.now().getEpochSecond())).getStatusCode());
        verifyNoInteractions(dao);
    }

    @Test
    void expiredSignedSessionReleasesReservation() throws Exception
    {
        String body = event("checkout.session.expired", "unpaid", false);
        assertEquals(HttpStatus.OK, controller.receive(body, sign(body, Instant.now().getEpochSecond())).getStatusCode());
        verify(dao).processStripeEvent(42L, "cs_test_example", "CANCELED", 1499L);
    }

    @Test
    void invalidSignatureIsRejected()
    {
        assertEquals(HttpStatus.BAD_REQUEST, controller.receive(event("checkout.session.completed", "paid", false),
                "t=123,v1=00").getStatusCode());
        verifyNoInteractions(dao);
    }

    @Test
    void validSignatureWithModifiedBodyIsRejected() throws Exception
    {
        String original = event("checkout.session.completed", "paid", false);
        String tampered = original.replace("1499", "9999");
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.receive(tampered, sign(original, Instant.now().getEpochSecond())).getStatusCode());
        verifyNoInteractions(dao);
    }

    @Test
    void expiredTimestampIsRejected() throws Exception
    {
        String body = event("checkout.session.completed", "paid", false);
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.receive(body, sign(body, Instant.now().getEpochSecond() - 600)).getStatusCode());
        verifyNoInteractions(dao);
    }

    @Test
    void liveModeEventIsRejectedEvenWithValidSignature() throws Exception
    {
        String body = event("checkout.session.completed", "paid", true);
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.receive(body, sign(body, Instant.now().getEpochSecond())).getStatusCode());
        verifyNoInteractions(dao);
    }

    @Test
    void wrongOrderReferenceIsRejected() throws Exception
    {
        String body = event("checkout.session.completed", "paid", false)
                .replace("\"client_reference_id\":\"42\"", "\"client_reference_id\":\"99\"");
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.receive(body, sign(body, Instant.now().getEpochSecond())).getStatusCode());
        verifyNoInteractions(dao);
    }

    private static String event(String type, String paymentStatus, boolean live)
    {
        return "{\"livemode\":" + live +
                ",\"type\":\"" + type + "\",\"data\":{\"object\":{"+
                "\"id\":\"cs_test_example\",\"mode\":\"payment\","+
                "\"metadata\":{\"order_id\":\"42\"},"+
                "\"client_reference_id\":\"42\","+
                "\"payment_status\":\"" + paymentStatus + "\","+
                "\"amount_total\":1499,\"currency\":\"usd\"}}}";
    }

    private static String sign(String payload, long timestamp) throws Exception
    {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = hmac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest)
            hex.append(String.format("%02x", b & 0xff));
        return "t=" + timestamp + ",v1=" + hex;
    }
}
