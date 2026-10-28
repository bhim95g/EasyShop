package org.yearup.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.UserDao;
import org.yearup.data.mysql.CheckoutOrderDao;
import org.yearup.models.User;
import org.yearup.models.checkout.CheckoutRequest;
import org.yearup.services.checkout.StripeTestCheckoutClient;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CheckoutControllerTest
{
    private CheckoutOrderDao dao;
    private UserDao users;
    private StripeTestCheckoutClient stripe;
    private CheckoutController controller;
    private final Principal principal = () -> "alice";

    @BeforeEach
    void setup()
    {
        dao = mock(CheckoutOrderDao.class);
        users = mock(UserDao.class);
        stripe = mock(StripeTestCheckoutClient.class);
        controller = new CheckoutController(dao, users, stripe, true);

        User user = new User();
        user.setId(7);
        when(users.getByUserName("alice")).thenReturn(user);
    }

    @Test
    void simulatorCreatesOrderOnlyForAuthenticatedCustomer()
    {
        when(dao.createOrder(eq(7), any())).thenReturn(42L);
        when(dao.getOrder(7, 42L)).thenReturn(Map.of(
                "orderId", 42L, "paymentMethod", "SIMULATED", "status", "PENDING_PAYMENT"));

        Map<String, Object> result = controller.createOrder(request("SIMULATED"), principal);

        assertEquals(42L, result.get("orderId"));
        verify(dao).createOrder(eq(7), any());
    }

    @Test
    void cannotCheckoutWithoutAuthentication()
    {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.createOrder(request("SIMULATED"), null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
        verifyNoInteractions(dao);
    }

    @Test
    void missingUserCannotCreateOrders()
    {
        when(users.getByUserName("alice")).thenReturn(null);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.createOrder(request("SIMULATED"), principal));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
        verifyNoInteractions(dao);
    }

    @Test
    void cannotSimulateAnotherUsersOrder()
    {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(dao).simulatePayment(7, 99L, true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.simulate(99L, Map.of("outcome", "APPROVED"), principal));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void invalidSimulationOutcomeIsRejected()
    {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.simulate(42L, Map.of("outcome", "MONEY"), principal));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(dao);
    }

    @Test
    void paymentSimulationCanBeDisabled()
    {
        CheckoutController disabled = new CheckoutController(dao, users, stripe, false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> disabled.createOrder(request("SIMULATED"), principal));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
        verifyNoInteractions(dao);
    }

    @Test
    void stripeRequiresTestKeyBeforeCreatingOrder()
    {
        when(stripe.isConfigured()).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.createOrder(request("STRIPE_TEST"), principal));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        verifyNoInteractions(dao);
    }

    private CheckoutRequest request(String method)
    {
        CheckoutRequest req = new CheckoutRequest();
        req.setAddress("123 Test St");
        req.setCity("Pittsburgh");
        req.setState("PA");
        req.setZip("15222");
        req.setPaymentMethod(method);
        return req;
    }
}
