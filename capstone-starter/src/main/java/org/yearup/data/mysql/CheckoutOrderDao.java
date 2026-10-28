package org.yearup.data.mysql;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.models.checkout.CheckoutRequest;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * All order placement, inventory reservation and cart clearing use one JDBC transaction.
 * Payment transitions are idempotent and restore reserved inventory on failure/expiry.
 */
@Component
public class CheckoutOrderDao
{
    private final DataSource dataSource;

    public CheckoutOrderDao(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public long createOrder(int userId, CheckoutRequest shipping)
    {
        try (Connection connection = dataSource.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                List<CartLine> lines = new ArrayList<>();
                String query = "SELECT c.product_id, c.quantity, p.name, p.price, p.stock " +
                        "FROM shopping_cart c INNER JOIN products p ON c.product_id = p.product_id " +
                        "WHERE c.user_id = ? ORDER BY c.product_id FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(query))
                {
                    statement.setInt(1, userId);
                    try (ResultSet rows = statement.executeQuery())
                    {
                        while (rows.next())
                        {
                            int quantity = rows.getInt("quantity");
                            int stock = rows.getInt("stock");
                            if (quantity < 1 || quantity > 100)
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cart quantity.");
                            if (stock < quantity)
                                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock for " + rows.getString("name"));
                            BigDecimal price = rows.getBigDecimal("price");
                            if (price == null || price.signum() < 0 || price.scale() > 2)
                                throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid product price.");
                            lines.add(new CartLine(rows.getInt("product_id"), rows.getString("name"), price, quantity));
                        }
                    }
                }
                if (lines.isEmpty())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty.");

                BigDecimal total = BigDecimal.ZERO;
                for (CartLine line : lines)
                    total = total.add(line.price.multiply(BigDecimal.valueOf(line.quantity)));
                if (total.signum() <= 0 || total.compareTo(new BigDecimal("9999999999.99")) > 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported order total.");

                // Do not accept shipping fees or totals from the browser.
                long orderId;
                String insert = "INSERT INTO orders (user_id, date, address, city, state, zip, shipping_amount, " +
                        "status, payment_provider, total_amount, currency) " +
                        "VALUES (?, NOW(), ?, ?, ?, ?, 0, 'PENDING_PAYMENT', ?, ?, 'USD')";
                try (PreparedStatement statement = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS))
                {
                    statement.setInt(1, userId);
                    statement.setString(2, shipping.getAddress());
                    statement.setString(3, shipping.getCity());
                    statement.setString(4, shipping.getState());
                    statement.setString(5, shipping.getZip());
                    statement.setString(6, shipping.getPaymentMethod());
                    statement.setBigDecimal(7, total);
                    if (statement.executeUpdate() != 1)
                        throw new SQLException("Order was not inserted.");
                    try (ResultSet keys = statement.getGeneratedKeys())
                    {
                        if (!keys.next())
                            throw new SQLException("Order ID was not generated.");
                        orderId = keys.getLong(1);
                    }
                }

                for (CartLine line : lines)
                {
                    try (PreparedStatement stock = connection.prepareStatement(
                            "UPDATE products SET stock = stock - ? WHERE product_id = ? AND stock >= ?"))
                    {
                        stock.setInt(1, line.quantity);
                        stock.setInt(2, line.productId);
                        stock.setInt(3, line.quantity);
                        if (stock.executeUpdate() != 1)
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product stock changed, retry checkout.");
                    }
                    try (PreparedStatement item = connection.prepareStatement(
                            "INSERT INTO order_line_items " +
                            "(order_id, product_id, sales_price, quantity, discount, product_name) " +
                            "VALUES (?, ?, ?, ?, 0, ?)"))
                    {
                        item.setLong(1, orderId);
                        item.setInt(2, line.productId);
                        item.setBigDecimal(3, line.price);
                        item.setInt(4, line.quantity);
                        item.setString(5, line.name);
                        item.executeUpdate();
                    }
                }
                try (PreparedStatement clear = connection.prepareStatement("DELETE FROM shopping_cart WHERE user_id = ?"))
                {
                    clear.setInt(1, userId);
                    clear.executeUpdate();
                }
                connection.commit();
                return orderId;
            }
            catch (Exception e)
            {
                connection.rollback();
                throw e;
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to create checkout order.", e);
        }
    }

    public Map<String, Object> getOrder(int userId, long orderId)
    {
        try (Connection connection = dataSource.getConnection())
        {
            Map<String, Object> order = readOrder(connection, orderId, userId);
            if (order == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
            return order;
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to read order.", e);
        }
    }

    public List<Map<String, Object>> getOrders(int userId)
    {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT order_id FROM orders WHERE user_id = ? ORDER BY date DESC, order_id DESC LIMIT 100"))
        {
            stmt.setInt(1, userId);
            List<Long> ids = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                    ids.add(rs.getLong(1));
            }
            List<Map<String, Object>> orders = new ArrayList<>();
            for (long id : ids)
                orders.add(readOrder(connection, id, userId));
            return orders;
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to list orders.", e);
        }
    }

    // Only called after creating the Stripe session with a server-owned order reference.
    public void attachStripeSession(long orderId, String sessionId)
    {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "UPDATE orders SET payment_reference = ? WHERE order_id = ? " +
                     "AND payment_provider = 'STRIPE_TEST' AND status = 'PENDING_PAYMENT' AND payment_reference IS NULL"))
        {
            stmt.setString(1, sessionId);
            stmt.setLong(2, orderId);
            if (stmt.executeUpdate() != 1)
                throw new IllegalStateException("Could not attach Stripe session.");
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to attach Stripe session.", e);
        }
    }

    public void simulatePayment(int userId, long orderId, boolean approved)
    {
        transition(orderId, userId, "SIMULATED", null, approved ? "PAID" : "DECLINED", null);
    }

    public void processStripeEvent(long orderId, String sessionId, String nextStatus, long amountCents)
    {
        transition(orderId, null, "STRIPE_TEST", sessionId, nextStatus, amountCents);
    }

    // Release a reserved order if Stripe session creation fails before a redirect is offered.
    public void failStripeSession(long orderId)
    {
        transition(orderId, null, "STRIPE_TEST", null, "CANCELED", null);
    }

    private void transition(long orderId, Integer userId, String provider, String sessionId,
                            String nextStatus, Long amountCents)
    {
        if (!Set.of("PAID", "DECLINED", "CANCELED").contains(nextStatus))
            throw new IllegalArgumentException("Invalid payment outcome.");

        try (Connection connection = dataSource.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                String currentStatus;
                String currentProvider;
                String currentSession;
                BigDecimal total;
                int owner;
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT user_id, status, payment_provider, payment_reference, total_amount " +
                        "FROM orders WHERE order_id = ? FOR UPDATE"))
                {
                    stmt.setLong(1, orderId);
                    try (ResultSet row = stmt.executeQuery())
                    {
                        if (!row.next())
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
                        owner = row.getInt("user_id");
                        currentStatus = row.getString("status");
                        currentProvider = row.getString("payment_provider");
                        currentSession = row.getString("payment_reference");
                        total = row.getBigDecimal("total_amount");
                    }
                }
                if (userId != null && owner != userId)
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
                if (!provider.equals(currentProvider))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment method mismatch.");
                if (sessionId != null && !sessionId.equals(currentSession))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Stripe session mismatch.");
                if (sessionId == null && provider.equals("STRIPE_TEST") && currentSession != null)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel an attached Stripe session.");
                if (amountCents != null && total.movePointRight(2).longValueExact() != amountCents)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment amount mismatch.");

                if (!"PENDING_PAYMENT".equals(currentStatus))
                {
                    // Webhook retries must be safe; never double-release inventory.
                    if (!nextStatus.equals(currentStatus))
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already finalized.");
                    connection.commit();
                    return;
                }

                if (!"PAID".equals(nextStatus))
                {
                    try (PreparedStatement items = connection.prepareStatement(
                            "SELECT product_id, quantity FROM order_line_items WHERE order_id = ?"))
                    {
                        items.setLong(1, orderId);
                        try (ResultSet rs = items.executeQuery())
                        {
                            while (rs.next())
                            {
                                try (PreparedStatement restore = connection.prepareStatement(
                                        "UPDATE products SET stock = stock + ? WHERE product_id = ?"))
                                {
                                    restore.setInt(1, rs.getInt("quantity"));
                                    restore.setInt(2, rs.getInt("product_id"));
                                    restore.executeUpdate();
                                }
                            }
                        }
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE orders SET status = ? WHERE order_id = ?"))
                {
                    update.setString(1, nextStatus);
                    update.setLong(2, orderId);
                    update.executeUpdate();
                }
                connection.commit();
            }
            catch (Exception e)
            {
                connection.rollback();
                throw e;
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to update payment status.", e);
        }
    }

    private Map<String, Object> readOrder(Connection connection, long id, int userId) throws SQLException
    {
        Map<String, Object> order = null;
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT order_id, status, payment_provider, total_amount, currency, date, " +
                "address, city, state, zip FROM orders WHERE order_id = ? AND user_id = ?"))
        {
            stmt.setLong(1, id);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    order = new LinkedHashMap<>();
                    order.put("orderId", rs.getLong("order_id"));
                    order.put("status", rs.getString("status"));
                    order.put("paymentMethod", rs.getString("payment_provider"));
                    order.put("total", rs.getBigDecimal("total_amount"));
                    order.put("currency", rs.getString("currency"));
                    order.put("createdAt", rs.getTimestamp("date").toInstant().toString());
                    order.put("address", rs.getString("address"));
                    order.put("city", rs.getString("city"));
                    order.put("state", rs.getString("state"));
                    order.put("zip", rs.getString("zip"));
                }
            }
        }
        if (order == null)
            return null;
        List<Map<String, Object>> lines = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT product_id, product_name, quantity, sales_price FROM order_line_items WHERE order_id = ?"))
        {
            stmt.setLong(1, id);
            try (ResultSet rows = stmt.executeQuery())
            {
                while (rows.next())
                {
                    Map<String, Object> line = new LinkedHashMap<>();
                    line.put("productId", rows.getInt("product_id"));
                    line.put("name", rows.getString("product_name"));
                    line.put("quantity", rows.getInt("quantity"));
                    line.put("unitPrice", rows.getBigDecimal("sales_price"));
                    lines.add(line);
                }
            }
        }
        order.put("items", lines);
        return order;
    }

    private static final class CartLine
    {
        final int productId;
        final String name;
        final BigDecimal price;
        final int quantity;
        CartLine(int productId, String name, BigDecimal price, int quantity)
        {
            this.productId = productId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }
}
