package org.yearup.data.mysql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yearup.data.ProductDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MySqlShoppingCartDaoUnitTest
{
    private DataSource dataSource;
    private ProductDao productDao;
    private MySqlShoppingCartDao dao;

    @BeforeEach
    void setUp()
    {
        dataSource = mock(DataSource.class);
        productDao = mock(ProductDao.class);
        dao = new MySqlShoppingCartDao(dataSource, productDao);
    }

    @Test
    void getByUserId_propagatesDatabaseFailure() throws SQLException
    {
        SQLException rootCause = new SQLException("Connection refused");
        when(dataSource.getConnection()).thenThrow(rootCause);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> dao.getByUserId(7));

        assertSame(rootCause, error.getCause());
    }

    @Test
    void addToCart_propagatesDatabaseFailure() throws SQLException
    {
        SQLException rootCause = new SQLException("Connection refused");
        when(dataSource.getConnection()).thenThrow(rootCause);
        Product product = new Product();
        product.setProductId(15);
        ShoppingCartItem item = new ShoppingCartItem();
        item.setProduct(product);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> dao.addToCart(7, item));

        assertSame(rootCause, error.getCause());
    }

    @Test
    void updateQuantity_returnsTrueWhenRowsWereUpdated() throws SQLException
    {
        Connection connection = mock(Connection.class);
        PreparedStatement update = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?"))
                .thenReturn(update);
        when(update.executeUpdate()).thenReturn(1);

        assertTrue(dao.updateQuantity(7, 15, 2));
        verify(update).setInt(1, 2);
        verify(update).setInt(2, 7);
        verify(update).setInt(3, 15);
    }

    @Test
    void updateQuantity_returnsTrueWhenExistingQuantityWasUnchanged() throws SQLException
    {
        Connection connection = mock(Connection.class);
        PreparedStatement update = mock(PreparedStatement.class);
        PreparedStatement existenceCheck = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?"))
                .thenReturn(update);
        when(connection.prepareStatement(
                "SELECT 1 FROM shopping_cart WHERE user_id = ? AND product_id = ?"))
                .thenReturn(existenceCheck);
        when(update.executeUpdate()).thenReturn(0);
        when(existenceCheck.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        assertTrue(dao.updateQuantity(7, 15, 2));
        verify(existenceCheck).setInt(1, 7);
        verify(existenceCheck).setInt(2, 15);
    }

    @Test
    void updateQuantity_returnsFalseForMissingItem() throws SQLException
    {
        Connection connection = mock(Connection.class);
        PreparedStatement update = mock(PreparedStatement.class);
        PreparedStatement existenceCheck = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?"))
                .thenReturn(update);
        when(connection.prepareStatement(
                "SELECT 1 FROM shopping_cart WHERE user_id = ? AND product_id = ?"))
                .thenReturn(existenceCheck);
        when(update.executeUpdate()).thenReturn(0);
        when(existenceCheck.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertFalse(dao.updateQuantity(7, 99, 2));
    }
}
