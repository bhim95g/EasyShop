package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yearup.data.ProductDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MySqlShoppingCartDao extends MySqlDaoBase implements ShoppingCartDao
{
    private final ProductDao productDao;

    @Autowired
    public MySqlShoppingCartDao(DataSource dataSource, ProductDao productDao)
    {
        super(dataSource);
        this.productDao = productDao;
    }

    @Override
    public ShoppingCart getByUserId(int userId)
    {
        ShoppingCart cart = new ShoppingCart();
        String sql = "SELECT product_id, quantity FROM shopping_cart WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    int productId = resultSet.getInt("product_id");
                    int quantity = resultSet.getInt("quantity");

                    Product product = productDao.getById(productId);
                    if (product != null)
                    {
                        ShoppingCartItem item = new ShoppingCartItem();
                        item.setProduct(product);
                        item.setQuantity(quantity);
                        cart.add(item);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to load shopping cart from database.", e);
        }
        return cart;
    }

    @Override
    public void addToCart(int userId, ShoppingCartItem item)
    {
        String sql = "INSERT INTO shopping_cart (user_id, product_id, quantity) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE quantity = quantity + ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userId);
            statement.setInt(2, item.getProduct().getProductId());
            statement.setInt(3, item.getQuantity());
            statement.setInt(4, item.getQuantity());
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to add shopping cart item to database.", e);
        }
    }

    @Override
    public boolean updateQuantity(int userId, int productId, int quantity)
    {
        String sql = "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?";
        String existsSql = "SELECT 1 FROM shopping_cart WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, quantity);
            statement.setInt(2, userId);
            statement.setInt(3, productId);
            if (statement.executeUpdate() > 0)
            {
                return true;
            }

            // Some driver settings count changed rows instead of matched rows.
            // A no-op update of an existing item must not be reported as not found.
            try (PreparedStatement exists = connection.prepareStatement(existsSql))
            {
                exists.setInt(1, userId);
                exists.setInt(2, productId);
                try (ResultSet resultSet = exists.executeQuery())
                {
                    return resultSet.next();
                }
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to update shopping cart item in database.", e);
        }
    }

    @Override
    public void removeFromCart(int userId, int productId)
    {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userId);
            statement.setInt(2, productId);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to remove shopping cart item from database.", e);
        }
    }

    @Override
    public void clearCart(int userId)
    {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to clear shopping cart in database.", e);
        }
    }
}
