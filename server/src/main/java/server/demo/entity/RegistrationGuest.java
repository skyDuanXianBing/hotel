package server.demo.entity;

import jakarta.persistence.*;
import server.demo.enums.ResidenceType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "registration_guests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_registration_guests_form_sort", columnNames = {"form_id", "sort_order"})
        },
        indexes = {
                @Index(name = "idx_registration_guests_form", columnList = "form_id")
        }
)
public class RegistrationGuest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private RegistrationForm form;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name_kana", length = 100)
    private String lastNameKana;

    @Column(name = "first_name_kana", length = 100)
    private String firstNameKana;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "nationality", length = 80)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "residence_type", length = 20)
    private ResidenceType residenceType;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "passport_number", length = 50)
    private String passportNumber;

    @Column(name = "prior_stay", length = 200)
    private String priorStay;

    @Column(name = "next_destination", length = 200)
    private String nextDestination;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationForm getForm() {
        return form;
    }

    public void setForm(RegistrationForm form) {
        this.form = form;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastNameKana() {
        return lastNameKana;
    }

    public void setLastNameKana(String lastNameKana) {
        this.lastNameKana = lastNameKana;
    }

    public String getFirstNameKana() {
        return firstNameKana;
    }

    public void setFirstNameKana(String firstNameKana) {
        this.firstNameKana = firstNameKana;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public ResidenceType getResidenceType() {
        return residenceType;
    }

    public void setResidenceType(ResidenceType residenceType) {
        this.residenceType = residenceType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public String getPriorStay() {
        return priorStay;
    }

    public void setPriorStay(String priorStay) {
        this.priorStay = priorStay;
    }

    public String getNextDestination() {
        return nextDestination;
    }

    public void setNextDestination(String nextDestination) {
        this.nextDestination = nextDestination;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
