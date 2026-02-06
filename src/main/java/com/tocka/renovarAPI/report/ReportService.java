package com.tocka.renovarAPI.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tocka.renovarAPI.bets.Bet;
import com.tocka.renovarAPI.bets.BetRepository;
import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.patient.PatientRepository;
import com.tocka.renovarAPI.score.repository.ScoreHistoryRepository;
import com.tocka.renovarAPI.user.User;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;

@Service
public class ReportService {

    @Value("${minio.url}")
    private String internalEndpoint;

    @Value("${minio.external-url}")
    private String externalEndpoint;

    @Value("${minio.bucket}")
    private String bucketName;

    private final ReportRepository reportRepository;
    private final PatientRepository patientRepository;
    private final BetRepository betRepository;
    private final PdfGeneratorService pdfGenerator;
    private final MinioClient minioClient;
    private final ScoreHistoryRepository scoreHistoryRepository;

    public ReportService(ReportRepository reportRepository,
                         PatientRepository patientRepository,
                         BetRepository betRepository,
                         PdfGeneratorService pdfGenerator,
                         MinioClient minioClient,
                         ScoreHistoryRepository scoreHistoryRepository) {
        this.reportRepository = reportRepository;
        this.patientRepository = patientRepository;
        this.betRepository = betRepository;
        this.pdfGenerator = pdfGenerator;
        this.minioClient = minioClient;
        this.scoreHistoryRepository = scoreHistoryRepository;
    }

    // ==================== LÓGICA DE PERÍODO E ACESSO ====================

    /**
     * Determina o mês de referência do relatório baseado no ciclo dia 28 a dia 27.
     * 
     * Exemplos:
     * - Hoje = 15/02/2025 → período ativo = 28/01 a 27/02 → referência = Janeiro
     * - Hoje = 28/02/2025 → período ativo = 28/02 a 27/03 → referência = Fevereiro
     * - Hoje = 05/01/2025 → período ativo = 28/12 a 27/01 → referência = Dezembro/2024
     */
    public YearMonth determineCurrentReportReference() {
        LocalDate hoje = LocalDate.now();
        int dayOfMonth = hoje.getDayOfMonth();

        if (dayOfMonth >= 28) {
            // Estamos no início do novo ciclo (dia 28+), referência é o mês atual
            return YearMonth.from(hoje);
        } else {
            // Estamos entre dia 1-27, referência é o mês anterior
            return YearMonth.from(hoje.minusMonths(1));
        }
    }

    /**
     * Determina o primeiro mês de referência válido para o paciente.
     * 
     * Regra: se o paciente criou a conta antes do dia 28 do mês X,
     * ele tem acesso ao relatório de X em diante.
     * Se criou no dia 28 ou depois, o primeiro relatório válido é X+1.
     * 
     * Exemplo:
     * - Conta criada 05/01 → primeiro relatório = Janeiro (gerado dia 28/01)
     * - Conta criada 28/01 → primeiro relatório = Fevereiro (gerado dia 28/02)
     * - Conta criada 29/01 → primeiro relatório = Fevereiro (gerado dia 28/02)
     */
    private YearMonth getFirstValidReportMonth(Patient patient) {
        LocalDateTime createdAt = patient.getCreatedAt();
        if (createdAt == null) {
            throw new RuntimeException("Não foi possível determinar a data de criação da conta.");
        }

        LocalDate creationDate = createdAt.toLocalDate();

        if (creationDate.getDayOfMonth() < 28) {
            // Entrou antes do dia 28 → já pega o relatório desse mês
            return YearMonth.from(creationDate);
        } else {
            // Entrou dia 28+ → primeiro relatório é o mês seguinte
            return YearMonth.from(creationDate).plusMonths(1);
        }
    }

    /**
     * Verifica se o paciente pode acessar o relatório de um determinado mês.
     */
    private boolean canAccessReport(Patient patient, YearMonth referenceMonth) {
        YearMonth firstValid = getFirstValidReportMonth(patient);
        return !referenceMonth.isBefore(firstValid);
    }

