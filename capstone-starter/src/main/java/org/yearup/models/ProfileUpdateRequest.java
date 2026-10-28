package org.yearup.models;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A full replacement of the caller's own profile data.
 * The user ID is intentionally absent: it always comes from the authenticated user.
 */
public class ProfileUpdateRequest
{
    @NotNull
    @Size(max = 50)
    private String firstName;

    @NotNull
    @Size(max = 50)
    private String lastName;

    @NotNull
    @Size(max = 20)
    private String phone;

    @NotNull
    @Email
    @Size(max = 200)
    private String email;

    @NotNull
    @Size(max = 200)
    private String address;

    @NotNull
    @Size(max = 50)
    private String city;

    @NotNull
    @Size(max = 50)
    private String state;

    @NotNull
    @Size(max = 20)
    private String zip;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public Profile toProfile(int userId)
    {
        return new Profile(userId, firstName, lastName, phone, email, address, city, state, zip);
    }
}
