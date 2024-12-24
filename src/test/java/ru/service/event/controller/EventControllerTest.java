package ru.service.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import ru.service.event.dto.event.request.DecisionRequest;
import ru.service.event.dto.event.request.DecisionResultRequest;
import ru.service.event.dto.event.request.EventRequest;
import ru.service.event.dto.event.response.common.EventResponse;
import ru.service.event.dto.event.response.create.DecisionResponseWithResultForCreate;
import ru.service.event.dto.event.response.create.EventResponseForCreate;
import ru.service.event.model.DecisionType;

import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.service.event.util.ApiRequest.createCustomEvent;
import static ru.service.event.util.ApiRequest.getRandomEvent;
import static ru.service.event.util.ObjectFactory.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc(printOnlyOnFailure = false)
public class EventControllerTest {

    private final MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Random random;

    @Autowired
    private EventControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
    }

    @Nested
    @DisplayName(value = "Тесты на аутентификацию")
    class AuthTest {

        private EventRequest eventRequest;
        private String eventAsString;

        @BeforeEach
        void setUp() throws Exception {
            this.eventRequest = getEventRequest1();
            this.eventAsString = objectMapper.writeValueAsString(eventRequest);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/admin/event"})
        @Description("Тесты на авторизацию")
        void authorizationTest(String url) throws Exception {
            //given
            // нужно, что бы понимать, что вернувшиеся ошибки связаны только с токенами
            String token = getEternalAdminToken();
            createCustomEvent(mockMvc, eventAsString, token);
            //when
            List<String> tokens = List.of(getEternalToken());
            tokens.forEach(t -> {
                MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders
                        .post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventAsString)
                        .header("Authorization", "Bearer " + t);
                try {
                    //then
                    mockMvc.perform(createRequest)
                            .andExpect(
                                    status().isForbidden());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/event", "/api/admin/event"})
        @Description("Тесты на аутентификацию")
        void authTest(String url) throws Exception {
            //given
            // нужно, что бы понимать, что вернувшиеся ошибки связаны только с токенами
            String token = getEternalAdminToken();
            createCustomEvent(mockMvc, eventAsString, token);
            //when
            List<String> tokens = List.of(getExpiredToken(), getChangedSignToken());
            tokens.forEach(t -> {
                MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders
                        .get(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + t);
                try {
                    //then
                    mockMvc.perform(createRequest)
                            .andExpect(
                                    status().is4xxClientError())
                            .andExpect(
                                    jsonPath("$.exceptions[0].exception").value("JwtException"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Nested
    @DisplayName(value = "Тесты на создание события")
    class CreateTest {

        private EventRequest eventRequest;
        private String eventAsString;

        @BeforeEach
        void setUp() throws Exception {
            this.eventRequest = getEventRequest1();
            this.eventAsString = objectMapper.writeValueAsString(eventRequest);
        }

        @Test
        @Description(value = "Тест на успешность создания события")
        void success() throws Exception {
            //given
            String token = getEternalAdminToken();
            //when
            String responseAsString = createCustomEvent(mockMvc, eventAsString, token)
                    //then
                    .andExpect(
                            status().isCreated())
                    .andExpect(
                            jsonPath("$.title").value(eventRequest.getTitle()))
                    .andExpect(
                            jsonPath("$.description").value(eventRequest.getDescription()))
                    .andReturn().getResponse().getContentAsString();

            EventResponseForCreate response = convertJsonToObject(objectMapper, responseAsString, EventResponseForCreate.class);

            boolean hasFirst = false;
            boolean hasText = false;
            for (DecisionResponseWithResultForCreate decision : response.getDecisions()) {
                if (decision.getDecisionType().equals(DecisionType.valueOf(eventRequest.getDecisions().get(0).getDecisionType())) &&
                        decision.getDecisionDescr().equals(eventRequest.getDecisions().get(0).getDescription()) &&
                        decision.getDifficulty().equals(eventRequest.getDecisions().get(0).getDifficulty()) &&
                        decision.getEventTitle().equals(eventRequest.getTitle())) {
                    hasFirst = true;
                    break;
                }
                hasText = decision.getDecisionType().equals(DecisionType.valueOf(eventRequest.getDecisions().get(2).getDecisionType())) &&
                        decision.getDecisionDescr().equals(eventRequest.getDecisions().get(2).getDescription()) &&
                        decision.getDifficulty().equals(eventRequest.getDecisions().get(2).getDifficulty()) &&
                        decision.getEventTitle().equals(eventRequest.getTitle()) &&
                        decision.getResults().get(true).getResultDescr().equals(eventRequest.getDecisions().get(2).getResults().get(true).getResultDescr()) &&
                        decision.getResults().get(false).getResultDescr().equals(eventRequest.getDecisions().get(2).getResults().get(false).getResultDescr());
            }
            assertTrue(hasFirst);

            for (DecisionResponseWithResultForCreate decision : response.getDecisions()) {
                if (decision.getDecisionType().equals(DecisionType.valueOf(eventRequest.getDecisions().get(2).getDecisionType())) &&
                        decision.getDecisionDescr().equals(eventRequest.getDecisions().get(2).getDescription()) &&
                        decision.getDifficulty().equals(eventRequest.getDecisions().get(2).getDifficulty()) &&
                        decision.getEventTitle().equals(eventRequest.getTitle()) &&
                        decision.getResults().get(true).getResultDescr().equals(eventRequest.getDecisions().get(2).getResults().get(true).getResultDescr()) &&
                        decision.getResults().get(false).getResultDescr().equals(eventRequest.getDecisions().get(2).getResults().get(false).getResultDescr())) {
                    hasText = true;
                    break;
                }
            }
            assertTrue(hasText);
        }

        @Test
        @Description(value = "Тест на успешность создания события")
        void noAdminRights() throws Exception {
            //given
            String token = getEternalToken();
            //when
            createCustomEvent(mockMvc, eventAsString, token)
                    //then
                    .andExpect(
                            status().isForbidden());
        }

        @Test
        @Description(value = "Тест на создание события с длинным названием")
        void longTitle() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.setTitle(getRandomString(random, 101));
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 100 characters long")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "       "})
        @Description(value = "Тест на создание события с empty и blank названием")
        void emptyTitle(String title) throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.setTitle(title);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[?(@.exception == 'BindingValidationException' " +
                                    "&& @.field == 'title' " +
                                    "&& @.descr == 'Should be no more than 100 characters long')]")
                                    .exists());
        }

        @Test
        @Description(value = "Тест на создание события с null названием")
        void nullTitle() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.setTitle(null);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 100 characters long")));
        }

        @Test
        @Description(value = "Тест на создание события с длинным описанием")
        void longDescription() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.setDescription(getRandomString(random, 1001));
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 1000 characters long")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "       "})
        @Description(value = "Тест на создание события с empty и blank описанием")
        void emptyDescription(String description) throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.setDescription(description);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[?(@.exception == 'BindingValidationException' " +
                                    "&& @.field == 'description' " +
                                    "&& @.descr == 'Should be no more than 1000 characters long')]")
                                    .exists());
        }

        @Test
        @Description(value = "Тест на создание события с null описанием")
        void nullDescription() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.setDescription(null);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 1000 characters long")));
        }

        @Test
        @Description(value = "Тест на создание события с null решениями")
        void nullDecisions() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.setDecisions(null);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The decisions list must contain at least one element")));
        }

        @Test
        @Description(value = "Тест на создание события с пустыми решениями")
        void emptyDecisions() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.setDecisions(Collections.emptyList());
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The decisions list must contain at least one element")));
        }

        @Test
        @Description(value = "Тест на создание события с количеством решений больше 20")
        void tooMuchDecisions() throws Exception {
            //given
            String token = getEternalAdminToken();
            List<DecisionRequest> decisionRequests = new ArrayList<>();
            for (int i = 0; i < 21; i++) {
                decisionRequests.add(
                        getDecisionRequestCustom(DecisionType.TEXT,
                                "Не привлекая внимания, быстро скроюсь, пока дракон меня не заметил",
                                0,
                                List.of("На цыпочках отхожу от дракона", "Тихо-тихо скрываюсь в тенях"),
                                Map.of(true, new DecisionResultRequest("Вы ушли от дракона"),
                                        false, new DecisionResultRequest("Вы ушли от дракона"))));
            }
            eventRequest.setDecisions(decisionRequests);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The decisions list must contain at least one element")));
        }

        @Test
        @Description(value = "Тест на создание события с одинаковыми решениями")
        void sameDecisions() throws Exception {
            //given
            String token = getEternalAdminToken();
            List<DecisionRequest> decisionRequests = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                decisionRequests.add(
                        getDecisionRequestCustom(DecisionType.TEXT,
                                "Не привлекая внимания, быстро скроюсь, пока дракон меня не заметил",
                                0,
                                List.of("На цыпочках отхожу от дракона", "Тихо-тихо скрываюсь в тенях"),
                                Map.of(true, new DecisionResultRequest("Вы ушли от дракона"),
                                        false, new DecisionResultRequest("Вы ушли от дракона"))));
            }
            eventRequest.setDecisions(decisionRequests);
            //when
            assertEquals(1, convertJsonToObject(objectMapper,
                    createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                            //then
                            .andExpect(
                                    status().isCreated())
                            .andReturn().getResponse().getContentAsString(),
                    EventResponseForCreate.class).getDecisions().size());
        }

        @Test
        @Description(value = "Тест на создание события с неверным типом решения")
        void wrongDecisionType() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDecisionType("WRONG");
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].decisionType"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Invalid decision type")));
        }

        @Test
        @Description(value = "Тест на создание события с длинным описанием решения")
        void longDecisionDescription() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDescription(getRandomString(random, 101));
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].description"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 100 characters long")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "       "})
        @Description(value = "Тест на создание события с empty и blank описанием решения")
        void emptyDecisionDescription(String description) throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDescription(description);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].description"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 100 characters long")));
        }

        @Test
        @Description(value = "Тест на создание события с null описанием решения")
        void nullDecisionDescription() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDescription(null);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].description"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 100 characters long")));
        }

        @Test
        @Description(value = "Тест на создание события с null логами решений")
        void nullDecisionLog() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDecisionLog(null);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].decisionLog"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Decision log must contain at least one entry")));
        }

        @Test
        @Description(value = "Тест на создание события с пустыми логами решений")
        void emptyDecisionLog() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDecisionLog(Collections.emptyList());
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].decisionLog"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Decision log must contain at least one entry")));
        }

        @Test
        @Description(value = "Тест на создание события с количеством логов решений больше 5")
        void tooMuchDecisionsLog() throws Exception {
            //given
            String token = getEternalAdminToken();
            List<String> decisionLogs = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                decisionLogs.add("Лог решения");
            }
            eventRequest.getDecisions().get(0).setDecisionLog(decisionLogs);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].decisionLog"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Decision log must contain at least one entry")));
        }

        @Test
        @Description(value = "Тест на создание события с одинаковыми логами решений")
        void sameDecisionLogs() throws Exception {
            //given
            String token = getEternalAdminToken();
            List<String> decisionLogs = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                decisionLogs.add("Лог решения");
            }
            eventRequest.getDecisions().get(0).setDecisionLog(decisionLogs);
            eventRequest.getDecisions().remove(eventRequest.getDecisions().size() - 1);
            eventRequest.getDecisions().remove(eventRequest.getDecisions().size() - 1);
            //when
            assertEquals(List.of("Лог решения").toString(), convertJsonToObject(objectMapper,
                    createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                            //then
                            .andExpect(
                                    status().isCreated())
                            .andReturn().getResponse().getContentAsString(),
                    EventResponseForCreate.class).getDecisions().get(0).getDecisionLog());
        }

        @Test
        @Description(value = "Тест на создание события с длинными логами решения")
        void longDecisionLog() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).getDecisionLog().add(getRandomString(random, 101));
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].decisionLog[2]"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Decision log entry cannot exceed 100 characters")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "       "})
        @Description(value = "Тест на создание события с empty и blank логом решения")
        void emptyEachDecisionLog(String decisionLog) throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).getDecisionLog().add(decisionLog);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].decisionLog[2]"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Decision log entry cannot exceed 100 characters")));
        }

        @Test
        @Description(value = "Тест на создание события с null логом решения")
        void nullEachDecisionLog() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).getDecisionLog().add(null);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].decisionLog[2]"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Decision log entry cannot exceed 100 characters")));
        }

        @Test
        @Description(value = "Тест на создание события с отрицательной сложностью решения")
        void negativeDecisionDifficulty() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDifficulty(-1);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].difficulty"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Difficulty must be at least 0")));
        }

        @Test
        @Description(value = "Тест на создание события с null результатами решений")
        void nullDecisionResults() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setResults(null);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("It is necessary to indicate the positive and negative result")));
        }

        @Test
        @Description(value = "Тест на создание события с пустыми результатами решений")
        void emptyDecisionResults() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setResults(Collections.emptyMap());
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The successful or unsuccessful result is not specified")));
        }

        @Test
        @Description(value = "Тест на создание события с длинными описанием решения")
        void longResultDescr() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).getResults().get(true).setResultDescr(getRandomString(random, 1001));
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results[true].resultDescr"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 1000 characters long")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "       "})
        @Description(value = "Тест на создание события с empty и blank описанием решения")
        void emptyResultDescr(String resultDescr) throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).getResults().get(true).setResultDescr(resultDescr);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results[true].resultDescr"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 1000 characters long")));
        }

        @Test
        @Description(value = "Тест на создание события с null описанием решений")
        void nullResultDescr() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).getResults().get(true).setResultDescr(null);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results[true].resultDescr"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("Should be no more than 1000 characters long")));
        }

        @Test
        @Description(value = "Тест на создание события без true/false решений")
        void NoTrueFalseDecisionResults() throws Exception {
            //given
            String token = getEternalAdminToken();
            //показывает ошибку, но просто создание new Boolean через строку deprecated, тест запускается
            eventRequest.getDecisions().get(0).setResults(Map.of(new Boolean("yes"), new DecisionResultRequest("Ничего не происходит")));
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The successful or unsuccessful result is not specified")));
        }

        @Test
        @Description(value = "Тест на создание события без true решений")
        void NoTrueDecisionResults() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setResults(Map.of(false, new DecisionResultRequest("Ничего не происходит")));
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The successful or unsuccessful result is not specified")));
        }

        @Test
        @Description(value = "Тест на создание события без false решений")
        void NoFalseDecisionResults() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setResults(Map.of(true, new DecisionResultRequest("Ничего не происходит")));
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The successful or unsuccessful result is not specified")));
        }

        @Test
        @Description(value = "Тест на создание события с TEXT решением и сложностью > 0")
        void decisionTypeTextAndDifficultyMore0() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDecisionType(DecisionType.TEXT.name());
            eventRequest.getDecisions().get(0).setDifficulty(1);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The simple decisions should be with 0 difficulty")));
        }

        @Test
        @Description(value = "Тест на создание события с не TEXT решением и сложностью == 0")
        void decisionTypeNoTextAndDifficultyEquals0() throws Exception {
            //given
            String token = getEternalAdminToken();
            eventRequest.getDecisions().get(0).setDecisionType(DecisionType.DEX.name());
            eventRequest.getDecisions().get(0).setDifficulty(0);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("decisions[0].results"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The characteristic check decisions should be with difficulty more than 0")));
        }

        @Test
        @Description(value = "Тест на создание события с неуникальным названием")
        void noUniqueTitle() throws Exception {
            //given
            String token = getEternalAdminToken();
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token);
            //when
            createCustomEvent(mockMvc, objectMapper.writeValueAsString(eventRequest), token)
                    //then
                    .andExpect(
                            status().isBadRequest())
                    .andExpect(
                            jsonPath("$.exceptions[0].exception").value("BindingValidationException"))
                    .andExpect(
                            jsonPath("$.exceptions[0].field").value("title"))
                    .andExpect(
                            jsonPath("$.exceptions[0].descr").value(containsString("The title is not unique")));
        }
    }

    @Nested
    @DisplayName(value = "Тесты на получение случайного события")
    class GetRandomTest {

        private EventRequest eventRequest;
        private String eventAsString;

        @BeforeEach
        void setUp() throws Exception {
            this.eventRequest = getEventRequest1();
            this.eventAsString = objectMapper.writeValueAsString(eventRequest);
        }

        @Test
        @Description(value = "Тест на получение случайного события")
        void success() throws Exception {
            //given
            String token = getEternalAdminToken();
            createCustomEvent(mockMvc, eventAsString, token);
            //when
            String json = getRandomEvent(mockMvc, getEternalToken())
                    //then
                    .andExpect(
                            status().isOk())
                    .andReturn().getResponse().getContentAsString();
            EventResponse eventResponse = convertJsonToObject(objectMapper, json, EventResponse.class);
            assertEquals(eventRequest.getTitle(), eventResponse.getTitle());
            assertEquals(eventRequest.getDescription(), eventResponse.getDescription());
        }
    }
}