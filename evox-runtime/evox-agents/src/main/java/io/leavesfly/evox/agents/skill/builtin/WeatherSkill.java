package io.leavesfly.evox.agents.skill.builtin;

import io.leavesfly.evox.agents.skill.BaseSkill;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class WeatherSkill extends BaseSkill {

    public WeatherSkill() {
        setName("weather");
        setDescription("Query weather information for a specified location. "
                + "Provides current conditions, temperature, humidity, wind, and multi-day forecasts.");

        setSystemPrompt(buildWeatherSystemPrompt());

        setRequiredTools(List.of("http"));

        Map<String, Map<String, String>> inputParams = new LinkedHashMap<>();

        Map<String, String> locationParam = new HashMap<>();
        locationParam.put("type", "string");
        locationParam.put("description", "Location to query weather for (city name, e.g. 'Beijing', 'San Francisco')");
        inputParams.put("location", locationParam);

        Map<String, String> daysParam = new HashMap<>();
        daysParam.put("type", "integer");
        daysParam.put("description", "Number of forecast days (1-7, default: 3)");
        inputParams.put("days", daysParam);

        Map<String, String> unitsParam = new HashMap<>();
        unitsParam.put("type", "string");
        unitsParam.put("description", "Temperature units: 'celsius' or 'fahrenheit' (default: 'celsius')");
        inputParams.put("units", unitsParam);

        setInputParameters(inputParams);
        setRequiredInputs(List.of("location"));
    }

    @Override
    public SkillResult execute(SkillContext context) {
        validateInputs(context.getParameters());

        String location = context.getInput();
        String days = context.getParameters().getOrDefault("days", "3").toString();
        String units = context.getParameters().getOrDefault("units", "celsius").toString();

        String prompt = buildPrompt(location, context.getAdditionalContext());

        String weatherPrompt = prompt
                + "\n\nPlease query the weather for: " + location
                + "\nForecast days: " + days
                + "\nTemperature units: " + units
                + "\n\nProvide the following information:"
                + "\n1. **Current Conditions**: Temperature, humidity, wind speed/direction, weather description"
                + "\n2. **Today's Forecast**: High/low temperatures, precipitation probability, UV index"
                + "\n3. **Multi-day Forecast**: Daily summary for the requested number of days"
                + "\n4. **Recommendations**: Clothing suggestions, umbrella needed, outdoor activity advice"
                + "\n\nUse the http tool to call a weather API (e.g., wttr.in or OpenWeatherMap) to get real data.";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillName", "weather");
        metadata.put("location", location);
        metadata.put("days", days);
        metadata.put("units", units);

        return SkillResult.success(weatherPrompt, metadata);
    }

    private String buildWeatherSystemPrompt() {
        return """
                You are a weather information assistant with expertise in meteorology.
                
                When querying weather:
                1. Use available HTTP tools to fetch real-time weather data
                2. You can use wttr.in API (e.g., curl wttr.in/CityName?format=j1) for quick weather data
                3. Present weather information in a clear, organized format
                4. Include practical recommendations based on weather conditions
                5. Convert temperature units as requested by the user
                6. Mention any severe weather alerts if applicable
                
                Format your response with clear sections and use weather emojis for readability:
                - ☀️ Sunny/Clear
                - ⛅ Partly Cloudy
                - ☁️ Cloudy
                - 🌧️ Rain
                - ❄️ Snow
                - 🌡️ Temperature""";
    }
}
