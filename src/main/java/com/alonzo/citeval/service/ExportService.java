package com.alonzo.citeval.service;

import com.alonzo.citeval.model.entity.Evaluation;
import com.alonzo.citeval.model.entity.EvaluationScore;
import com.alonzo.citeval.model.entity.Professor;
import com.alonzo.citeval.repository.EvaluationRepository;
import com.alonzo.citeval.repository.ProfessorRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
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
        // Admin export: do not withhold here unless you explicitly want to.
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
        // Single evaluation exports are inherently identifying; keep as-is, or remove this endpoint for faculty.
        Evaluation evaluation = findEvaluationWithScores(evaluationId);
        return generateCsvBytes(List.of(evaluation));
    }

    public byte[] exportEvaluationToPdf(Long evaluationId) {
        // Single evaluation exports are inherently identifying; keep as-is, or remove this endpoint for faculty.
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

    /**
     * Faculty-facing / professor-facing CSV export:
     * - No student identifiers
     * - No timestamps
     * - No DB ids (use Response 1..N)
     * - Includes section + scrubbed feedback + scores summary
     * - Rows randomized to reduce correlation risk
     */
    private byte[] generateCsvBytes(List<Evaluation> evaluations) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            writer.writeNext(new String[]{
                    "Response",
                    "Section",
                    "Feedback",
                    "Scores summary"
            });

            List<Evaluation> randomized = new ArrayList<>(evaluations == null ? List.of() : evaluations);
            Collections.shuffle(randomized, new Random());

            int responseNum = 1;
            for (Evaluation evaluation : randomized) {
                writer.writeNext(new String[]{
                        "Response " + responseNum,
                        safe(evaluation.getSection()),
                        decryptForExport(evaluation),
                        buildScoresSummary(evaluation)
                });
                responseNum++;
            }

            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export CSV", ex);
        }
    }

    /**
     * Faculty-facing / professor-facing PDF export (report-style):
     * - Title + generated time + response count
     * - Overall performance score (avg of per-evaluation averages)
     * - Per-criterion aggregated stats: mean, median, std dev
     * - Randomized, anonymized comments
     * - No student identifiers
     */
    private byte[] generatePdfBytes(String title, List<Evaluation> evaluations) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            List<Evaluation> safeEvals = evaluations == null ? List.of() : evaluations;

            // Header
            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph("Generated on: " + OffsetDateTime.now(), normalFont));
            document.add(new Paragraph("Responses: " + safeEvals.size(), normalFont));
            document.add(new Paragraph(" "));

            // Overall performance score (avg of per-evaluation averages)
            double overall = safeEvals.stream().mapToDouble(this::overallAverageScore).average().orElse(0.0);
            document.add(new Paragraph("Performance Score", sectionFont));
            document.add(new Paragraph(String.format(Locale.ROOT, "%.2f / 10", overall), normalFont));
            document.add(new Paragraph(" "));

            // Per-criterion aggregated stats (mean/median/stddev)
            document.add(new Paragraph("Aggregated Quantitative Scores (per criterion)", sectionFont));

            Map<String, List<Double>> byCriterion = new LinkedHashMap<>();
            for (Evaluation e : safeEvals) {
                if (e.getScores() == null) continue;
                for (EvaluationScore s : e.getScores()) {
                    String key = criterionTitle(s);
                    double val = (s == null) ? 0.0 : (double) s.getScore();;
                    byCriterion.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
                }
            }

            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            statsTable.setWidths(new float[]{3.0f, 1.2f, 1.2f, 1.4f});

            statsTable.addCell("Criterion");
            statsTable.addCell("Mean");
            statsTable.addCell("Median");
            statsTable.addCell("Std Dev");

            for (Map.Entry<String, List<Double>> entry : byCriterion.entrySet()) {
                List<Double> vals = entry.getValue();
                double mean = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                statsTable.addCell(entry.getKey());
                statsTable.addCell(String.format(Locale.ROOT, "%.2f", mean));
                statsTable.addCell(String.format(Locale.ROOT, "%.2f", median(vals)));
                statsTable.addCell(String.format(Locale.ROOT, "%.2f", stddev(vals)));
            }

            document.add(statsTable);
            document.add(new Paragraph(" "));

            // Randomized comment list
            document.add(new Paragraph("Detailed Student Feedback (randomized)", sectionFont));

            List<Evaluation> randomized = new ArrayList<>(safeEvals);
            Collections.shuffle(randomized, new Random());

            int i = 1;
            for (Evaluation e : randomized) {
                String comment = decryptForExport(e);
                if (comment == null || comment.isBlank()) {
                    comment = "No comment provided.";
                }
                document.add(new Paragraph("Anonymous Participant " + i, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                document.add(new Paragraph("\"" + comment + "\"", normalFont));
                document.add(new Paragraph(" "));
                i++;
            }

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export PDF", ex);
        }
    }

    private double overallAverageScore(Evaluation e) {
        if (e == null || e.getScores() == null || e.getScores().isEmpty()) return 0.0;
        return e.getScores().stream()
                .mapToDouble(s -> ((s == null) ? 0.0 : (double) s.getScore()))
                .average()
                .orElse(0.0);
    }

    private double median(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        List<Double> copy = new ArrayList<>(values);
        Collections.sort(copy);
        int n = copy.size();
        if (n % 2 == 1) return copy.get(n / 2);
        return (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
    }

    private double stddev(List<Double> values) {
        if (values == null || values.size() < 2) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .sum() / (values.size() - 1); // sample std dev
        return Math.sqrt(variance);
    }

    private String decryptForExport(Evaluation evaluation) {
        try {
            String decrypted = evaluationService.decryptFeedback(
                    evaluation.getCiphertext(),
                    evaluation.getStudentPublicKey(),
                    evaluation.getIv()
            );
            return redactPii(decrypted);
        } catch (Exception ignored) {
            return DECRYPTION_FAILED;
        }
    }

    /**
     * Basic PII scrubbing to protect anonymity if a student types names/emails/ids in feedback.
     * This is heuristic-based; for strong guarantees, consider an NLP redaction library/service.
     */
    private String redactPii(String text) {
        if (text == null) return "";

        String redacted = text;

        // email addresses
        redacted = redacted.replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[REDACTED_EMAIL]");

        // long digit sequences (student numbers / phones / IDs)
        redacted = redacted.replaceAll("\\b\\d{6,}\\b", "[REDACTED_NUMBER]");

        // crude "my name is X" pattern
        redacted = redacted.replaceAll("(?i)\\bmy\\s+name\\s+is\\s+[A-Z][a-z]+\\b", "my name is [REDACTED_NAME]");

        return redacted;
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
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}