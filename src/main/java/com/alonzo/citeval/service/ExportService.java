package com.alonzo.citeval.service;

import com.alonzo.citeval.model.entity.Evaluation;
import com.alonzo.citeval.model.entity.EvaluationScore;
import com.alonzo.citeval.model.entity.Professor;
import com.alonzo.citeval.repository.EvaluationRepository;
import com.alonzo.citeval.repository.ProfessorRepository;
import com.opencsv.CSVWriter;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private static final String DECRYPTION_FAILED = "[Decryption Failed]";

    private final EvaluationRepository evaluationRepository;
    private final ProfessorRepository professorRepository;
    private final EvaluationService evaluationService;

    public ExportService(EvaluationRepository evaluationRepository,
                         ProfessorRepository professorRepository,
                         EvaluationService evaluationService) {
        this.evaluationRepository = evaluationRepository;
        this.professorRepository = professorRepository;
        this.evaluationService = evaluationService;
    }

    public byte[] exportAllToCsv() {
        List<Evaluation> evaluations = evaluationRepository.findAllWithScores();
        return generateCsvBytes(evaluations);
    }

    public byte[] exportAllToPdf() {
        List<Evaluation> evaluations = evaluationRepository.findAllWithScores();
        return generatePdfBytes("All Evaluations Report", evaluations);
    }

    public byte[] exportFacultyToCsv(String facultyEmail) {
        List<Evaluation> evaluations = loadFacultyEvaluations(facultyEmail);
        return generateCsvBytes(evaluations);
    }

    public byte[] exportFacultyToPdf(String facultyEmail) {
        String normalized = normalizeRequiredEmail(facultyEmail);
        List<Evaluation> evaluations = evaluationRepository.findByFacultyEmailWithScores(normalized);
        return generatePdfBytes("Faculty Evaluation Report: " + normalized, evaluations);
    }

    public byte[] exportEvaluationToCsv(Long evaluationId) {
        Evaluation evaluation = findEvaluationWithScores(evaluationId);
        return generateCsvBytes(List.of(evaluation));
    }

    public byte[] exportEvaluationToPdf(Long evaluationId) {
        Evaluation evaluation = findEvaluationWithScores(evaluationId);
        return generatePdfBytes("Evaluation Detail Report: #" + evaluationId, List.of(evaluation));
    }

    public byte[] exportProfessorToCsv(Long professorId) {
        Professor professor = findProfessor(professorId);
        List<Evaluation> evaluations = evaluationRepository.findByFacultyEmailWithScores(normalizeRequiredEmail(professor.getEmail()));
        return generateCsvBytes(evaluations);
    }

    public byte[] exportProfessorToPdf(Long professorId) {
        Professor professor = findProfessor(professorId);
        List<Evaluation> evaluations = evaluationRepository.findByFacultyEmailWithScores(normalizeRequiredEmail(professor.getEmail()));
        return generatePdfBytes("Professor Evaluation Report: " + safe(professor.getName()), evaluations);
    }

    private List<Evaluation> loadFacultyEvaluations(String facultyEmail) {
        String normalized = normalizeRequiredEmail(facultyEmail);
        return evaluationRepository.findByFacultyEmailWithScores(normalized);
    }

    private Evaluation findEvaluationWithScores(Long evaluationId) {
        return evaluationRepository.findWithScoresById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluation not found"));
    }

    private Professor findProfessor(Long professorId) {
        return professorRepository.findById(professorId)
                .orElseThrow(() -> new RuntimeException("Professor not found"));
    }

    private byte[] generateCsvBytes(List<Evaluation> evaluations) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            writer.writeNext(new String[]{
                    "ID",
                    "Student Number",
                    "Student Email",
                    "Faculty Email",
                    "Section",
                    "Feedback",
                    "Scores"
            });

            for (Evaluation evaluation : evaluations) {
                writer.writeNext(new String[]{
                        String.valueOf(evaluation.getId()),
                        safe(evaluation.getStudentNumber()),
                        safe(evaluation.getStudentEmail()),
                        safe(evaluation.getFacultyEmail()),
                        safe(evaluation.getSection()),
                        decryptForExport(evaluation),
                        buildScoresSummary(evaluation)
                });
            }

            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export CSV", ex);
        }
    }

    private byte[] generatePdfBytes(String title, List<Evaluation> evaluations) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Generated on: " + OffsetDateTime.now()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 1.8f, 2.8f, 2.8f, 1.3f, 5.0f, 4.2f});

            table.addCell("ID");
            table.addCell("Student Number");
            table.addCell("Student Email");
            table.addCell("Faculty Email");
            table.addCell("Section");
            table.addCell("Feedback");
            table.addCell("Scores");

            for (Evaluation evaluation : evaluations) {
                table.addCell(String.valueOf(evaluation.getId()));
                table.addCell(safe(evaluation.getStudentNumber()));
                table.addCell(safe(evaluation.getStudentEmail()));
                table.addCell(safe(evaluation.getFacultyEmail()));
                table.addCell(safe(evaluation.getSection()));
                table.addCell(decryptForExport(evaluation));
                table.addCell(buildScoresSummary(evaluation));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export PDF", ex);
        }
    }

    private String decryptForExport(Evaluation evaluation) {
        try {
            return evaluationService.decryptFeedback(
                    evaluation.getCiphertext(),
                    evaluation.getStudentPublicKey(),
                    evaluation.getIv()
            );
        } catch (Exception ignored) {
            return DECRYPTION_FAILED;
        }
    }

    private String buildScoresSummary(Evaluation evaluation) {
        if (evaluation.getScores() == null || evaluation.getScores().isEmpty()) {
            return "";
        }

        return evaluation.getScores().stream()
                .sorted(Comparator.comparing(score -> criterionTitle(score).toLowerCase(Locale.ROOT)))
                .map(score -> criterionTitle(score) + ": " + score.getScore())
                .collect(Collectors.joining(" | "));
    }

    private String criterionTitle(EvaluationScore score) {
        if (score == null || score.getCriterion() == null || score.getCriterion().getTitle() == null) {
            return "Unknown Criterion";
        }
        return score.getCriterion().getTitle();
    }

    private String normalizeRequiredEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Faculty email is required");
        }
        return email.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
