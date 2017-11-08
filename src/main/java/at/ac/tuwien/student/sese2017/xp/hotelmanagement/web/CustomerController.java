package at.ac.tuwien.student.sese2017.xp.hotelmanagement.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class CustomerController {

  @RequestMapping("/customer/index")
  public String userIndex() {
    log.info("customer index - Page called");
    return "customer/index"; //path to the template to call
  }
}
