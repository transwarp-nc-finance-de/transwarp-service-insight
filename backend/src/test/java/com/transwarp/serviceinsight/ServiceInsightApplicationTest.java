package com.transwarp.serviceinsight;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrecheckController.class) @Import({MockPrecheckService.class,GlobalExceptionHandler.class})
class ServiceInsightApplicationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 @Test void deterministicMockMaintainsHumanBoundary(){var s=new MockPrecheckService();var r=new PrecheckRequest("查询变慢","模拟环境响应增加","Inceptor","SQL","6.0","P2","单任务","脱敏摘要");var a=s.precheck(r);var b=s.precheck(r);assertThat(a.precheckId()).isEqualTo(b.precheckId());assertThat(a.humanReviewRequired()).isTrue();assertThat(a.summary()).contains("不是最终根因");assertThat(a.references()).allMatch(x->x.mockData()&&x.url().startsWith("https://example.com/"));}
 @Test void detectsMissingInformation(){var r=new PrecheckRequest("告警","模拟告警",null,null,null,null,null,null);assertThat(new MockPrecheckService().precheck(r).confidence()).isEqualTo(Confidence.LOW);assertThat(new MockPrecheckService().missingInformation(r)).contains("产品","影响范围");}
 @Test void successContract() throws Exception {var r=new PrecheckRequest("告警","模拟告警",null,null,null,null,null,null);mvc.perform(post("/api/v1/precheck").contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isOk()).andExpect(jsonPath("$.precheckId").exists()).andExpect(jsonPath("$.humanReviewRequired").value(true)).andExpect(jsonPath("$.confidence").value("LOW"));}
 @Test void structured400() throws Exception {mvc.perform(post("/api/v1/precheck").contentType("application/json").content("{\"title\":\"\",\"description\":\"\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR")).andExpect(jsonPath("$.fieldErrors.title").exists()).andExpect(jsonPath("$.fieldErrors.description").exists());}
}
