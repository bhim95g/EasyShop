package org.yearup.data.mysql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yearup.models.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MySqlProfileDaoUnitTest
{
    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private MySqlProfileDao dao;

    @BeforeEach
    void setUp() throws SQLException
    {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dao = new MySqlProfileDao(dataSource);
    }

    @Test
    void getByUserId_usesParameterizedCallerIdAndMapsData() throws SQLException
    {
        ResultSet rows = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true);
        when(rows.getInt("user_id")).thenReturn(7);
        when(rows.getString("first_name")).thenReturn("Alice");
        when(rows.getString("last_name")).thenReturn("Example");
        when(rows.getString("phone")).thenReturn("");
        when(rows.getString("email")).thenReturn("alice@example.com");
        when(rows.getString("address")).thenReturn("123 Main St");
        when(rows.getString("city")).thenReturn("Pittsburgh");
        when(rows.getString("state")).thenReturn("PA");
        when(rows.getString("zip")).thenReturn("15222");

        Profile result = dao.getByUserId(7);

        assertEquals(7, result.getUserId());
        assertEquals("Alice", result.getFirstName());
        assertEquals("alice@example.com", result.getEmail());
        assertEquals("Pittsburgh", result.getCity());
        verify(statement).setInt(1, 7);
        verify(rows).close();
    }

    @Test
    void getByUserId_returnsNullWhenNotFound() throws SQLException
    {
        ResultSet rows = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(false);

        assertNull(dao.getByUserId(99));
        verify(statement).setInt(1, 99);
    }

    @Test
    void update_usesTheProfileUserIdAsTheWhereParameter() throws SQLException
    {
        Profile profile = new Profile(7, "Alice", "Example", "", "alice@example.com",
                "123 Main St", "Pittsburgh", "PA", "15222");

        dao.update(profile);

        verify(statement).setString(1, "Alice");
        verify(statement).setString(4, "alice@example.com");
        verify(statement).setInt(9, 7);
        verify(statement).executeUpdate();
    }

    @Test
    void getByUserId_propagatesDatabaseError() throws SQLException
    {
        SQLException cause = new SQLException("Connection lost");
        when(statement.executeQuery()).thenThrow(cause);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> dao.getByUserId(7));

        assertSame(cause, exception.getCause());
    }

    @Test
    void update_propagatesDatabaseError() throws SQLException
    {
        SQLException cause = new SQLException("Update failed");
        when(statement.executeUpdate()).thenThrow(cause);

        Profile profile = new Profile(7, "", "", "", "", "", "", "", "");
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> dao.update(profile));

        assertSame(cause, exception.getCause());
    }
}
