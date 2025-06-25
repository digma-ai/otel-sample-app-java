package org.springframework.samples.petclinic.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;

@Controller
class WelcomeController {

    private static final String WELCOME_VIEW = "welcome";
    
    @GetMapping("/")
    @Cacheable(value = "welcomePageCache", sync = true)
    public ModelAndView welcome(Model model) {
        // Using ModelAndView for better performance as it pre-allocates view name
        return new ModelAndView(WELCOME_VIEW);
    }
}