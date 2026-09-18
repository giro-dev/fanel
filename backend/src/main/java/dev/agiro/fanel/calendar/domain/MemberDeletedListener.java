package dev.agiro.fanel.calendar.domain;

import dev.agiro.fanel.calendar.infra.CalendarEventRepository;
import dev.agiro.fanel.household.api.MemberDeleted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Drops a member from any calendar event it was assigned to, once it's removed. */
@Component("calendarMemberDeletedListener")
class MemberDeletedListener {
    private final CalendarEventRepository events;

    MemberDeletedListener(CalendarEventRepository events) {
        this.events = events;
    }

    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(MemberDeleted event) {
        events.removeAssignee(event.memberId().toString());
    }
}
