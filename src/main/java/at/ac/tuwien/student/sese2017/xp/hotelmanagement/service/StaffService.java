package at.ac.tuwien.student.sese2017.xp.hotelmanagement.service;

import at.ac.tuwien.student.sese2017.xp.hotelmanagement.auth.AuthenticationFacade;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.auth.PasswordManager;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.Role;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.StaffEntity;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.UserEntity;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.VacationEntity;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.data.VacationStatus;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.dto.StaffEmployment;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.repository.StaffRepository;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.repository.UserRepository;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.domain.repository.VacationRepository;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.exceptions.ForbiddenException;
import at.ac.tuwien.student.sese2017.xp.hotelmanagement.exceptions.NotEnoughVacationDaysException;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@Slf4j
public class StaffService {

  private static final int DEFAULT_YEARLY_VACATION_DAYS = 20;
  private final VacationRepository vacationRepository;
  private final UserRepository userRepository;
  private final StaffRepository staffRepository;
  private final AuthenticationFacade authFacade;
  private PasswordManager passwordManager;

  @Autowired
  public StaffService(VacationRepository vacationRepository,
      UserRepository userRepository,
      StaffRepository staffRepository,
      AuthenticationFacade authFacade,
      PasswordManager passwordManager) {
    this.vacationRepository = vacationRepository;
    this.userRepository = userRepository;
    this.staffRepository = staffRepository;
    this.authFacade = authFacade;
    this.passwordManager = passwordManager;
  }

  public StaffEmployment create(@Valid StaffEntity entity) {
    if(entity == null) {
      throw new IllegalArgumentException("StaffEntity cannot be null!");
    }
    List<UserEntity> manager = userRepository
        .findByUsername(authFacade.getAuthentication().getName());
    if(manager.size() != 1) {
      throw new IllegalStateException("Did not find exactly 1 manager with username " + 
          authFacade.getAuthentication().getName() + "!");
    }
    UserEntity managerEntity = manager.get(0);
    if(!managerEntity.getRoles().contains(Role.MANAGER)) {
      throw new ForbiddenException(Role.MANAGER, managerEntity.getId(), "creating staffer");
    }
    StaffEmployment employment = new StaffEmployment();
    String clearTextPassword = passwordManager.generatePassword();
    employment.setClearTextPassword(clearTextPassword);
    entity.setUsername(entity.getEmail());
    entity.setPassword(passwordManager.encodePassword(clearTextPassword));
    entity.setRoles(entity.getJobTitle().getRoles());
    if(entity.getYearlyVacationDays() == null || entity.getYearlyVacationDays().isEmpty()) {
      //FIXME make default yearly vacation days configurable if customer wants them to be
      Map<Integer, Integer> yearlyVacationDays = new HashMap<>();
      yearlyVacationDays.put(LocalDate.now().getYear(), DEFAULT_YEARLY_VACATION_DAYS);
      entity.setYearlyVacationDays(yearlyVacationDays);
    }
    Long id = staffRepository.save(entity).getId();
    employment.setId(id);
    return employment;
  }

