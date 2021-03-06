package at.ac.tuwien.student.sese2017.xp.hotelmanagement.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import at.ac.tuwien.student.sese2017.xp.hotelmanagement.HotelManagementApplicationTests;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.AddressEntity;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.CustomerEntity;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.Sex;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.repository.CustomerRepository;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Transactional
public class CustomerServiceTest extends HotelManagementApplicationTests {
  private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consetetur sadipscing "
      + "elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, "
      + "sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita "
      + "kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum "
      + "dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut "
      + "labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et "
      + "justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est "
      + "Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, "
      + "sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed "
      + "diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita "
      + "kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. \n" + "\n"
      + "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie "
      + "consequat, vel illum dolore eu feugiat nulla facilisis at";

  @Autowired
  private CustomerService customerService;
  @Autowired
  CustomerRepository repo;

  @Test
  public void testCreateEntityWithAllFields() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    Long id = customerService.save(entity);
    assertNotNull(id);
  }

  @Test
  public void testCreateEntityWithCompulsoryFields() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    entity.setCompanyName(null);
    entity.setFaxNumber(null);
    entity.setNote(null);
    entity.setWebAddress(null);
    Long id = customerService.save(entity);
    assertNotNull(id);
  }

  @Test(expected = ValidationException.class)
  public void testCreateEntityWithOneInvalidField() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    entity.setPhoneNumber("This is not a phone number");
    customerService.save(entity);
  }

  @Test(expected = ConstraintViolationException.class)
  public void testCreateEntityWithMultipleInvalidFields() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    entity.setBirthday(LocalDate.now().plus(1, ChronoUnit.DAYS));
    entity.setEmail("invalid email");
    customerService.save(entity);
  }

  @Test(expected = ConstraintViolationException.class)
  public void testCreateEntityWithCompulsoryNullField() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    entity.setName(null);
    customerService.save(entity);
  }

  @Test(expected = ConstraintViolationException.class)
  public void testCreateEntityWithInvalidDiscount() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    entity.setDiscount(BigDecimal.valueOf(101.0D));
    customerService.save(entity);
  }

  @Test(expected = ValidationException.class)
  public void testCreateEntityWithInvalidBirthday() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    entity.setBirthday(LocalDate.now().plus(1, ChronoUnit.DAYS));
    customerService.save(entity);
  }

  @Test(expected = ValidationException.class)
  public void testCreateEntityWithInvalidPhoneNumber() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    entity.setPhoneNumber("invalid Phone Number");
    customerService.save(entity);
  }

  @Test
  public void testGetCustomerById() throws MalformedURLException {
    CustomerEntity entity = createCustomerEntity();
    Long id = customerService.save(entity);

    CustomerEntity result = customerService.getCustomer(id);
    assertEquals(entity, result);
  }

  private CustomerEntity createCustomerEntity() throws MalformedURLException {
    CustomerEntity entity = new CustomerEntity();
    entity.setBillingAddress(createAddressEntity());
    entity.setBirthday(LocalDate.now().minus(2, ChronoUnit.DECADES));
    entity.setCompanyName("Company");
    entity.setDiscount(BigDecimal.ZERO);
    entity.setEmail("test@email.com");
    entity.setFaxNumber("010101101010110");
    entity.setName("First Last");
    entity.setNote(LOREM_IPSUM);
    entity.setPhoneNumber("0123456789");
    entity.setSex(Sex.FEMALE);
    entity.setWebAddress(URI.create("http://localhost").toURL());
    return entity;
  }
  
  private AddressEntity createAddressEntity() {
    return new AddressEntity()
        .setName("Abbey Fields")
        .setStreetAddress1("Karlsplatz 1")
        .setZipCode("1040")
        .setCity("Wien")
        .setState("Austria");
  }
}
