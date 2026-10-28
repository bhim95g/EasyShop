package org.yearup.data.mysql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.models.checkout.CheckoutRequest;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutOrderDaoIntegrationTest
{
    private DataSource database;
    private CheckoutOrderDao dao;

    @BeforeEach
    void setUp() throws Exception
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:checkout_" + UUID.randomUUID().toString().replace("-", "") +
                ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        database = ds;
        dao = new CheckoutOrderDao(database);

        try (Connection c = database.getConnection(); Statement s = c.createStatement())
        {
            s.execute("CREATE TABLE products (" +
                    "product_id INT PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2), stock INT)");
            s.execute("CREATE TABLE shopping_cart (" +
                    "user_id INT, product_id INT, quantity INT, PRIMARY KEY(user_id, product_id))");
            s.execute("CREATE TABLE orders (" +
                    "order_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, date TIMESTAMP NOT NULL, " +
                    "address VARCHAR(100), city VARCHAR(50), state VARCHAR(50), zip VARCHAR(20), " +
                    "shipping_amount DECIMAL(10,2), status VARCHAR(30), payment_provider VARCHAR(30), " +
                    "payment_reference VARCHAR(255), total_amount DECIMAL(12,2), currency CHAR(3))");
            s.execute("CREATE TABLE order_line_items (" +
                    "order_line_item_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, " +
                    "product_id INT, sales_price DECIMAL(10,2), quantity INT, " +
                    "discount DECIMAL(10,2), product_name VARCHAR(100))");
            s.execute("INSERT INTO products VALUES (15, 'Headphones', 19.99, 3)");
            s.execute("INSERT INTO shopping_cart VALUES (7, 15, 2)");
        }
    }

    @Test
    void checkoutCreatesOrderWithServerPricesAndReservesInventory() throws Exception
    {
        long id = dao.createOrder(7, request("SIMULATED"));
        Map<String, Object> order = dao.getOrder(7, id);

        assertEquals("PENDING_PAYMENT", order.get("status"));
        assertEquals(new BigDecimal("39.98"), order.get("total"));
        assertEquals("SIMULATED", order.get("paymentMethod"));
        assertEquals(1, stock());
        assertEquals(0, cartCount());
        assertEquals("Headphones", ((Map<?, ?>)((java.util.List<?>)order.get("items")).get(0)).get("name"));
    }

    @Test
    void insufficientStockRollsBackOrderAndPreservesCart() throws Exception
    {
        try (Connection c = database.getConnection(); Statement s = c.createStatement())
        {
            s.execute("UPDATE shopping_cart SET quantity = 4 WHERE user_id = 7");
        }

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> dao.createOrder(7, request("SIMULATED")));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals(3, stock());
        assertEquals(1, cartCount());
        assertEquals(0, orderCount());
    }

    @Test
    void declinedPaymentRestoresStockOnceEvenWhenRetried() throws Exception
    {
        long id = dao.createOrder(7, request("SIMULATED"));
        dao.simulatePayment(7, id, false);
        dao.simulatePayment(7, id, false);

        assertEquals("DECLINED", dao.getOrder(7, id).get("status"));
        assertEquals(3, stock());
    }

    @Test
    void approvedPaymentKeepsStockReservedAndIsIdempotent() throws Exception
    {
        long id = dao.createOrder(7, request("SIMULATED"));
        dao.simulatePayment(7, id, true);
        dao.simulatePayment(7, id, true);

        assertEquals("PAID", dao.getOrder(7, id).get("status"));
        assertEquals(1, stock());
    }

    @Test
    void anAuthenticatedCustomerCannotReadAnotherCustomersOrder()
    {
        long id = dao.createOrder(7, request("SIMULATED"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> dao.getOrder(8, id));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        assertTrue(dao.getOrders(8).isEmpty());
    }

    @Test
    void stripeWebhookMustMatchSessionAndPaymentAmount() throws Exception
    {
        long id = dao.createOrder(7, request("STRIPE_TEST"));
        dao.attachStripeSession(id, "cs_test_order42");

        assertThrows(ResponseStatusException.class,
                () -> dao.processStripeEvent(id, "cs_test_other", "PAID", 3998));
        assertThrows(ResponseStatusException.class,
                () -> dao.processStripeEvent(id, "cs_test_order42", "PAID", 100));
        assertEquals("PENDING_PAYMENT", dao.getOrder(7, id).get("status"));

        dao.processStripeEvent(id, "cs_test_order42", "PAID", 3998);
        assertEquals("PAID", dao.getOrder(7, id).get("status"));
    }

    private CheckoutRequest request(String paymentMethod)
    {
        CheckoutRequest req = new CheckoutRequest();
        req.setAddress("123 Main St");
        req.setCity("Pittsburgh");
        req.setState("PA");
        req.setZip("15222");
        req.setPaymentMethod(paymentMethod);
        return req;
    }

    private int stock() throws Exception
    {
        try (Connection c = database.getConnection(); Statement s = c.createStatement();
             ResultSet row = s.executeQuery("SELECT stock FROM products WHERE product_id = 15"))
        {
            row.next();
            return row.getInt(1);
        }
    }

    private int cartCount() throws Exception
    {
        try (Connection c = database.getConnection(); Statement s = c.createStatement();
             ResultSet row = s.executeQuery("SELECT COUNT(*) FROM shopping_cart"))
        {
            row.next();
            return row.getInt(1);
        }
    }

    private int orderCount() throws Exception
    {
        try (Connection c = database.getConnection(); Statement s = c.createStatement();
             ResultSet row = s.executeQuery("SELECT COUNT(*) FROM orders"))
        {
            row.next();
            return row.getInt(1);
        }
    }
}