    /**
     * Endpoint inteligente: obtém ou gera o relatório do período atual.
     * 
     * Fluxo:
     * 1. Determina qual mês de referência é relevante
     * 2. Verifica se o paciente já estava ativo nesse período
     * 3. Se o relatório já existe → retorna URL de download
     * 4. Se não existe → gera, salva e retorna URL de download
     */
    @Transactional
    public DownloadUrlDTO getOrGenerateCurrentReport(User user)
            throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
                   InternalException, InvalidResponseException, NoSuchAlgorithmException,
                   XmlParserException, ServerException, IllegalArgumentException, IOException {

        Patient patient = getPatient(user);
        YearMonth referenceMonth = determineCurrentReportReference();

        // Valida se o paciente já estava ativo nesse período
        if (!canAccessReport(patient, referenceMonth)) {
            YearMonth firstValid = getFirstValidReportMonth(patient);
            String monthName = firstValid.getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"));
            throw new RuntimeException(
                String.format(
                    "Seu primeiro relatório será o de %s/%d. " +
                    "Continue usando a plataforma e ele estará disponível em breve!",
                    monthName, firstValid.getYear()
                )
            );
        }

        // Tenta encontrar relatório existente para esse período
        Optional<Report> existingReport = reportRepository
                .findByPatientAndReferenceMonthAndReferenceYear(
                    patient,
                    referenceMonth.getMonthValue(),
                    referenceMonth.getYear()
                );

        Report report;

        if (existingReport.isPresent()) {
            report = existingReport.get();

            // Se deu erro na geração anterior, tenta gerar novamente
            if (report.getStatus() == ReportStatus.ERROR) {
                reportRepository.delete(report);
                reportRepository.flush();
                ReportResponseDTO generated = generateMonthlyReport(
                    user, referenceMonth.getMonthValue(), referenceMonth.getYear());
                report = reportRepository.findById(generated.id())
                        .orElseThrow(() -> new RuntimeException("Erro ao recuperar relatório gerado"));
            }

            if (report.getStatus() == ReportStatus.GENERATING) {
                throw new RuntimeException("Seu relatório está sendo gerado. Tente novamente em alguns instantes.");
            }

        } else {
            // Não existe → gera automaticamente
            ReportResponseDTO generated = generateMonthlyReport(
                user, referenceMonth.getMonthValue(), referenceMonth.getYear());
            report = reportRepository.findById(generated.id())
                    .orElseThrow(() -> new RuntimeException("Erro ao recuperar relatório gerado"));
        }

        // Gera URL pré-assinada para download
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("response-content-type", "application/pdf");

        String internalUrl = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(report.getS3Key())
                .expiry(2, TimeUnit.HOURS)
                .extraQueryParams(reqParams)
                .build()
        );

        String externalUrl = internalUrl.replace(internalEndpoint, externalEndpoint);
        return new DownloadUrlDTO(externalUrl);
    }

    // ==================== LISTAGEM E RELATÓRIOS ====================

    @Transactional(readOnly = true)
    public List<ReportResponseDTO> listReports(User user) {
        Patient patient = getPatient(user);
        LocalDate hoje = LocalDate.now();
        
        return reportRepository.findByPatientOrderByReferenceYearDescReferenceMonthDesc(patient)
                .stream()
                .map(report -> {
                    // ✅ CORREÇÃO: Disponível a partir do dia 28 do mês de referência
                    LocalDate dataLiberacao = report.getReferenceYearMonth().atDay(28);
                    boolean available = report.getStatus() == ReportStatus.COMPLETED 
                            && !hoje.isBefore(dataLiberacao);
                    return new ReportResponseDTO(report, available);
                })
                .toList();
    }


