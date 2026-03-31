package com.alonzo.citeval.service;

import com.alonzo.citeval.model.dto.EvaluationRequestDTO;
import com.alonzo.citeval.model.dto.EvaluationResponseDTO;
import com.alonzo.citeval.model.dto.EvaluationScoreDTO;
import com.alonzo.citeval.model.dto.ScoreRequestDTO;
import com.alonzo.citeval.model.entity.Criterion;
import com.alonzo.citeval.model.entity.Evaluation;
import com.alonzo.citeval.model.entity.EvaluationScore;
import com.alonzo.citeval.repository.CriterionRepository;
import com.alonzo.citeval.repository.EvaluationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EvaluationService {

    private static final String ALGO = "EC";
    private final EvaluationRepository evaluationRepository;
    private final CriterionRepository criterionRepository;
    private final KeyService keyService;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             CriterionRepository criterionRepository,
                             KeyService keyService) {
        this.evaluationRepository = evaluationRepository;
        this.criterionRepository = criterionRepository;
        this.keyService = keyService;
    }

    @Transactional
    public EvaluationResponseDTO submitEvaluation(EvaluationRequestDTO req) {
        Evaluation eval = new Evaluation();
        eval.setStudentNumber(req.studentNumber());
        eval.setFacultyEmail(req.facultyEmail());
        eval.setSection(req.section());
        eval.setRating(req.rating());
        eval.setStudentEmail(req.studentEmail());
        eval.setCiphertext(req.ciphertext());
        eval.setStudentPublicKey(req.studentPublicKey());
        eval.setIv(req.iv());

        if (req.scores() != null) {
            for (ScoreRequestDTO sr : req.scores()) {
                Criterion criterion = criterionRepository.findById(sr.criterionId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Criterion ID: " + sr.criterionId()));

                EvaluationScore scoreEntity = new EvaluationScore();
                scoreEntity.setEvaluation(eval);
                scoreEntity.setCriterion(criterion);
                scoreEntity.setScore(sr.score());

                eval.getScores().add(scoreEntity);
            }
        }

        Evaluation saved = evaluationRepository.save(eval);
        return mapToResponseDTO(saved);
    }

    public List<EvaluationResponseDTO> getAllEvaluations(String facultyEmail) {
        List<Evaluation> evaluations;
        if (facultyEmail != null && !facultyEmail.isBlank()) {
            evaluations = evaluationRepository.findByFacultyEmail(facultyEmail.trim());
        } else {
            evaluations = evaluationRepository.findAll();
        }

        return evaluations.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public EvaluationResponseDTO getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evaluation not found"));
    }

    public String decryptFeedback(String ciphertext, String studentPubKeyBase64, String ivBase64) throws Exception {
        PrivateKey serverPrivKey = keyService.getServerPrivateKey();

        byte[] pubKeyBytes = Base64.getDecoder().decode(studentPubKeyBase64);
        KeyFactory kf = KeyFactory.getInstance(ALGO);
        PublicKey studentPubKey = kf.generatePublic(new X509EncodedKeySpec(pubKeyBytes));

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(serverPrivKey);
        ka.doPhase(studentPubKey, true);
        byte[] sharedSecret = ka.generateSecret();

        byte[] aesKeyBytes = Arrays.copyOf(sharedSecret, 16);
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = Base64.getDecoder().decode(ivBase64);

        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        byte[] decodedCiphertext = Base64.getDecoder().decode(ciphertext);
        byte[] plaintext = cipher.doFinal(decodedCiphertext);

        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private EvaluationResponseDTO mapToResponseDTO(Evaluation eval) {
        List<EvaluationScoreDTO> scores = eval.getScores().stream()
                .map(s -> new EvaluationScoreDTO(
                        s.getId(),
                        s.getCriterion().getId(),
                        s.getCriterion().getTitle(),
                        s.getScore()
                ))
                .collect(Collectors.toList());

        return new EvaluationResponseDTO(
                eval.getId(),
                eval.getStudentNumber(),
                eval.getStudentEmail(),
                eval.getFacultyEmail(),
                eval.getSection(),
                eval.getRating(),
                eval.getCiphertext(),
                eval.getStudentPublicKey(),
                eval.getIv(),
                scores
        );
    }
}
