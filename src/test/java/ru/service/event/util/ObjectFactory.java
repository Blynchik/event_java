package ru.service.event.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.service.event.dto.event.request.DecisionRequest;
import ru.service.event.dto.event.request.DecisionResultRequest;
import ru.service.event.dto.event.request.EventRequest;
import ru.service.event.model.DecisionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

;

public class ObjectFactory {

    public static String getRandomString(Random random, int length) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < length; i++) {
            string.append(Character.toString('A' + random.nextInt(26)));
        }
        return string.toString();
    }

    public static <T> T convertJsonToObject(ObjectMapper objectMapper, String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Convert error " + clazz.getSimpleName(), e);
        }
    }

    public static <T> List<T> convertJsonToList(ObjectMapper objectMapper, String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException("Convert error " + clazz.getSimpleName() + ">", e);
        }
    }

    public static String getChangedSignToken() {
        return getEternalToken().subSequence(0, getEternalToken().length() - 2) + "A";
    }

    public static String getExpiredToken() {
        return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiaWJhQHlhbmRleC5ydSIsInVzZXJJZCI6MSwiaWF0IjoxNzMyMTk3NDAyLCJleHAiOjE3MzIxOTc0MTIsImF1dGhvcml0aWVzIjpbIlJPTEVfVVNFUiJdfQ.Uw76kNVdU-mK_YB59xLMCtKZjScKEzyeolxR610jlyA";
    }

    public static String getEternalToken() {
        return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiaWJhQHlhbmRleC5ydSIsInVzZXJJZCI6MSwiaWF0IjoxNzMyMTk3ODgzLCJleHAiOjIwMzk3ODE4ODMsImF1dGhvcml0aWVzIjpbIlJPTEVfVVNFUiJdfQ.g9KlcaBmOD1qMH4P0Gq_-Dn_GVcsX14EFzYvtfYXAZI";
    }

    public static String getEternalAdminToken() {
        return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiaWJhQHlhbmRleC5ydSIsInVzZXJJZCI6MSwiaWF0IjoxNzM0NDQwNzAyLCJleHAiOjIwNDk4MDA3MDIsImF1dGhvcml0aWVzIjpbIlJPTEVfVVNFUiIsIlJPTEVfQURNSU4iXX0.l_RFfhdX5boOLI5AkShItkzrGlYCSRmguUyND0L3zfA";
    }

    public static EventRequest getEventRequest1() {
        return getEventRequestCustom("Ужасный дракон!",
                "Вам встретился ужасный дракон! Как вы поступите?",
                new ArrayList<>() {{
                    add(getDecisionRequestCustom(DecisionType.STR,
                            "Поборю дракона своей силой",
                            10,
                            new ArrayList<>() {{
                                add("Бью дракона");
                                add("Кусаю дракона");
                            }},
                            Map.of(true, new DecisionResultRequest("Вы победили дракона"),
                                    false, new DecisionResultRequest("Дракон вас слопал"))));
                    add(getDecisionRequestCustom(DecisionType.CHA,
                            "Заболтаю дракона",
                            8,
                            new ArrayList<>() {{
                                add("Льщу дракону");
                                add("Заговариваю зубы дракону");
                            }},
                            Map.of(true, new DecisionResultRequest("Дракон наградил вас за сладкие речи"),
                                    false, new DecisionResultRequest("Дракон вас слопал"))));
                    add(getDecisionRequestCustom(DecisionType.TEXT,
                            "Не привлекая внимания, быстро скроюсь, пока дракон меня не заметил",
                            0,
                            new ArrayList<>() {{
                                add("На цыпочках отхожу от дракона");
                                add("Тихо-тихо скрываюсь в тенях");
                            }},
                            Map.of(true, new DecisionResultRequest("Вы ушли от дракона"),
                                    false, new DecisionResultRequest("Вы ушли от дракона"))));
                }}
        );
    }

    public static EventRequest getEventRequestCustom(String title, String description,
                                                     List<DecisionRequest> decisions) {
        return new EventRequest(title, description, decisions);
    }

    public static DecisionRequest getDecisionRequestCustom(DecisionType decisionType, String description, int difficulty,
                                                           List<String> decisionLog,
                                                           Map<Boolean, DecisionResultRequest> results) {
        return new DecisionRequest(decisionType.name(), description, decisionLog, difficulty, results);
    }

    public static DecisionResultRequest getDecisionResultRequestCustom(String resultDescr) {
        return new DecisionResultRequest(resultDescr);
    }
}
