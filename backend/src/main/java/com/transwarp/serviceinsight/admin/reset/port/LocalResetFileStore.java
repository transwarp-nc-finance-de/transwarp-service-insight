package com.transwarp.serviceinsight.admin.reset.port;

import java.io.IOException;

public interface LocalResetFileStore {
  void clearOriginalFiles() throws IOException;
}
