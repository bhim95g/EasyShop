package org.yearup.data.mysql;

import org.springframework.stereotype.Component;
import org.yearup.data.OrderDao;
import org.yearup.models.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MySqlOrderDao extends MySqlDaoBase implements OrderDao
{
    public MySqlOrderDao(DataSource dataSource)
    {
        super(dataSource);
    }

    @Override
    public Order checkout(int userId, Profile shipTo, ShoppingCart cart)
    {
        String orderSql = "INSERT INTO orders (user_id, date, address, city, state, zip, shipping_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String lineSql = "INSERT INTO order_line_items (order_id, product_id, sales_price, quantity, discount) " +
                "VALUES (?, ?, ?, ?, ?)";
        String clearCartSql = "DELETE FROM shopping_cart WHERE user_id = ?";

        Order order = new Order();
        order.setUserId(userId);
        order.setDate(LocalDateTime.now().withNano(0));
        order.setAddress(shipTo.getAddress());
        order.setCity(shipTo.getCity());
        order.setState(shipTo.getState());
        order.setZip(shipTo.getZip());
        order.setShippingAmount(BigDecimal.ZERO);

        try (Connection connection = getConnection())
        {
            // the order, its line items and emptying the cart all succeed or all fail together
            connection.setAutoCommit(false);
            try
            {
                try (PreparedStatement statement = connection.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS))
                {
                    statement.setInt(1, userId);
                    statement.setTimestamp(2, Timestamp.valueOf(order.getDate()));
                    statement.setString(3, order.getAddress());
                    statement.setString(4, order.getCity());
                    statement.setString(5, order.getState());
                    statement.setString(6, order.getZip());
                    statement.setBigDecimal(7, order.getShippingAmount());
                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys())
                    {
                        keys.next();
                        order.setOrderId(keys.getInt(1));
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(lineSql, Statement.RETURN_GENERATED_KEYS))
                {
                    for (ShoppingCartItem cartItem : cart.getItems().values())
                    {
                        BigDecimal price = cartItem.getProduct().getPrice();
                        BigDecimal discount = price
                                .multiply(new BigDecimal(cartItem.getQuantity()))
                                .multiply(cartItem.getDiscountPercent())
                                .setScale(2, RoundingMode.HALF_UP);

                        OrderLineItem line = new OrderLineItem(0, order.getOrderId(), cartItem.getProductId(),
                                price, cartItem.getQuantity(), discount);

                        statement.setInt(1, line.getOrderId());
                        statement.setInt(2, line.getProductId());
                        statement.setBigDecimal(3, line.getSalesPrice());
                        statement.setInt(4, line.getQuantity());
                        statement.setBigDecimal(5, line.getDiscount());
                        statement.executeUpdate();

                        try (ResultSet keys = statement.getGeneratedKeys())
                        {
                            if (keys.next())
                                line.setOrderLineItemId(keys.getInt(1));
                        }
                        order.getLineItems().add(line);
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(clearCartSql))
                {
                    statement.setInt(1, userId);
                    statement.executeUpdate();
                }

                connection.commit();
                return order;
            }
            catch (SQLException e)
            {
                connection.rollback();
                throw e;
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Order> getByUserId(int userId)
    {
        String sql = "SELECT o.*, li.order_line_item_id, li.product_id, li.sales_price, li.quantity, li.discount " +
                "FROM orders o LEFT JOIN order_line_items li ON li.order_id = o.order_id " +
                "WHERE o.user_id = ? ORDER BY o.date DESC, o.order_id DESC, li.order_line_item_id";

        Map<Integer, Order> orders = new LinkedHashMap<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userId);

            try (ResultSet row = statement.executeQuery())
            {
                while (row.next())
                {
                    int orderId = row.getInt("order_id");
                    Order order = orders.get(orderId);
                    if (order == null)
                    {
                        order = new Order();
                        order.setOrderId(orderId);
                        order.setUserId(row.getInt("user_id"));
                        order.setDate(row.getTimestamp("date").toLocalDateTime());
                        order.setAddress(row.getString("address"));
                        order.setCity(row.getString("city"));
                        order.setState(row.getString("state"));
                        order.setZip(row.getString("zip"));
                        order.setShippingAmount(row.getBigDecimal("shipping_amount"));
                        orders.put(orderId, order);
                    }

                    int lineId = row.getInt("order_line_item_id");
                    if (!row.wasNull())
                    {
                        order.getLineItems().add(new OrderLineItem(lineId, orderId,
                                row.getInt("product_id"),
                                row.getBigDecimal("sales_price"),
                                row.getInt("quantity"),
                                row.getBigDecimal("discount")));
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

        return new ArrayList<>(orders.values());
    }
}
