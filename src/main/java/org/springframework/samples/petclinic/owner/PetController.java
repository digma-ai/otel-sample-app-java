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

import java.time.LocalDate;
import java.util.Collection;import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
@RequestMapping("/owners/{ownerId}")class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private final OwnerRepository owners;

	public PetController(OwnerRepository owners) {
		this.owners = owners;
	}

	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.owners.findPetTypes();
	}

	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable("ownerId") int ownerId) {
		Owner owner = this.owners.findById(ownerId);
		if (owner == null) {
			throw new IllegalArgumentException("Owner ID not found: " + ownerId);
		}
		return owner;
	}

	@ModelAttribute("pet")
	public Pet findPet(@PathVariable("ownerId") int ownerId,
			@PathVariable(name = "petId", required = false) Integer petId) {
		// Validate owner existence
		Owner owner = this.owners.findById(ownerId);
		if (owner == null) {
			throw new IllegalArgumentException("Owner ID not found: " + ownerId);
		}

		// Check for duplicate pet ID if petId is provided
		if (petId != null) {
			Pet pet = owner.getPet(petId);
			if (pet == null) {
				throw new IllegalArgumentException("Pet ID not found: " + petId);
			}
			return pet;
		}
		return new Pet();
	}@InitBinder("owner")
public void initOwnerBinder(WebDataBinder dataBinder) {
	dataBinder.setDisallowedFields("id");
}

@InitBinder("pet")
public void initPetBinder(WebDataBinder dataBinder) {
	dataBinder.setValidator(new PetValidator());
}

@GetMapping("/pets/new")
public String initCreationForm(Owner owner, ModelMap model) {
	Pet pet = new Pet();
	owner.addPet(pet);
	model.put("pet", pet);
	return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
}

@PostMapping("/pets/new")
public String processCreationForm(Owner owner, @Valid Pet pet, BindingResult result, ModelMap model) {
	// Validate for duplicate pet name within the same owner
	if (StringUtils.hasText(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null) {
		result.rejectValue("name", "duplicate", "A pet with this name already exists for this owner");
	}

	// Validate that birth date is not in the future
	LocalDate currentDate = LocalDate.now();
	if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
		result.rejectValue("birthDate", "typeMismatch.birthDate", "Birth date cannot be in the future");
	}
owner.addPet(pet);
		if (result.hasErrors()) {
			model.put("pet", pet);
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		this.owners.save(owner);
		return "redirect:/owners/{ownerId}";
	}

	@GetMapping("/pets/{petId}/edit")
	// Initializes the form for updating an existing pet
	public String initUpdateForm(Owner owner, @PathVariable("petId") int petId, ModelMap model) {
		// Validate pet exists for the owner
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException("Pet with ID " + petId + " not found");
		}
		model.put("pet", pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/{petId}/edit")
	// Processes the pet update form with validation
	public String processUpdateForm(@Valid Pet pet, BindingResult result, Owner owner, ModelMap model) {
		// Validate pet ID exists
		if (pet.getId() == null) {
			result.rejectValue("id", "required", "Pet ID is required");
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		String petName = pet.getName();

		// Comprehensive duplicate name validation
		if (StringUtils.hasText(petName)) {
			Pet existingPet = owner.getPet(petName.toLowerCase(), false);
			if (existingPet != null && existingPet.getId() != pet.getId()) {
				result.rejectValue("name", "duplicate", "A pet with name '" + petName + "' already exists for this owner");
			}
		} else {
			result.rejectValue("name", "required", "Pet name is required");
		}LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			model.put("pet", pet);
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		owner.addPet(pet);
		this.owners.save(owner);
		return "redirect:/owners/{ownerId}";
	}

}