/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.domain.OwnerValidation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controllerclass OwnerController implements InitializingBean {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private OwnerValidation validator;

	@Autowired
	private OpenTelemetry openTelemetry;

	private Tracer otelTracer;
	@Autowired
	private OwnerRepository ownerRepository;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.otelTracer = openTelemetry.getTracer("OwnerController");

		validator = new OwnerValidation(this.otelTracer);
	}

	private final OwnerRepository owners;
	private final JdbcTemplate jdbcTemplate;

	public OwnerController(OwnerRepository clinicService, JdbcTemplate jdbcTemplate

	) {
		this.owners = clinicService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}@ModelAttribute("owner")
	public Owner findOwner(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		return ownerId == null ? new Owner() : this.owners.findById(ownerId);
	}

	@GetMapping("/owners/new")
	public String initCreationForm(Map<String, Object> model) {
		Owner owner = new Owner();
		validator.ValidateOwnerWithExternalService(owner);
		model.put("owner", owner);

		validator.ValidateUserAccess("admin", "pwd", "fullaccess");

		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/owners/new")
	public String processCreationForm(@Valid Owner owner, BindingResult result) {
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		validator.ValidateOwnerWithExternalService(owner);
		validator.PerformValidationFlow(owner);

		validator.checkOwnerValidity(owner);
		this.owners.save(owner);
		validator.ValidateUserAccess("admin", "pwd", "fullaccess");
		return "redirect:/owners/" + owner.getId();
	}@GetMapping("/owners/find")
	public String initFindForm() {
		return "owners/findOwners";
	}

	@GetMapping("/owners")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, Owner owner, BindingResult result,
			Model model) {

		validator.ValidateUserAccess("admin", "pwd", "fullaccess");

		// allow parameterless GET request for /owners to return all records
		if (owner.getLastName() == null) {
			owner.setLastName(""); // empty string signifies broadest possible search
		}

		// find owners by last name
		Page<Owner> ownersResults = findPaginatedForOwnersLastName(page, owner.getLastName());
		if (ownersResults.isEmpty()) {
			// no owners found
			result.rejectValue("lastName", "notFound", "not found");
			return "owners/findOwners";
		}

		if (ownersResults.getTotalElements() == 1) {
			// 1 owner found
			owner = ownersResults.iterator().next();
			return "redirect:/owners/" + owner.getId();
		}// multiple owners found
		return addPaginationModel(page, model, ownersResults);
	}

	@WithSpan
	private String addPaginationModel(int page, Model model, Page<Owner> paginated) {
		// throw new RuntimeException();
		model.addAttribute("listOwners", paginated);
		List<Owner> listOwners = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listOwners", listOwners);
		return "owners/ownersList";
	}

	@WithSpan()
	private Page<Owner> findPaginatedForOwnersLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return owners.findByLastName(lastname, pageable);
	}@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm(@PathVariable("ownerId") int ownerId, Model model) {
		Owner owner = this.owners.findById(ownerId);
		var petCount = ownerRepository.countPets(owner.getId());
		var totalVisits = owner.getPets().stream().mapToLong(pet-> pet.getVisits().size())
			.sum();
		var averageVisits = petCount > 0 ? totalVisits / petCount : 0;
		model.addAttribute(owner);
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	private static void delay(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.interrupted();
		}
	}

	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid Owner owner, BindingResult result,
			@PathVariable("ownerId") int ownerId) {
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		owner.setId(ownerId);
		validator.checkOwnerValidity(owner);

		validator.ValidateOwnerWithExternalService(owner);
validator.PerformValidationFlow(owner);
		this.owners.save(owner);
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Custom handler for displaying an owner.
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
		validator.ValidateUserAccess("admin", "pwd", "fullaccess");

		ModelAndView mav = new ModelAndView("owners/ownerDetails");
		Owner owner = this.owners.findById(ownerId);
		validator.ValidateOwnerWithExternalService(owner);

		mav.addObject(owner);
		return mav;
	}

	@GetMapping("/owners/{ownerId}/pets")
	@ResponseBody
	public String getOwnerPetsMap(@PathVariable("ownerId") int ownerId) {
		String sql = "SELECT p.id AS pet_id, p.owner_id AS owner_id FROM pets p JOIN owners o ON p.owner_id = o.id";

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);Map<Integer, List<Integer>> ownerToPetsMap = rows.stream()
			.collect(Collectors.toMap(
				row -> (Integer) row.get("owner_id"),
				row -> List.of((Integer) row.get("pet_id"))  // Immutable list
			));


		List<Integer> pets = ownerToPetsMap.get(ownerId);

		if (pets == null || pets.isEmpty()) {
			return "No pets found for owner " + ownerId;
		}

		return "Pets for owner " + ownerId + ": " + pets.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(", "));

	}
}