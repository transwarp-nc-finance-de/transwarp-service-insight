package com.transwarp.serviceinsight;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@SpringBootApplication
public class ServiceInsightApplication { public static void main(String[] args) { SpringApplication.run(ServiceInsightApplication.class, args); } }
record PrecheckRequest(@NotBlank(message="标题不能为空") String title, @NotBlank(message="问题描述不能为空") String description, String product, String module, String version, String severity, String impactScope, String attachmentsSummary) {}
record ReferenceItem(String sourceType, String title, String excerpt, String url, boolean mockData) {}
enum Confidence { HIGH, MEDIUM, LOW }
record PrecheckResponse(String precheckId, String summary, List<String> recommendations, List<ReferenceItem> references, Confidence confidence, boolean humanReviewRequired, List<String> missingInformation, String fallbackReason) {}
record ApiError(String code, String message, Map<String,String> fieldErrors, Instant timestamp) {}

@Service
class MockPrecheckService {
    PrecheckResponse precheck(PrecheckRequest r) {
        var missing = missingInformation(r);
        var id = UUID.nameUUIDFromBytes((r.title().trim()+"|"+r.description().trim()).getBytes(StandardCharsets.UTF_8)).toString();
        var module = text(r.module()) ? r.module().trim() : "未明确模块";
        var refs = List.of(new ReferenceItem("PRODUCT_MANUAL","产品手册排查章节（模拟数据）","建议核对版本、模块状态和近期变更。","https://example.com/mock/product-manual",true), new ReferenceItem("HISTORICAL_SLA","相似历史 SLA 摘要（模拟数据）","建议先补齐时间范围与影响对象。","https://example.com/mock/historical-sla",true));
        return new PrecheckResponse(id,"模拟预诊：已整理 "+module+" 的辅助排查方向；这不是最终根因或处理结论。",List.of("请人工核对发生时间、影响范围和复现步骤。","确认适用性后再执行只读检查或补充脱敏信息。","由提交人确认内容后决定是否继续提交 SLA。"),refs,missing.size()>=4?Confidence.LOW:Confidence.MEDIUM,true,missing,"M1 使用确定性 Mock 规则，未调用真实 RAG、大模型或外部服务。");
    }
    List<String> missingInformation(PrecheckRequest r) {
        var m=new ArrayList<String>(); add(m,r.product(),"产品"); add(m,r.module(),"模块"); add(m,r.version(),"版本"); add(m,r.severity(),"紧急程度"); add(m,r.impactScope(),"影响范围"); add(m,r.attachmentsSummary(),"附件摘要（仅限模拟或脱敏内容）"); return m;
    }
    private void add(List<String> m,String v,String label){if(!text(v))m.add(label);} private boolean text(String v){return v!=null&&!v.isBlank();}
}

@RestController @RequestMapping("/api/v1")
class PrecheckController { private final MockPrecheckService service; PrecheckController(MockPrecheckService service){this.service=service;} @PostMapping("/precheck") PrecheckResponse precheck(@Valid @RequestBody PrecheckRequest request){return service.precheck(request);} }

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex){var errors=new LinkedHashMap<String,String>();ex.getBindingResult().getFieldErrors().forEach(e->errors.put(e.getField(),e.getDefaultMessage()));return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR","请求参数校验失败",errors,Instant.now()));}
    @ExceptionHandler(Exception.class) ResponseEntity<ApiError> unexpected(Exception ex){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("INTERNAL_ERROR","预诊服务暂时不可用，请稍后重试或继续人工提交",Map.of(),Instant.now()));}
}
