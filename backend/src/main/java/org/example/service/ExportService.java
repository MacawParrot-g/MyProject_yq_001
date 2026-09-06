package org.example.service;

import org.example.common.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ExportService {

    Result getUnexportedByUser(String recorder);

//    Result executeExport(String recorder);

    Result getExportStatus(String recorder);

    void downloadAndDelete(String recorder, HttpServletResponse response);

    Result executeExportByDate(String recorder, String date);

    Result executeExportByHashes(String recorder, List<String> hashes);

    Result executeExportAll(String recorder);

    Result countUnexportedToday(String recorder, String date);
}
