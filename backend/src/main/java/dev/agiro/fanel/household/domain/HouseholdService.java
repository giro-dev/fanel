package dev.agiro.fanel.household.domain;

import dev.agiro.fanel.household.api.HouseholdApi;
import dev.agiro.fanel.household.api.HouseholdCreated;
import dev.agiro.fanel.household.api.HouseholdDto;
import dev.agiro.fanel.household.api.MemberDto;
import dev.agiro.fanel.household.api.MemberRole;
import dev.agiro.fanel.household.infra.HouseholdRepository;
import dev.agiro.fanel.household.infra.MemberRepository;
import dev.agiro.fanel.shared.web.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class HouseholdService implements HouseholdApi {
    private final HouseholdRepository households;
    private final MemberRepository members;
    private final ApplicationEventPublisher events;

    public HouseholdService(HouseholdRepository households, MemberRepository members,
                            ApplicationEventPublisher events) {
        this.households = households;
        this.members = members;
        this.events = events;
    }

    @Override
    public HouseholdDto create(String name, String locale, String timezone) {
        Household household = households.save(new Household(name, locale, timezone));
        events.publishEvent(new HouseholdCreated(household.getId()));
        return toDto(household);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseholdDto> list() {
        return households.findAll().stream().map(HouseholdService::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HouseholdDto get(UUID id) {
        return toDto(find(id));
    }

    @Override
    public MemberDto addMember(UUID householdId, String name, MemberRole role, String color) {
        Household household = find(householdId);
        return toDto(members.save(household.addMember(name, role, color)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberDto> listMembers(UUID householdId) {
        find(householdId);
        return members.findAllByHouseholdId(householdId).stream().map(HouseholdService::toDto).toList();
    }

    @Override
    public MemberDto setMemberPin(UUID householdId, UUID memberId, String pin) {
        Member member = findMember(householdId, memberId);
        member.setPin(pin);
        return toDto(members.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyMemberPin(UUID householdId, UUID memberId, String pin) {
        return findMember(householdId, memberId).matchesPin(pin);
    }

    private Member findMember(UUID householdId, UUID memberId) {
        return members.findById(memberId)
                .filter(m -> m.getHousehold().getId().equals(householdId))
                .orElseThrow(() -> new EntityNotFoundException("Member not found: " + memberId));
    }

    private Household find(UUID id) {
        return households.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Household not found: " + id));
    }

    private static HouseholdDto toDto(Household household) {
        return new HouseholdDto(household.getId(), household.getName(), household.getLocale(),
                household.getTimezone(), household.getCreatedAt());
    }

    private static MemberDto toDto(Member member) {
        return new MemberDto(member.getId(), member.getHousehold().getId(), member.getName(),
                member.getRole(), member.getColor(), member.getCreatedAt());
    }
}
