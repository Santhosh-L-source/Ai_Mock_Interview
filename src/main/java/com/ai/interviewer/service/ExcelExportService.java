package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.AttendanceMarksRecord;
import com.ai.interviewer.domain.entity.ScheduledInterview;
import com.ai.interviewer.repository.ScheduledInterviewRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Asia/Kolkata"));

    private static final String[] HEADERS = {
            "S.No", "Roll No", "Candidate Name", "Email", "Department",
            "Status", "Technical Score", "Communication Score",
            "Overall Score", "Verdict", "Malpractice", "Warnings",
            "Started At", "Ended At"
    };

    private final AdminSchedulingService adminSchedulingService;
    private final ScheduledInterviewRepository scheduledInterviewRepository;

    public ExcelExportService(AdminSchedulingService adminSchedulingService,
                              ScheduledInterviewRepository scheduledInterviewRepository) {
        this.adminSchedulingService = adminSchedulingService;
        this.scheduledInterviewRepository = scheduledInterviewRepository;
    }

    public byte[] generateInterviewReport(UUID interviewId) throws IOException {
        ScheduledInterview interview = scheduledInterviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));

        List<AttendanceMarksRecord> records = adminSchedulingService.getInterviewAttendance(interviewId);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String sheetName = interview.getTitle();
            if (sheetName.length() > 31) {
                sheetName = sheetName.substring(0, 31);
            }
            sheetName = sheetName.replaceAll("[\\\\/:*?\\[\\]]", "_");
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle scoreStyle = createScoreStyle(workbook);
            CellStyle malpracticeStyle = createMalpracticeStyle(workbook);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(interview.getTitle() + " — Results Report");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            // Subtitle row
            Row subtitleRow = sheet.createRow(1);
            Cell subtitleCell = subtitleRow.createCell(0);
            subtitleCell.setCellValue("Role: " + interview.getTargetRole()
                    + "  |  Total Questions: " + interview.getTotalQuestions()
                    + "  |  Duration: " + interview.getDurationMinutes() + " min"
                    + "  |  Target Year: " + (interview.getTargetYear() != null ? interview.getTargetYear() : "ALL"));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADERS.length - 1));

            // Header row
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 4;
            for (int i = 0; i < records.size(); i++) {
                AttendanceMarksRecord r = records.get(i);
                Row row = sheet.createRow(rowIdx++);
                boolean isMalpractice = Boolean.TRUE.equals(r.isMalpractice());
                CellStyle style = isMalpractice ? malpracticeStyle : dataStyle;
                CellStyle numStyle = isMalpractice ? malpracticeStyle : scoreStyle;

                createCell(row, 0, i + 1, style);
                createCell(row, 1, r.candidateRollNo() != null ? r.candidateRollNo() : "-", style);
                createCell(row, 2, r.candidateName(), style);
                createCell(row, 3, r.candidateEmail(), style);
                createCell(row, 4, "-", style);
                createCell(row, 5, r.attemptStatus(), style);
                createNumericCell(row, 6, r.technicalScore(), numStyle);
                createNumericCell(row, 7, r.communicationScore(), numStyle);
                createNumericCell(row, 8, r.overallScore(), numStyle);
                createCell(row, 9, r.verdict() != null ? r.verdict() : "-", style);
                createCell(row, 10, isMalpractice ? "YES" : "No", isMalpractice ? malpracticeStyle : style);
                createCell(row, 11, r.proctorWarningsCount() != null ? r.proctorWarningsCount() : 0, style);
                createCell(row, 12, r.startedAt() != null ? DATE_FMT.format(r.startedAt()) : "-", style);
                createCell(row, 13, r.endedAt() != null ? DATE_FMT.format(r.endedAt()) : "-", style);
            }

            // Summary row
            rowIdx++;
            Row summaryRow = sheet.createRow(rowIdx);
            Cell summaryLabel = summaryRow.createCell(0);
            summaryLabel.setCellValue("Total Candidates: " + records.size()
                    + "  |  Completed: " + records.stream().filter(r -> "COMPLETED".equals(r.attemptStatus())).count()
                    + "  |  Malpractice: " + records.stream().filter(r -> Boolean.TRUE.equals(r.isMalpractice())).count());
            summaryLabel.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, HEADERS.length - 1));

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createScoreStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("0.0"));
        return style;
    }

    private CellStyle createMalpracticeStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(value != null ? value.toString() : "-");
        }
        cell.setCellStyle(style);
    }

    private void createNumericCell(Row row, int col, Double value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("-");
        }
        cell.setCellStyle(style);
    }
}
