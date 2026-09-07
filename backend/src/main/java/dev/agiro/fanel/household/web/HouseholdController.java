package dev.agiro.fanel.household.web;

import dev.agiro.fanel.household.api.HouseholdDto;
import dev.agiro.fanel.household.api.MemberDto;
import dev.agiro.fanel.household.api.MemberRole;
import dev.agiro.fanel.household.domain.HouseholdService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {
    private final HouseholdService service;

    public HouseholdController(HouseholdService service) {
        this.service = service;
    }

    @GetMapping
    public List<HouseholdDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdDto create(@Valid @RequestBody CreateHousehold request) {
        return service.create(request.name(), request.locale(), request.timezone());
    }

    @GetMapping("/{id}")
    public HouseholdDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberDto addMember(@PathVariable UUID id, @Valid @RequestBody CreateMember request) {
        return service.addMember(id, request.name(), request.role(), request.color());
    }

    @GetMapping("/{id}/members")
    public List<MemberDto> listMembers(@PathVariable UUID id) {
        return service.listMembers(id);
    }

    @PutMapping("/{id}/members/{memberId}/pin")
    public MemberDto setPin(@PathVariable UUID id, @PathVariable UUID memberId,
                            @RequestBody SetPin request) {
        return service.setMemberPin(id, memberId, request.pin());
    }

    @PostMapping("/{id}/members/{memberId}/verify-pin")
    public Map<String, Boolean> verifyPin(@PathVariable UUID id, @PathVariable UUID memberId,
                                          @RequestBody SetPin request) {
        return Map.of("valid", service.verifyMemberPin(id, memberId, request.pin()));
    }

    public record CreateHousehold(@NotBlank String name, @NotBlank String locale, @NotBlank String timezone) {}
    public record CreateMember(@NotBlank String name, MemberRole role, String color) {}
    public record SetPin(String pin) {}
}
