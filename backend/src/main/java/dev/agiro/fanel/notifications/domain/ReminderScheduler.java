package dev.agiro.fanel.notifications.domain;

import dev.agiro.fanel.calendar.api.CalendarApi;
import dev.agiro.fanel.calendar.api.CalendarEventDto;
import dev.agiro.fanel.chores.api.ChoreDto;
import dev.agiro.fanel.chores.api.ChoresApi;
import dev.agiro.fanel.household.api.HouseholdApi;
import dev.agiro.fanel.household.api.HouseholdDto;
import dev.agiro.fanel.household.api.MemberDto;
import dev.agiro.fanel.notifications.infra.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Once a day, at {@code fanel.notifications.reminder-hour} in each household's own timezone, sends a
 * Web Push reminder to members with chores due today or calendar events happening today.
 */
@Component
public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final HouseholdApi household;
    private final ChoresApi chores;
    private final CalendarApi calendar;
    private final PushSubscriptionRepository subscriptions;
    private final PushSender pushSender;
    private final MessageSource messages;
    private final int reminderHour;

    public ReminderScheduler(HouseholdApi household, ChoresApi chores, CalendarApi calendar,
                             PushSubscriptionRepository subscriptions, PushSender pushSender, MessageSource messages,
                             @Value("${fanel.notifications.reminder-hour:8}") int reminderHour) {
        this.household = household;
        this.chores = chores;
        this.calendar = calendar;
        this.subscriptions = subscriptions;
        this.pushSender = pushSender;
        this.messages = messages;
        this.reminderHour = reminderHour;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sendDueReminders() {
        Instant now = Instant.now();
        for (HouseholdDto householdDto : household.list()) {
            ZonedDateTime local;
            try {
                local = now.atZone(ZoneId.of(householdDto.timezone()));
            } catch (ZoneRulesException e) {
                log.warn("Unknown timezone {} for household {}", householdDto.timezone(), householdDto.id());
                continue;
            }
            if (local.getHour() != reminderHour) continue;
            sendRemindersFor(householdDto, local.toLocalDate());
        }
    }

    private void sendRemindersFor(HouseholdDto householdDto, LocalDate today) {
        Map<UUID, List<String>> choreTitlesByMember = new HashMap<>();
        for (ChoreDto chore : chores.list(householdDto.id())) {
            if (chore.assigneeId() != null && !chore.done() && today.equals(chore.dueDate())) {
                choreTitlesByMember.computeIfAbsent(chore.assigneeId(), key -> new ArrayList<>()).add(chore.title());
            }
        }

        List<MemberDto> members = household.listMembers(householdDto.id());
        Map<UUID, List<String>> eventTitlesByMember = new HashMap<>();
        for (CalendarEventDto event : calendar.list(householdDto.id(), today, today)) {
            List<UUID> targets = event.assigneeIds().isEmpty()
                    ? members.stream().map(MemberDto::id).toList()
                    : event.assigneeIds();
            for (UUID memberId : targets) {
                eventTitlesByMember.computeIfAbsent(memberId, key -> new ArrayList<>()).add(event.title());
            }
        }

        Set<UUID> memberIds = new HashSet<>();
        memberIds.addAll(choreTitlesByMember.keySet());
        memberIds.addAll(eventTitlesByMember.keySet());
        if (memberIds.isEmpty()) return;

        Locale locale = Locale.forLanguageTag(householdDto.locale());
        String title = messages.getMessage("notifications.reminder.title", null, "Fanel", locale);
        for (UUID memberId : memberIds) {
            List<PushSubscription> memberSubscriptions = subscriptions.findByHouseholdIdAndMemberId(householdDto.id(), memberId);
            if (memberSubscriptions.isEmpty()) continue;
            String body = buildBody(choreTitlesByMember.getOrDefault(memberId, List.of()),
                    eventTitlesByMember.getOrDefault(memberId, List.of()), locale);
            for (PushSubscription subscription : memberSubscriptions) {
                pushSender.send(subscription, title, body);
            }
        }
    }

    private String buildBody(List<String> choreTitles, List<String> eventTitles, Locale locale) {
        List<String> parts = new ArrayList<>();
        if (!choreTitles.isEmpty()) {
            parts.add(messages.getMessage("notifications.reminder.chores",
                    new Object[]{String.join(", ", choreTitles)}, locale));
        }
        if (!eventTitles.isEmpty()) {
            parts.add(messages.getMessage("notifications.reminder.events",
                    new Object[]{String.join(", ", eventTitles)}, locale));
        }
        return String.join(" \u00b7 ", parts);
    }
}
