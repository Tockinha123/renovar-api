package com.tocka.renovarAPI.report;

import java.time.YearMonth;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tocka.renovarAPI.patient.Patient;
import com.tocka.renovarAPI.patient.PatientRepository;
import com.tocka.renovarAPI.user.User;

@Component
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

    private final ReportService reportService;
    private final PatientRepository patientRepository;

    public ReportScheduler(ReportService reportService, PatientRepository patientRepository) {
        this.reportService = reportService;
        this.patientRepository = patientRepository;
    }

    /**
     * Executa todo DIA 28 às 00:01 (horário de Brasília).
     * Gera relatório do MÊS ATUAL para todos os pacientes ativos.
     * 
     * Exemplo:
     * - 28/12/2024 → Gera relatório de Dezembro/2024
     * - 28/01/2025 → Gera relatório de Janeiro/2025
     */
    @Scheduled(cron = "0 1 0 28 * ?", zone = "America/Sao_Paulo")
    @Transactional
    public void generateMonthlyReportsForAllPatients() {
        log.info("🕐 Iniciando geração automática de relatórios mensais...");

        // ✅ Gera o relatório do MÊS ATUAL (não do mês anterior)
        YearMonth mesAtual = YearMonth.now();
        int month = mesAtual.getMonthValue();
        int year = mesAtual.getYear();

        log.info("📅 Gerando relatórios para: {}/{}", month, year);

        List<Patient> patients = patientRepository.findAll();
        int sucessos = 0;
        int falhas = 0;

        for (Patient patient : patients) {
            try {
                User user = patient.getUser();
                
                // Verifica se já existe relatório para este mês
                if (reportService.reportExists(patient, month, year)) {
                    log.info("⏭️  Relatório já existe para paciente {} ({}/{})", 
                            patient.getName(), month, year);
                    continue;
                }

                // Gera o relatório
                reportService.generateMonthlyReport(user, month, year);
                sucessos++;
                
                log.info("✅ Relatório gerado para paciente: {} ({}/{})", 
                        patient.getName(), month, year);

            } catch (Exception e) {
                falhas++;
                log.error("❌ Erro ao gerar relatório para paciente {}: {}", 
                        patient.getName(), e.getMessage());
            }
        }

        log.info("🏁 Geração de relatórios concluída: {} sucessos, {} falhas", sucessos, falhas);
    }

    /**
     * MÉTODO AUXILIAR: Executa manualmente a geração (útil para testes).
     * Remova ou comente após testar.
     */
    //@Scheduled(cron = "0 */2 * * * ?") // A cada 2 minutos (APENAS PARA TESTE)
    public void testGenerateReportsEveryTwoMinutes() {
        log.info("🧪 [TESTE] Executando geração de relatórios...");
        generateMonthlyReportsForAllPatients();
    }
}