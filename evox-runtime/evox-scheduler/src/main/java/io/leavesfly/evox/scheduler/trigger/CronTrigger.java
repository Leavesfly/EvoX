package io.leavesfly.evox.scheduler.trigger;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
public class CronTrigger implements ITrigger {

    @Getter
    private final String cronExpression;
    private final Set<Integer> seconds;
    private final Set<Integer> minutes;
    private final Set<Integer> hours;
    private final Set<Integer> daysOfMonth;
    private final Set<Integer> months;
    private final Set<Integer> daysOfWeek;
    private Instant lastFireTime;
    private Instant nextFireTime;

    public CronTrigger(String cronExpression) {
        this.cronExpression = cronExpression;
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length < 6) {
            throw new IllegalArgumentException(
                    "Cron expression must have at least 6 fields: " + cronExpression);
        }
        this.seconds = parseCronField(parts[0], 0, 59);
        this.minutes = parseCronField(parts[1], 0, 59);
        this.hours = parseCronField(parts[2], 0, 23);
        this.daysOfMonth = parseCronField(parts[3], 1, 31);
        this.months = parseCronField(parts[4], 1, 12);
        this.daysOfWeek = parseCronField(parts[5], 0, 7);
        this.nextFireTime = calculateNextFireTime(Instant.now());
    }

    @Override
    public String getType() {
        return "cron";
    }

    @Override
    public Instant getNextFireTime() {
        return nextFireTime;
    }

    @Override
    public boolean shouldFire() {
        return nextFireTime != null && !Instant.now().isBefore(nextFireTime);
    }

    @Override
    public void onFired() {
        this.lastFireTime = Instant.now();
        this.nextFireTime = calculateNextFireTime(Instant.now().plusSeconds(1));
    }

    @Override
    public boolean isRepeating() {
        return true;
    }

    private Instant calculateNextFireTime(Instant from) {
        ZonedDateTime zdt = from.atZone(ZoneId.systemDefault()).withNano(0);

        for (int i = 0; i < 366 * 24 * 60; i++) {
            if (months.contains(zdt.getMonthValue())
                    && daysOfMonth.contains(zdt.getDayOfMonth())
                    && matchesDayOfWeek(zdt)
                    && hours.contains(zdt.getHour())
                    && minutes.contains(zdt.getMinute())
                    && seconds.contains(zdt.getSecond())) {
                return zdt.toInstant();
            }
            zdt = zdt.plusSeconds(1);
        }
        return null;
    }

    private boolean matchesDayOfWeek(ZonedDateTime zdt) {
        int dow = zdt.getDayOfWeek().getValue() % 7;
        return daysOfWeek.contains(dow) || daysOfWeek.contains(dow + 7);
    }

    private Set<Integer> parseCronField(String field, int min, int max) {
        Set<Integer> values = new TreeSet<>();
        if ("*".equals(field) || "?".equals(field)) {
            for (int i = min; i <= max; i++) {
                values.add(i);
            }
            return values;
        }

        for (String part : field.split(",")) {
            if (part.contains("/")) {
                String[] stepParts = part.split("/");
                int start = "*".equals(stepParts[0]) ? min : Integer.parseInt(stepParts[0]);
                int step = Integer.parseInt(stepParts[1]);
                for (int i = start; i <= max; i += step) {
                    values.add(i);
                }
            } else if (part.contains("-")) {
                String[] rangeParts = part.split("-");
                int start = Integer.parseInt(rangeParts[0]);
                int end = Integer.parseInt(rangeParts[1]);
                for (int i = start; i <= end; i++) {
                    values.add(i);
                }
            } else {
                values.add(Integer.parseInt(part));
            }
        }
        return values;
    }
}