@Transactional
    public ReportResponseDTO generateMonthlyReport(User user, int month, int year) {
        Patient patient = getPatient(user);
        YearMonth mesSolicitado = YearMonth.of(year, month);
        YearMonth mesAtual = YearMonth.now();

        // Não pode gerar relatório de meses futuros
        if (mesSolicitado.isAfter(mesAtual)) {
            throw new RuntimeException("Não é possível gerar relatório de meses futuros");
        }

        // Verifica se já existe relatório para este mês
        if (reportRepository.existsByPatientAndReferenceMonthAndReferenceYear(patient, month, year)) {
            throw new RuntimeException("Relatório para este mês já foi gerado");
        }

        // Cria registro do relatório
        String fileName = generateFileName(patient, month, year);
        String s3Key = generateS3Key(patient, month, year, fileName);

        Report report = Report.builder()
                .patient(patient)
                .fileName(fileName)
                .s3Key(s3Key)
                .referenceMonth(month)
                .referenceYear(year)
                .status(ReportStatus.GENERATING)
                .build();

        reportRepository.save(report);

        try {
            // Calcula os dados do relatório
            MonthlyReportDataDTO reportData = calculateMonthlyData(patient, mesSolicitado);
            
            // Gera o PDF
            byte[] pdfContent = pdfGenerator.generateMonthlyReportPdf(reportData);
            
            ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfContent);

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(s3Key)
                    .stream(inputStream, inputStream.available(), -1)
                    .contentType("application/pdf")
                    .build()
            );
            
            report.setStatus(ReportStatus.COMPLETED);
            reportRepository.save(report);

        } catch (Exception e) {
            report.setStatus(ReportStatus.ERROR);
            reportRepository.save(report);
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage(), e);
        }

        // ✅ CORREÇÃO: Disponível a partir do dia 28 do mês de referência
        LocalDate dataLiberacao = mesSolicitado.atDay(28);
        LocalDate hoje = LocalDate.now();
        boolean available = !hoje.isBefore(dataLiberacao);
        
        return new ReportResponseDTO(report, available);
    }

    /**
     * Gera relatório do mês anterior (chamado automaticamente ou manualmente).
     */
    @Transactional
    public ReportResponseDTO generatePreviousMonthReport(User user) {
        YearMonth mesAnterior = YearMonth.now().minusMonths(1);
        return generateMonthlyReport(user, mesAnterior.getMonthValue(), mesAnterior.getYear());
    }

    /**
     * Obtém URL para download do relatório.
     * Só permite download de relatórios de meses anteriores ao atual.
     */
// ...existing code...
    @Transactional(readOnly = true)
    public String getDownloadUrl(User user, String reportId) throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException, XmlParserException, ServerException, IllegalArgumentException, IOException {
        Patient patient = getPatient(user);
        
        Report report = reportRepository.findByIdAndPatient(
                java.util.UUID.fromString(reportId), patient)
                .orElseThrow(() -> new RuntimeException("Relatório não encontrado"));

        if (report.getStatus() != ReportStatus.COMPLETED) {
            throw new RuntimeException("Relatório ainda não está pronto");
        }

        //Download disponível a partir do dia 28 do mês de referência
        YearMonth mesRelatorio = report.getReferenceYearMonth();
        LocalDate dataLiberacao = mesRelatorio.atDay(28);
        LocalDate hoje = LocalDate.now();

        if (hoje.isBefore(dataLiberacao)) {
            throw new RuntimeException(
                "Este relatório estará disponível para download a partir de " 
                + dataLiberacao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
        }

        Map<String, String> reqParams = new HashMap<String, String>();
        reqParams.put("response-content-type", "application/pdf");

        String internalUrl = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(report.getS3Key())
                .expiry(2, TimeUnit.HOURS)
                .extraQueryParams(reqParams)
                .build()
        );

        String externalUrl = internalUrl.replace(internalEndpoint, externalEndpoint);

        return externalUrl; 
    }

