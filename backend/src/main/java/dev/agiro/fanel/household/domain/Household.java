package dev.agiro.fanel.household.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "household")
public class Household extends UuidEntity {
    @NotBlank
    private String name;
    private String locale;
    private String timezone;
    private Instant createdAt;

    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Member> members = new ArrayList<>();

    protected Household() {}

    public Household(String name, String locale, String timezone) {
        this.name = name;
        this.locale = locale;
        this.timezone = timezone;
        this.createdAt = Instant.now();
    }

    public String getName() { return name; }
    public String getLocale() { return locale; }
    public String getTimezone() { return timezone; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Member> getMembers() { return members; }

    public Member addMember(String name, MemberRole role, String color) {
        Member member = new Member(this, name, role, color);
        members.add(member);
        return member;
    }
}
