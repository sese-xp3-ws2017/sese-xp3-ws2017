package at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

import org.hibernate.annotations.ColumnDefault;

/**
 * Representation of a reservation.
 * @author johannes
 */
@Data
@Entity
public class ReservationEntity {

  /**
   * Unique id of a reservation.
   */
  @Id
  @GeneratedValue
  private Long reservationId;
  
  @OneToMany(cascade = {CascadeType.ALL})
  @Size(min = 1)
  private List<ReservationRoomBooking> rooms;

  @OneToMany
  @Size(min = 1)
  private List<CustomerEntity> customers;
  
  @Column
  private LocalDateTime startTime;
  
  @Column
  private LocalDateTime endTime;
  
  @Column
  @NotNull
  @ColumnDefault("0")
  @Digits(fraction = 2, integer = 3)
  @Min(value = 0)
  @Max(value = 100)
  private BigDecimal discount;

  @Column
  private Double price;
}