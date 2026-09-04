package dev.agiro.fanel.household.web;

import dev.agiro.fanel.household.api.HouseholdDto;
import dev.agiro.fanel.household.api.MemberDto;
import dev.agiro.fanel.household.domain.Household;
import dev.agiro.fanel.household.domain.MemberRole;
import dev.agiro.fanel.household.infra.HouseholdRepository;
import dev.agiro.fanel.household.infra.MemberRepository;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {
    private final HouseholdRepository households;
    private final MemberRepository members;
    private final ApplicationEventPublisher events;

    public HouseholdController(HouseholdRepository households, MemberRepository members, ApplicationEventPublisher events) {
        this.households = households;
        this.members = members;
        this.events = events;
    }

    @GetMapping
    public List<HouseholdDto> list() {
        return households.findAll().stream().map(HouseholdDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdDto create(@Valid @RequestBody CreateHousehold request) {
        Household household = households.save(new Household(request.name(), request.locale(), request.timezone()));
        events.publishEvent(new dev.agiro.fanel.household.api.HouseholdCreated(household.getId()));
        return HouseholdDto.from(household);
    }

    @GetMapping("/{id}")
    public HouseholdDto get(@PathVariable UUID id) {
        return HouseholdDto.from(find(id));
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberDto addMember(@PathVariable UUID id, @Valid @RequestBody CreateMember request) {
        Household household = find(id);
        return MemberDto.from(members.save(household.addMember(request.name(), request.role(), request.color())));
    }

    @GetMapping("/{id}/members")
    public List<MemberDto> listMembers(@PathVariable UUID id) {
        find(id);
        return members.findAllByHouseholdId(id).stream().map(MemberDto::from).toList();
    }

    private Household find(UUID id) {
        return households.findById(id).orElseThrow(() -> new EntityNotFoundException("Household not found: " + id));
    }

    public record CreateHousehold(@NotBlank String name, @NotBlank String locale, @NotBlank String timezone) {}
    public record CreateMember(@NotBlank String name, MemberRole role, String color) {}
}
