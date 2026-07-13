package com.transwarp.serviceinsight.precheck.port;

import com.transwarp.serviceinsight.precheck.application.CreatePrecheckCommand;
import java.util.List;

public interface CompletenessAssessmentPort {
  List<String> missingInformation(CreatePrecheckCommand command);
}
