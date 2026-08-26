package org.example.service;

import org.example.common.Result;
import jakarta.servlet.http.HttpServletResponse;

public interface ExportService {

    Result getUnexportedByUser(String recorder);

    Result executeExport(String recorder);

    Result getExportStatus(String recorder);

    void downloadAndDelete(String recorder, HttpServletResponse response);
}
