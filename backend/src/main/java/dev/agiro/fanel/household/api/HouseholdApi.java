package dev.agiro.fanel.household.api;

import java.util.List;
import java.util.UUID;

public interface HouseholdApi {
    HouseholdDto create(String name, String locale, String timezone);
    List<HouseholdDto> list();
    HouseholdDto get(UUID id);
    MemberDto addMember(UUID householdId, String name, MemberRole role, String color);
    List<MemberDto> listMembers(UUID householdId);
    MemberDto setMemberPin(UUID householdId, UUID memberId, String pin);
    boolean verifyMemberPin(UUID householdId, UUID memberId, String pin);
}
