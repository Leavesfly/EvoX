package io.leavesfly.evox.tools.api;

import io.leavesfly.evox.tools.base.BaseTool;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 时间日期工具
 * 提供日期时间相关的操作
 * 
 * @author EvoX Team
 */
@Slf4j
public class DateTimeTool extends BaseTool {

    private static final Map<String, ZoneId> TIMEZONE_ALIASES = new HashMap<>();
    
    static {
        // 常用时区别名
        TIMEZONE_ALIASES.put("北京", ZoneId.of("Asia/Shanghai"));
        TIMEZONE_ALIASES.put("上海", ZoneId.of("Asia/Shanghai"));
        TIMEZONE_ALIASES.put("中国", ZoneId.of("Asia/Shanghai"));
        TIMEZONE_ALIASES.put("东京", ZoneId.of("Asia/Tokyo"));
        TIMEZONE_ALIASES.put("日本", ZoneId.of("Asia/Tokyo"));
        TIMEZONE_ALIASES.put("首尔", ZoneId.of("Asia/Seoul"));
        TIMEZONE_ALIASES.put("韩国", ZoneId.of("Asia/Seoul"));
        TIMEZONE_ALIASES.put("纽约", ZoneId.of("America/New_York"));
        TIMEZONE_ALIASES.put("洛杉矶", ZoneId.of("America/Los_Angeles"));
        TIMEZONE_ALIASES.put("伦敦", ZoneId.of("Europe/London"));
        TIMEZONE_ALIASES.put("巴黎", ZoneId.of("Europe/Paris"));
        TIMEZONE_ALIASES.put("悉尼", ZoneId.of("Australia/Sydney"));
        TIMEZONE_ALIASES.put("新加坡", ZoneId.of("Asia/Singapore"));
        TIMEZONE_ALIASES.put("香港", ZoneId.of("Asia/Hong_Kong"));
        TIMEZONE_ALIASES.put("台北", ZoneId.of("Asia/Taipei"));
    }
    
