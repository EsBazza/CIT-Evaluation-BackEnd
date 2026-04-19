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
import java.util.Set;
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

    // =========================================================
    // EXISTING EXPORTS (FACULTY/PROFESSOR) - UNCHANGED OUTPUT
    // =========================================================

    public byte[] exportAllToCsv() {
        return exportAdminDashboardOverallToCsv("ALL");
    }

    public byte[] exportAllToPdf() {
        // Keeps your existing "report-style" layout.
        return exportAdminDashboardOverallToPdf("ALL");
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
        List<Evaluation> evaluations = evaluationRepository.findByFacultyEmailWithScores(
                normalizeRequiredEmail(professor.getEmail())
        );
        return generateCsvBytes(evaluations);
    }

    public byte[] exportProfessorToPdf(Long professorId) {
        Professor professor = findProfessor(professorId);
        List<Evaluation> evaluations = evaluationRepository.findByFacultyEmailWithScores(
                normalizeRequiredEmail(professor.getEmail())
        );
        return generatePdfBytes("Professor Evaluation Report: " + safe(professor.getName()), evaluations);
    }

    // =========================================================
    // ADMIN DASHBOARD EXPORTS (NEW) - DIFFERENT LAYOUT + CONTENT
    // =========================================================

    /**
     * Admin: Overall Evaluation Insights PDF (dashboard-based content).
     * @param sectionOrNull "ALL" or null => all sections, otherwise filter by section (e.g., "3-A")
     */
    public byte[] exportAdminDashboardOverallToPdf(String sectionOrNull) {
        List<Evaluation> evaluations = evaluationRepository.findAllWithScores();
        List<Evaluation> filtered = filterBySectionIfProvided(evaluations, sectionOrNull);

        AdminDashboardMetrics metrics = computeAdminDashboardMetrics(filtered, sectionOrNull);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4); // portrait like your screenshot
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("Overall Evaluation Insights", titleFont));
            document.add(new Paragraph("Generated on: " + OffsetDateTime.now(), normalFont));
            document.add(new Paragraph("Section: " + metrics.sectionLabel, normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Summary", sectionFont));
            document.add(new Paragraph("Submissions: " + metrics.submissions, normalFont));

            // both averages (as requested)
            document.add(new Paragraph(String.format(Locale.ROOT,
                    "Average Score (all criteria): %.2f / 10", metrics.avgAllCriteria), normalFont));
            document.add(new Paragraph(String.format(Locale.ROOT,
                    "Performance Score (avg per evaluation): %.2f / 10", metrics.avgPerEvaluation), normalFont));

            document.add(new Paragraph("Ranked Teachers: " + metrics.rankedTeachers, normalFont));
            document.add(new Paragraph(
                    String.format(Locale.ROOT, "Best Teacher: %s (%.2f)",
                            safe(metrics.bestTeacherName),
                            metrics.bestTeacherScore),
                    normalFont
            ));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Faculty Ranking (Top 10)", sectionFont));

            PdfPTable rankingTable = new PdfPTable(4);
            rankingTable.setWidthPercentage(100);
            rankingTable.setWidths(new float[]{0.8f, 3.0f, 1.2f, 1.4f});

            rankingTable.addCell("Rank");
            rankingTable.addCell("Professor");
            rankingTable.addCell("Responses");
            rankingTable.addCell("Avg Score");

            int rank = 1;
            for (TeacherRankRow row : metrics.rankingTop10) {
                rankingTable.addCell(String.valueOf(rank));
                rankingTable.addCell(safe(row.professorName));
                rankingTable.addCell(String.valueOf(row.responses));
                rankingTable.addCell(String.format(Locale.ROOT, "%.2f", row.avgScore));
                rank++;
            }

            document.add(rankingTable);

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export Admin Dashboard PDF", ex);
        }
    }

    /**
     * Admin: Overall Evaluation Insights CSV (dashboard-based content).
     * @param sectionOrNull "ALL" or null => all sections, otherwise filter by section
     */
    public byte[] exportAdminDashboardOverallToCsv(String sectionOrNull) {
        List<Evaluation> evaluations = evaluationRepository.findAllWithScores();
        List<Evaluation> filtered = filterBySectionIfProvided(evaluations, sectionOrNull);

        AdminDashboardMetrics metrics = computeAdminDashboardMetrics(filtered, sectionOrNull);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Add UTF-8 BOM so Excel detects encoding correctly
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                writer.writeNext(new String[]{"Overall Evaluation Insights"});
                writer.writeNext(new String[]{"Generated on", OffsetDateTime.now().toString()});
                writer.writeNext(new String[]{"Section", metrics.sectionLabel});
                writer.writeNext(new String[]{"Submissions", String.valueOf(metrics.submissions)});
                writer.writeNext(new String[]{"Average Score (all criteria)", String.format(Locale.ROOT, "%.2f", metrics.avgAllCriteria)});
                writer.writeNext(new String[]{"Performance Score (avg per evaluation)", String.format(Locale.ROOT, "%.2f", metrics.avgPerEvaluation)});
                writer.writeNext(new String[]{"Ranked Teachers", String.valueOf(metrics.rankedTeachers)});
                writer.writeNext(new String[]{"Best Teacher", safe(metrics.bestTeacherName)});
                writer.writeNext(new String[]{"Best Teacher Score", String.format(Locale.ROOT, "%.2f", metrics.bestTeacherScore)});
                writer.writeNext(new String[]{}); // blank row

                writer.writeNext(new String[]{"Rank", "Professor", "Responses", "Avg Score"});
                int rank = 1;
                for (TeacherRankRow row : metrics.rankingTop10) {
                    writer.writeNext(new String[]{
                            String.valueOf(rank),
                            safe(row.professorName),
                            String.valueOf(row.responses),
                            String.format(Locale.ROOT, "%.2f", row.avgScore)
                    });
                    rank++;
                }
            }

            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to export Admin Dashboard CSV", ex);
        }
    }

    // =========================================================
    // EXISTING PRIVATE HELPERS
    // =========================================================

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
     * Faculty-facing / professor-facing CSV export (UNCHANGED).
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
     * Faculty-facing / professor-facing PDF export (UNCHANGED).
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

            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph("Generated on: " + OffsetDateTime.now(), normalFont));
            document.add(new Paragraph("Responses: " + safeEvals.size(), normalFont));
            document.add(new Paragraph(" "));

            double overall = safeEvals.stream().mapToDouble(this::overallAverageScore).average().orElse(0.0);
            document.add(new Paragraph("Performance Score", sectionFont));
            document.add(new Paragraph(String.format(Locale.ROOT, "%.2f / 10", overall), normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Aggregated Quantitative Scores (per criterion)", sectionFont));

            Map<String, List<Double>> byCriterion = new LinkedHashMap<>();
            for (Evaluation e : safeEvals) {
                if (e.getScores() == null) continue;
                for (EvaluationScore s : e.getScores()) {
                    String key = criterionTitle(s);
                    double val = (s == null) ? 0.0 : (double) s.getScore();
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
                .sum() / (values.size() - 1);
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

    private String redactPii(String text) {
        if (text == null) return "";

        String redacted = text;
        redacted = redacted.replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[REDACTED_EMAIL]");
        redacted = redacted.replaceAll("\\b\\d{6,}\\b", "[REDACTED_NUMBER]");
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

    // =========================================================
    // ADMIN DASHBOARD METRICS - PRIVATE HELPERS (NEW)
    // =========================================================

    private List<Evaluation> filterBySectionIfProvided(List<Evaluation> evals, String sectionOrNull) {
        if (evals == null) return List.of();
        if (sectionOrNull == null || sectionOrNull.isBlank() || "ALL".equalsIgnoreCase(sectionOrNull.trim())) {
            return evals;
        }
        String sec = sectionOrNull.trim().toUpperCase(Locale.ROOT);
        return evals.stream()
                .filter(e -> e != null
                        && e.getSection() != null
                        && e.getSection().trim().toUpperCase(Locale.ROOT).equals(sec))
                .toList();
    }

    private AdminDashboardMetrics computeAdminDashboardMetrics(List<Evaluation> evals, String sectionOrNull) {
        List<Evaluation> safe = evals == null ? List.of() : evals;

        int submissions = safe.size();

        // A) dashboard avg (flattened across all criterion scores)
        double avgAllCriteria = overallDashboardAverageScore(safe);

        // B) avg of per-evaluation averages
        double avgPerEvaluation = safe.stream().mapToDouble(this::overallAverageScore).average().orElse(0.0);

        // facultyEmail -> accumulator
        Map<String, TeacherAccumulator> acc = new LinkedHashMap<>();
        for (Evaluation e : safe) {
            if (e == null) continue;
            String facultyEmail = e.getFacultyEmail();
            if (facultyEmail == null || facultyEmail.isBlank()) continue;

            String normalizedEmail = facultyEmail.trim().toLowerCase(Locale.ROOT);
            TeacherAccumulator t = acc.computeIfAbsent(normalizedEmail, k -> new TeacherAccumulator());

            t.responses++;

            if (e.getScores() != null) {
                for (EvaluationScore s : e.getScores()) {
                    if (s == null) continue;
                    t.sum += (double) s.getScore();
                    t.count++;
                }
            }
        }

        Map<String, String> emailToName = resolveProfessorNames(acc.keySet());

        List<TeacherRankRow> rankingAll = acc.entrySet().stream()
                .map(entry -> {
                    String email = entry.getKey();
                    TeacherAccumulator t = entry.getValue();
                    double avg = t.count == 0 ? 0.0 : (t.sum / t.count);
                    String name = emailToName.getOrDefault(email, email);
                    return new TeacherRankRow(email, name, t.responses, avg);
                })
                .sorted(Comparator.comparingDouble((TeacherRankRow r) -> r.avgScore).reversed())
                .toList();

        int rankedTeachers = rankingAll.size();

        List<TeacherRankRow> rankingTop10 = rankingAll.stream().limit(10).toList();

        String bestTeacherName = rankingTop10.isEmpty() ? "N/A" : rankingTop10.get(0).professorName;
        double bestTeacherScore = rankingTop10.isEmpty() ? 0.0 : rankingTop10.get(0).avgScore;

        String sectionLabel =
                (sectionOrNull == null || sectionOrNull.isBlank() || "ALL".equalsIgnoreCase(sectionOrNull.trim()))
                        ? "All Sections"
                        : sectionOrNull.trim().toUpperCase(Locale.ROOT);

        return new AdminDashboardMetrics(
                sectionLabel,
                submissions,
                avgAllCriteria,
                avgPerEvaluation,
                rankedTeachers,
                bestTeacherName,
                bestTeacherScore,
                rankingTop10
        );
    }

    private double overallDashboardAverageScore(List<Evaluation> evals) {
        if (evals == null || evals.isEmpty()) return 0.0;

        double sum = 0.0;
        long count = 0;

        for (Evaluation e : evals) {
            if (e == null || e.getScores() == null) continue;
            for (EvaluationScore s : e.getScores()) {
                if (s == null) continue;
                sum += (double) s.getScore();
                count++;
            }
        }
        return count == 0 ? 0.0 : (sum / count);
    }

    /**
     * Looks up professor name by faculty email. Falls back to email when not found.
     * Requires ProfessorRepository.findByEmailIgnoreCase(...)
     */
    private Map<String, String> resolveProfessorNames(Set<String> normalizedEmails) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String email : normalizedEmails) {
            if (email == null || email.isBlank()) continue;

            Professor p = professorRepository.findByEmailIgnoreCase(email).orElse(null);
            if (p != null && p.getName() != null && !p.getName().isBlank()) {
                map.put(email, p.getName());
            }
        }
        return map;
    }

    private static class TeacherAccumulator {
        long responses = 0;
        double sum = 0.0;
        long count = 0;
    }

    private static class TeacherRankRow {
        final String facultyEmail;
        final String professorName;
        final long responses;
        final double avgScore;

        private TeacherRankRow(String facultyEmail, String professorName, long responses, double avgScore) {
            this.facultyEmail = facultyEmail;
            this.professorName = professorName;
            this.responses = responses;
            this.avgScore = avgScore;
        }
    }

    private static class AdminDashboardMetrics {
        final String sectionLabel;
        final int submissions;
        final double avgAllCriteria;
        final double avgPerEvaluation;
        final int rankedTeachers;
        final String bestTeacherName;
        final double bestTeacherScore;
        final List<TeacherRankRow> rankingTop10;

        private AdminDashboardMetrics(String sectionLabel,
                                      int submissions,
                                      double avgAllCriteria,
                                      double avgPerEvaluation,
                                      int rankedTeachers,
                                      String bestTeacherName,
                                      double bestTeacherScore,
                                      List<TeacherRankRow> rankingTop10) {
            this.sectionLabel = sectionLabel;
            this.submissions = submissions;
            this.avgAllCriteria = avgAllCriteria;
            this.avgPerEvaluation = avgPerEvaluation;
            this.rankedTeachers = rankedTeachers;
            this.bestTeacherName = bestTeacherName;
            this.bestTeacherScore = bestTeacherScore;
            this.rankingTop10 = rankingTop10;
        }
    }
}