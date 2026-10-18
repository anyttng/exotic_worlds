package com.exoticworlds.compat.sable;

import org.jspecify.annotations.Nullable;

public interface SableMotorGoalHolder {
    @Nullable SableMotorGoal exotic_worlds$motorGoal();

    void exotic_worlds$motorGoal(@Nullable SableMotorGoal goal);
}
