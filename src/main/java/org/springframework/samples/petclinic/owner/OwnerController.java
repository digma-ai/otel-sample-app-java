@GetMapping("/owners/{ownerId}/pets")
@ResponseBody
public String getOwnerPetsMap(@PathVariable("ownerId") int ownerId) {
    String sql = "SELECT p.id AS pet_id, p.owner_id AS owner_id FROM pets p JOIN owners o ON p.owner_id = o.id";

    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

    Map<Integer, List<Integer>> ownerToPetsMap = rows.stream()
        .collect(Collectors.groupingBy(
            row -> (Integer) row.get("owner_id"),
            Collectors.mapping(
                row -> (Integer) row.get("pet_id"),
                Collectors.toList()
            )
        ));

    List<Integer> pets = ownerToPetsMap.get(ownerId);

    if (pets == null || pets.isEmpty()) {
        return "No pets found for owner " + ownerId;
    }

    return "Pets for owner " + ownerId + ": " + pets.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(", "));
}