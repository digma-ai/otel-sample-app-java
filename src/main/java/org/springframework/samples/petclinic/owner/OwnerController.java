package org.springframework.samples.petclinic.owner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owners")
public class OwnerController {

    private final JdbcTemplate jdbcTemplate;

    public OwnerController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{ownerId}/pets")
    public Map<Integer, List<Integer>> getOwnerPetsMap(@PathVariable("ownerId") int ownerId) {
        String sql = "SELECT owner_id, pet_id FROM owners_pets WHERE owner_id = ?";
        return jdbcTemplate.queryForList(sql, ownerId).stream()
            .collect(Collectors.groupingBy(
                row -> (Integer) row.get("owner_id"),
                Collectors.mapping(
                    row -> (Integer) row.get("pet_id"),
                    Collectors.toList()
                )
            ));
    }
}