    public DateTimeTool() {
        this.name = "datetime";
        this.description = "获取当前时间、计算日期差、格式化日期、时区转换等时间日期相关操作";
        
        this.inputs = new HashMap<>();
        
        Map<String, String> operationDef = new HashMap<>();
        operationDef.put("type", "string");
        operationDef.put("description", "操作类型: 'now'(当前时间), 'format'(格式化), 'diff'(日期差), 'add'(日期加减), 'convert'(时区转换), 'parse'(解析日期)");
        inputs.put("operation", operationDef);
        
        Map<String, String> dateDef = new HashMap<>();
        dateDef.put("type", "string");
        dateDef.put("description", "日期字符串，格式如 '2024-01-15' 或 '2024-01-15 14:30:00'");
        inputs.put("date", dateDef);
        
        Map<String, String> date2Def = new HashMap<>();
        date2Def.put("type", "string");
        date2Def.put("description", "第二个日期字符串（用于计算日期差）");
        inputs.put("date2", date2Def);
        
        Map<String, String> formatDef = new HashMap<>();
        formatDef.put("type", "string");
        formatDef.put("description", "日期格式，如 'yyyy-MM-dd' 或 'yyyy年MM月dd日'");
        inputs.put("format", formatDef);
        
        Map<String, String> timezoneDef = new HashMap<>();
        timezoneDef.put("type", "string");
        timezoneDef.put("description", "时区，如 'Asia/Shanghai', '北京', 'America/New_York'");
        inputs.put("timezone", timezoneDef);
        
        Map<String, String> amountDef = new HashMap<>();
        amountDef.put("type", "integer");
        amountDef.put("description", "增减数量（用于日期加减）");
        inputs.put("amount", amountDef);
        
        Map<String, String> unitDef = new HashMap<>();
        unitDef.put("type", "string");
        unitDef.put("description", "时间单位: 'days', 'weeks', 'months', 'years', 'hours', 'minutes'");
        inputs.put("unit", unitDef);
        
        this.required = List.of("operation");
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            validateParameters(parameters);
            
            String operation = getParameter(parameters, "operation", "now");
            
            return switch (operation.toLowerCase()) {
                case "now" -> executeNow(parameters);
                case "format" -> executeFormat(parameters);
                case "diff" -> executeDiff(parameters);
                case "add" -> executeAdd(parameters);
                case "convert" -> executeConvert(parameters);
                case "parse" -> executeParse(parameters);
                default -> ToolResult.failure("不支持的操作: " + operation);
            };
            
        } catch (Exception e) {
            log.error("时间日期操作失败", e);
            return ToolResult.failure("操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前时间
     */
    private ToolResult executeNow(Map<String, Object> parameters) {
        String timezone = getParameter(parameters, "timezone", "Asia/Shanghai");
        String format = getParameter(parameters, "format", "yyyy-MM-dd HH:mm:ss");
        
        ZoneId zoneId = resolveZone(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("datetime", now.format(DateTimeFormatter.ofPattern(format)));
        result.put("date", now.format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.put("time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        result.put("timestamp", now.toInstant().toEpochMilli());
        result.put("timezone", zoneId.getId());
        result.put("day_of_week", now.getDayOfWeek().toString());
        result.put("day_of_week_cn", getDayOfWeekChinese(now.getDayOfWeek()));
        result.put("year", now.getYear());
        result.put("month", now.getMonthValue());
        result.put("day", now.getDayOfMonth());
        result.put("hour", now.getHour());
        result.put("minute", now.getMinute());
        result.put("second", now.getSecond());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("operation", "now");
        metadata.put("timezone", zoneId.getId());
        
        return ToolResult.success(result, metadata);
    }
    
    /**
     * 格式化日期
     */
    private ToolResult executeFormat(Map<String, Object> parameters) {
        String dateStr = getParameter(parameters, "date", "");
        String format = getParameter(parameters, "format", "yyyy年MM月dd日 HH:mm:ss");
        
        if (dateStr.isEmpty()) {
            return ToolResult.failure("日期参数不能为空");
        }
        
        LocalDateTime dateTime = parseDateTime(dateStr);
        String formatted = dateTime.format(DateTimeFormatter.ofPattern(format));
        
        Map<String, Object> result = new HashMap<>();
        result.put("original", dateStr);
        result.put("formatted", formatted);
        result.put("format", format);
        
        return ToolResult.success(result);
    }
    
    /**
     * 计算日期差
     */
    private ToolResult executeDiff(Map<String, Object> parameters) {
        String date1Str = getParameter(parameters, "date", "");
        String date2Str = getParameter(parameters, "date2", "");
        
        if (date1Str.isEmpty() || date2Str.isEmpty()) {
            return ToolResult.failure("需要提供两个日期");
        }
        
        LocalDateTime date1 = parseDateTime(date1Str);
        LocalDateTime date2 = parseDateTime(date2Str);
        
        long days = ChronoUnit.DAYS.between(date1, date2);
        long hours = ChronoUnit.HOURS.between(date1, date2);
        long minutes = ChronoUnit.MINUTES.between(date1, date2);
        long seconds = ChronoUnit.SECONDS.between(date1, date2);
        
        Map<String, Object> result = new HashMap<>();
        result.put("date1", date1.toString());
        result.put("date2", date2.toString());
        result.put("diff_days", days);
        result.put("diff_hours", hours);
        result.put("diff_minutes", minutes);
        result.put("diff_seconds", seconds);
        
        // 友好描述
        result.put("description", buildDiffDescription(days, hours % 24, minutes % 60));
        
        return ToolResult.success(result);
    }
    
    /**
     * 日期加减
     */
    private ToolResult executeAdd(Map<String, Object> parameters) {
        String dateStr = getParameter(parameters, "date", "");
        Integer amount = getParameter(parameters, "amount", 0);
        String unit = getParameter(parameters, "unit", "days");
        
        LocalDateTime dateTime;
        if (dateStr.isEmpty()) {
            dateTime = LocalDateTime.now();
        } else {
            dateTime = parseDateTime(dateStr);
        }
        
        LocalDateTime result = switch (unit.toLowerCase()) {
            case "days" -> dateTime.plusDays(amount);
            case "weeks" -> dateTime.plusWeeks(amount);
            case "months" -> dateTime.plusMonths(amount);
            case "years" -> dateTime.plusYears(amount);
            case "hours" -> dateTime.plusHours(amount);
            case "minutes" -> dateTime.plusMinutes(amount);
            case "seconds" -> dateTime.plusSeconds(amount);
            default -> throw new IllegalArgumentException("不支持的时间单位: " + unit);
        };
        
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("original", dateTime.toString());
        resultMap.put("result", result.toString());
        resultMap.put("result_formatted", result.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        resultMap.put("amount", amount);
        resultMap.put("unit", unit);
        
        return ToolResult.success(resultMap);
    }
    
    /**
     * 时区转换
     */
    private ToolResult executeConvert(Map<String, Object> parameters) {
        String dateStr = getParameter(parameters, "date", "");
        String fromTimezone = getParameter(parameters, "timezone", "Asia/Shanghai");
        String toTimezone = getParameter(parameters, "date2", "America/New_York"); // 复用date2作为目标时区
        
        if (dateStr.isEmpty()) {
            dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        ZoneId fromZone = resolveZone(fromTimezone);
        ZoneId toZone = resolveZone(toTimezone);
        
        LocalDateTime localDateTime = parseDateTime(dateStr);
        ZonedDateTime fromDateTime = localDateTime.atZone(fromZone);
        ZonedDateTime toDateTime = fromDateTime.withZoneSameInstant(toZone);
        
        Map<String, Object> result = new HashMap<>();
        result.put("from_timezone", fromZone.getId());
        result.put("to_timezone", toZone.getId());
        result.put("from_datetime", fromDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("to_datetime", toDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return ToolResult.success(result);
    }
    
    /**
     * 解析日期
     */
    private ToolResult executeParse(Map<String, Object> parameters) {
        String dateStr = getParameter(parameters, "date", "");
        
        if (dateStr.isEmpty()) {
            return ToolResult.failure("日期字符串不能为空");
        }
        
        try {
            LocalDateTime dateTime = parseDateTime(dateStr);
            
            Map<String, Object> result = new HashMap<>();
            result.put("original", dateStr);
            result.put("parsed", dateTime.toString());
            result.put("year", dateTime.getYear());
            result.put("month", dateTime.getMonthValue());
            result.put("day", dateTime.getDayOfMonth());
            result.put("hour", dateTime.getHour());
            result.put("minute", dateTime.getMinute());
            result.put("second", dateTime.getSecond());
            result.put("day_of_week", dateTime.getDayOfWeek().toString());
            result.put("day_of_year", dateTime.getDayOfYear());
            result.put("is_leap_year", Year.isLeap(dateTime.getYear()));
            
            return ToolResult.success(result);
            
        } catch (Exception e) {
            return ToolResult.failure("无法解析日期: " + dateStr);
        }
    }
    
    /**
     * 解析时区
     */
    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            return ZoneId.of("Asia/Shanghai");
        }
        
        // 检查别名
        if (TIMEZONE_ALIASES.containsKey(timezone)) {
            return TIMEZONE_ALIASES.get(timezone);
        }
        
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("无法解析时区: {}，使用默认时区", timezone);
            return ZoneId.of("Asia/Shanghai");
        }
    }
    
    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateStr) {
        // 尝试多种格式
        List<DateTimeFormatter> formatters = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE
        );
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        
        // 尝试只有日期的情况
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return date.atStartOfDay();
        } catch (DateTimeParseException ignored) {}
        
        throw new IllegalArgumentException("无法解析日期: " + dateStr);
    }
    
    /**
     * 获取中文星期
     */
    private String getDayOfWeekChinese(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }
    
    /**
     * 构建日期差的友好描述
     */
    private String buildDiffDescription(long days, long hours, long minutes) {
        StringBuilder sb = new StringBuilder();
        
        if (days != 0) {
            sb.append(Math.abs(days)).append("天");
        }
        if (hours != 0) {
            sb.append(Math.abs(hours)).append("小时");
        }
        if (minutes != 0) {
            sb.append(Math.abs(minutes)).append("分钟");
        }
        
        if (sb.length() == 0) {
            return "0分钟";
        }
        
        return sb.toString();
    }
}
