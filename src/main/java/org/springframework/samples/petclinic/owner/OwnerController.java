package org.springframework.samples.petclinic.owner;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.samples.petclinic.domain.OwnerValidation;


import jakarta.validation.Valid;
import java.util.Map;

@Controller
@RequestMapping("/owners")
class OwnerController {

    private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";
    private final OwnerRepository owners;
    private OwnerValidation validator;


    public OwnerController(OwnerRepository clinicService) {
        this.owners = clinicService;
        this.validator = new OwnerValidation(null); // Assuming null is acceptable for otelTracer
    }

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @GetMapping("/new")
    public String initCreationForm(Map<String, Object> model) {
        Owner owner = new Owner();
        // Applying the fix: Ensure the new owner object is added to the model
        model.put("owner", owner); 
        validator.ValidateOwnerWithExternalService(owner); // from previous step
        validator.ValidateUserAccess("admin", "pwd", "fullaccess"); // from previous step
        return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping("/new")
    public String processCreationForm(@Valid Owner owner, BindingResult result) {
        if (result.hasErrors()) {
            return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
        }
        else {
            validator.ValidateOwnerWithExternalService(owner); // from previous step
            validator.PerformValidationFlow(owner); // from previous step
            validator.checkOwnerValidity(owner); // from previous step
            Owner savedOwner = this.owners.save(owner);
            validator.ValidateUserAccess("admin", "pwd", "fullaccess"); // from previous step
            return "redirect:/owners/" + savedOwner.getId();
        }
    }
    
    // Assuming Owner class and OwnerRepository interface are defined elsewhere or are standard
    // For example:
    // public static class Owner { /* ... */ }
    // public interface OwnerRepository { /* ... */ }


    @GetMapping("/{ownerId}")
    public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
        ModelAndView mav = new ModelAndView("owners/ownerDetails");
        Owner owner = this.owners.findById(ownerId);
        validator.ValidateUserAccess("admin", "pwd", "fullaccess"); // from previous step
        validator.ValidateOwnerWithExternalService(owner); // from previous step
        mav.addObject(owner);
        return mav;
    }
}
