package org.yearup.models.checkout;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Checkout prices and user IDs are never accepted from the browser. */
public class CheckoutRequest
{
    @NotBlank @Size(max = 100)
    private String address;

    @NotBlank @Size(max = 50)
    private String city;

    @NotBlank @Size(max = 50)
    private String state;

    @NotBlank @Size(max = 20)
    private String zip;

    @NotBlank
    @Pattern(regexp = "SIMULATED|STRIPE_TEST", message = "Use SIMULATED or STRIPE_TEST.")
    private String paymentMethod;

    public String getAddress() { return address; }
    public void setAddress(String value) { this.address = value; }
    public String getCity() { return city; }
    public void setCity(String value) { this.city = value; }
    public String getState() { return state; }
    public void setState(String value) { this.state = value; }
    public String getZip() { return zip; }
    public void setZip(String value) { this.zip = value; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { this.paymentMethod = value; }
}