// ...existing code...
    /**
     * Retorna o relatório do mês atual (se existir) ou null.
     */
    @Transactional(readOnly = true)
    public ReportResponseDTO getCurrentMonthReport(User user) {
        Patient patient = getPatient(user);
        YearMonth mesAtual = YearMonth.now();
        
        return reportRepository.findByPatientAndReferenceMonthAndReferenceYear(
                patient, mesAtual.getMonthValue(), mesAtual.getYear())
                .map(report -> new ReportResponseDTO(report, false))
                .orElse(null);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Patient getPatient(User user) {
        return patientRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
    }

    private MonthlyReportDataDTO calculateMonthlyData(Patient patient, YearMonth mes) {
        LocalDateTime inicioMes = mes.atDay(1).atStartOfDay();
        LocalDateTime fimMes = mes.atEndOfMonth().atTime(23, 59, 59);

        // Busca apostas do mês
        List<Bet> apostasDoMes = betRepository.findByPatientAndCreatedAtBetween(
                patient, inicioMes, fimMes);

        int quantidadeApostas = apostasDoMes.size();
        
        // Calcula maior streak do mês
        int maiorStreak = calcularMaiorStreakDoMes(patient, mes, apostasDoMes);
        
        // Calcula economia e horas
        int diasLimposNoMes = mes.lengthOfMonth() - contarDiasComAposta(apostasDoMes);
        BigDecimal economia = patient.getFinancialBaseline()
                .multiply(BigDecimal.valueOf(diasLimposNoMes));
        long horasSalvas = (long) diasLimposNoMes * patient.getSessionTimeBaseline() / 60;

        Optional<Double> mediaJaneiro = scoreHistoryRepository
            .findAverageScoreByPatientAndMonth(patient, mes.getYear(), mes.getMonthValue());

        double media = mediaJaneiro.orElse(0.0);

        
        // Frase motivacional baseada no desempenho
        String frase = gerarFraseMotivacional(quantidadeApostas, maiorStreak, media);

        return new MonthlyReportDataDTO(
            patient.getName(),
            mes.getMonthValue(),
            mes.getYear(),
            maiorStreak,
            horasSalvas,
            economia,
            quantidadeApostas,
            media,
            frase
        );
    }

    private int calcularMaiorStreakDoMes(Patient patient, YearMonth mes, List<Bet> apostas) {
        if (apostas.isEmpty()) {
            return mes.lengthOfMonth(); // Mês inteiro limpo
        }

        // Cria set de dias com aposta
        var diasComAposta = apostas.stream()
                .map(bet -> bet.getCreatedAt().toLocalDate())
                .collect(java.util.stream.Collectors.toSet());

        int maiorStreak = 0;
        int streakAtual = 0;

        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate data = mes.atDay(dia);
            
            if (data.isAfter(LocalDate.now())) {
                break; // Não conta dias futuros
            }
            
            if (!diasComAposta.contains(data)) {
                streakAtual++;
                maiorStreak = Math.max(maiorStreak, streakAtual);
            } else {
                streakAtual = 0;
            }
        }

        return maiorStreak;
    }

    private int contarDiasComAposta(List<Bet> apostas) {
        return (int) apostas.stream()
                .map(bet -> bet.getCreatedAt().toLocalDate())
                .distinct()
                .count();
    }

    private String gerarFraseMotivacional(int apostas, int streak, double score) {
        if (apostas == 0) {
            return "Parabéns! Você passou o mês inteiro sem apostar. Continue assim, você está no caminho certo!";
        }
        
        if (score >= 701) {
            return "Excelente progresso! Sua determinação está fazendo a diferença. Continue firme!";
        }
        
        if (score >= 501) {
            return "Você está indo bem! Cada dia sem apostar é uma vitória. Mantenha o foco!";
        }
        
        if (score >= 301) {
            return "Não desista! Reconhecer o problema é o primeiro passo. Você é mais forte do que imagina.";
        }
        
        return "Lembre-se: recaídas fazem parte da jornada. O importante é não desistir. Busque ajuda se precisar!";
    }

    private String generateFileName(Patient patient, int month, int year) {
        String cleanName = patient.getName().replaceAll("[^a-zA-Z0-9]", "_");
        return String.format("relatorio_%s_%02d_%d.pdf", cleanName, month, year);
    }

    private String generateS3Key(Patient patient, int month, int year, String fileName) {
        return String.format("reports/%s/%d/%02d/%s", 
                patient.getId(), year, month, fileName);
    }


    public boolean reportExists(Patient patient, int month, int year) {
        return reportRepository.existsByPatientAndReferenceMonthAndReferenceYear(
                patient, month, year);
    }
}