  //FIXME set Transaction level otherwise conflicts could be created
  public Long requestVacation(@Valid VacationEntity vacation) throws NotEnoughVacationDaysException {
    StaffEntity requester = vacation.getStaffer();
    
    if(vacation.getResolution() == null) {
      vacation.setResolution(VacationStatus.PENDING);
    } else if(!vacation.getResolution().equals(VacationStatus.PENDING)) {
      throw new IllegalArgumentException("Vacation request already resolved");
    }
    
    if(requester == null) {
      throw new IllegalArgumentException("Vacation not requested by anyone");
    }
    
    if(vacation.getVacationDays() < 1) {
      throw new IllegalArgumentException("Cannot request a vacation with " + vacation.getVacationDays() + " days");
    }
        
    //Calculate available vacation days up until and including this year
    //Future reductions in vacation days could lead to illegal vacations 
    Integer targetYear = vacation.getToDate().getYear();
    //Sort yearlyVacationDays by year
    NavigableMap<Integer, Integer> yearlyVacationDays = new TreeMap<>(requester.getYearlyVacationDays());
    
    if(yearlyVacationDays.isEmpty()) {
      throw new IllegalStateException("Staffer " + requester.getId() + " does not have any vacation days set");
    } else if(yearlyVacationDays.firstKey() > targetYear) {
      throw new IllegalStateException("No vacation days set before " + yearlyVacationDays.firstKey());
    }
    
    if(vacation.getToDate().isBefore(vacation.getFromDate())) {
      throw new IllegalArgumentException("Vacation cannot end before it starts");
    }
    
    int maxDays = (int)ChronoUnit.DAYS.between(vacation.getFromDate(), vacation.getToDate()) + 1;
    if(vacation.getVacationDays() > maxDays) {
      throw new IllegalArgumentException("Requested too many days for vacation period of max. " + maxDays + " days");
    }

    log.info("Staffer {} request {} days of vacation from {} till {}"
        , requester.getId(), vacation.getVacationDays(), vacation.getFromDate(), vacation.getToDate());
    
    //Calculate total days of accepted and pending vacation requests 
    Integer totalUsedVacationDays = requester.getVacations()
        .stream().filter(v -> v.getResolution().equals(VacationStatus.ACCEPTED) 
            || v.getResolution().equals(VacationStatus.PENDING))
        .reduce(0, (totalDays, v) -> totalDays + v.getVacationDays(), (l, r) -> l + r);
    
    //Calculate total number of vacation days possible up until and including the target year
    Integer totalPossibleVacationDays = yearlyVacationDays.descendingMap().entrySet().stream()
        .filter(year -> year.getKey() < targetYear)
        .reduce(new AbstractMap.SimpleEntry<Integer,Integer>(targetYear, 0)
            , (prevYear, currentYear) -> new AbstractMap.SimpleEntry<Integer,Integer>(currentYear.getKey()
                  , prevYear.getValue() + currentYear.getValue() * (prevYear.getKey() - currentYear.getKey())
                  )
        ).getValue();
    Integer sameYearDays = yearlyVacationDays.get(targetYear);
    if(sameYearDays != null) {
      totalPossibleVacationDays += sameYearDays;
    }
    
    Integer leftVacationDays = totalPossibleVacationDays - totalUsedVacationDays;
    
    //Check if vacation uses up more than the available vacation days
    if(leftVacationDays - vacation.getVacationDays() < 0) {
      throw new NotEnoughVacationDaysException(requester.getId(), leftVacationDays, targetYear);
    }
    
    vacationRepository.save(vacation);
    
    return vacation.getId();
  }

  public void confirmVacation(Long vacationId) {
    Optional<VacationEntity> vacationOpt = vacationRepository.findById(vacationId);
    if(!vacationOpt.isPresent()) {
      throw new IllegalArgumentException("Vacation with id " + vacationId + " does not exist!");
    }
    VacationEntity vacation = vacationOpt.get();
    if(!vacation.getResolution().equals(VacationStatus.PENDING)) {
      throw new IllegalStateException("Cannot confirm a vacation that is already in state " + vacation.getResolution() + "!");
    }
    vacation.setManager(tryGetCurrentUserAsManager());
    vacation.setResolution(VacationStatus.ACCEPTED);
    vacationRepository.save(vacation);
  }

  public void rejectVacation(Long vacationId, String reason) {
    if(reason == null || reason.equals("")) {
      throw new IllegalArgumentException("Reason cannot be empty!");
    }
    Optional<VacationEntity> vacationOpt = vacationRepository.findById(vacationId);
    if(!vacationOpt.isPresent()) {
      throw new IllegalArgumentException("Vacation with id " + vacationId + " does not exist!");
    }
    VacationEntity vacation = vacationOpt.get();
    if(!vacation.getResolution().equals(VacationStatus.PENDING)) {
      throw new IllegalStateException("Cannot reject a vacation that is already in state " + vacation.getResolution() + "!");
    }
    vacation.setManager(tryGetCurrentUserAsManager());
    vacation.setResolution(VacationStatus.REJECTED);
    vacation.setReason(reason);
    vacationRepository.save(vacation);
  }

  private StaffEntity tryGetCurrentUserAsManager() {
    Authentication authentication = authFacade.getAuthentication();
    StaffEntity manager;
    if (!(authentication instanceof AnonymousAuthenticationToken) && authentication != null) {
      manager = Optional.ofNullable(
          userRepository.findByUsername(authentication.getName()))
          .map(us -> {
            if(us.isEmpty()) {
              return null;
            } else {
              return us.get(0).getId();
            }
          })
          .flatMap(u -> staffRepository.findById(u))
          .orElseThrow(() -> new IllegalStateException("Could not find manager by currently logged in user name!"));
    } else {
      throw new IllegalStateException("Anonymous users cannot confirm vacations!");
    }

    if(!manager.getRoles().contains(Role.MANAGER)) {
      throw new ForbiddenException(Role.MANAGER, manager.getId(), "staff changes");
    }
    return manager;
  }
}
