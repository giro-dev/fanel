package dev.agiro.fanel.household.api;

import java.util.List;
import java.util.UUID;

public interface HouseholdApi {
    HouseholdDto create(String name, String locale, String timezone);
    List<HouseholdDto> list();
    HouseholdDto get(UUID id);
    MemberDto addMember(UUID householdId, String name, MemberRole role, String color);
    List<MemberDto> listMembers(UUID householdId);
    MemberDto getMember(UUID householdId, UUID memberId);
    MemberDto updateMember(UUID householdId, UUID memberId, String name, String color);
    void deleteMember(UUID householdId, UUID memberId);
    MemberDto setMemberPin(UUID householdId, UUID memberId, String pin);
    boolean verifyMemberPin(UUID householdId, UUID memberId, String pin);
    MemberDto setMemberCredentials(UUID householdId, UUID memberId, String username, String rawPassword);
    MemberDto setMemberRole(UUID householdId, UUID memberId, MemberRole role);
    MemberDto setMemberGuardians(UUID householdId, UUID childId, List<UUID> guardianIds);

    /** Children that the given adult/admin member is responsible for. Empty for children or unknown members. */
    List<UUID> relatedChildIds(UUID householdId, UUID memberId);
}
