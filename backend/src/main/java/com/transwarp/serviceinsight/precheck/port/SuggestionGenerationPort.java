package com.transwarp.serviceinsight.precheck.port;

import com.transwarp.serviceinsight.precheck.application.CreatePrecheckCommand;
import java.util.List;

public interface SuggestionGenerationPort {
  List<String> generate(CreatePrecheckCommand command, List<String> missingInformation);

  FollowUpSuggestion followUp(String message);

  record FollowUpSuggestion(
      String reply,
      List<String> recommendations,
      List<String> missingInformation,
      boolean matched,
      String evidenceTitle) {}
}
