package org.springframework.samples.petclinic.owner;

/**
 * A lightweight DTO for Owner list view to avoid loading unnecessary data
 */
public class OwnerDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private String address;
    private String city;
    private String telephone;
    private int petCount;

    public OwnerDTO(Integer id, String firstName, String lastName, String address, String city, String telephone, int petCount) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.city = city;
        this.telephone = telephone;
        this.petCount = petCount;
    }

    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getTelephone() {
        return telephone;
    }

    public int getPetCount() {
        return petCount;
    }
}