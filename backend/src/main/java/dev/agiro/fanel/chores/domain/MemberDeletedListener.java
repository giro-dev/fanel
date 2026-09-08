package dev.agiro.fanel.chores.domain;

import dev.agiro.fanel.chores.infra.ChoreRepository;
import dev.agiro.fanel.household.api.MemberDeleted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Un-assigns chores from a member once it's removed, instead of keeping a stale reference. */
@Component("choresMemberDeletedListener")
class MemberDeletedListener {
    private final ChoreRepository chores;

    MemberDeletedListener(ChoreRepository chores) {
        this.chores = chores;
    }

    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(MemberDeleted event) {
        chores.clearAssignee(event.memberId());
    }
}
