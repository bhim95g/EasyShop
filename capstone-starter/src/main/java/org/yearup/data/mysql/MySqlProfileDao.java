package org.yearup.data.mysql;

import org.springframework.stereotype.Component;
import org.yearup.data.ProfileDao;
import org.yearup.models.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MySqlProfileDao extends MySqlDaoBase implements ProfileDao
{
    public MySqlProfileDao(DataSource dataSource)
    {
        super(dataSource);
    }

    @Override
    public Profile create(Profile profile)
    {
        String sql = "INSERT INTO profiles (user_id, first_name, last_name, phone, email, address, city, state, zip) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            bindFields(statement, profile);
            statement.executeUpdate();
            return profile;
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to create profile.", e);
        }
    }

    @Override
    public Profile getByUserId(int userId)
    {
        String sql = "SELECT user_id, first_name, last_name, phone, email, address, city, state, zip " +
                     "FROM profiles WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userId);
            try (ResultSet row = statement.executeQuery())
            {
                if (!row.next())
                {
                    return null;
                }

                return new Profile(
                        row.getInt("user_id"),
                        row.getString("first_name"),
                        row.getString("last_name"),
                        row.getString("phone"),
                        row.getString("email"),
                        row.getString("address"),
                        row.getString("city"),
                        row.getString("state"),
                        row.getString("zip")
                );
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to load profile.", e);
        }
    }

    @Override
    public void update(Profile profile)
    {
        String sql = "UPDATE profiles SET first_name = ?, last_name = ?, phone = ?, email = ?, " +
                     "address = ?, city = ?, state = ?, zip = ? WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, profile.getFirstName());
            statement.setString(2, profile.getLastName());
            statement.setString(3, profile.getPhone());
            statement.setString(4, profile.getEmail());
            statement.setString(5, profile.getAddress());
            statement.setString(6, profile.getCity());
            statement.setString(7, profile.getState());
            statement.setString(8, profile.getZip());
            statement.setInt(9, profile.getUserId());
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to update profile.", e);
        }
    }

    private void bindFields(PreparedStatement statement, Profile profile) throws SQLException
    {
        statement.setInt(1, profile.getUserId());
        statement.setString(2, profile.getFirstName());
        statement.setString(3, profile.getLastName());
        statement.setString(4, profile.getPhone());
        statement.setString(5, profile.getEmail());
        statement.setString(6, profile.getAddress());
        statement.setString(7, profile.getCity());
        statement.setString(8, profile.getState());
        statement.setString(9, profile.getZip());
    }
}
