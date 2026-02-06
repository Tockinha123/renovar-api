package com.tocka.renovarAPI.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class PdfGeneratorService {

    private static final String[] MESES = {
        "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private String logoBase64;

    public PdfGeneratorService() {
        this.logoBase64 = loadLogoAsBase64();
    }

    private String loadLogoAsBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("static/logo.png");
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] imageBytes = inputStream.readAllBytes();
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (IOException e) {
            // Se não encontrar a logo, retorna null e usa texto como fallback
            System.err.println("Logo não encontrada em static/logo.png: " + e.getMessage());
            return null;
        }
    }

    public byte[] generateMonthlyReportPdf(MonthlyReportDataDTO data) {
        String html = buildHtmlReport(data);
        return convertHtmlToPdf(html);
    }

    private byte[] convertHtmlToPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF: " + e.getMessage(), e);
        }
    }

    private String buildHtmlReport(MonthlyReportDataDTO data) {
        String mesAno = MESES[data.referenceMonth()] + " de " + data.referenceYear();
        String dataGeracao = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));

        // Determina a cor do score
        String scoreColor = getScoreColor(data.scoreMedio());
        
        // Logo: se tiver Base64 usa imagem, senão usa texto
        String logoHtml;
        if (logoBase64 != null) {
            logoHtml = "<img src=\"data:image/png;base64," + logoBase64 + "\" class=\"logo-image\" />";
        } else {
            logoHtml = "<span class=\"header-logo\">Re:Novar</span>";
        }
        
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <style type="text/css">
                    @page {
                        size: A4;
                        margin: 20mm 15mm 20mm 15mm;
                    }
                    body {
                        font-family: Helvetica, Arial, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #FFFFFF;
                        color: #333333;
                    }
                    .container {
                        width: 100%%;
                        background-color: #FFFFFF;
                    }
                    .header {
                        width: 100%%;
                        background: linear-gradient(135deg, #007F95 0%%, #009580 100%%);
                        padding: 30px 0;
                        text-align: center;
                    }
                    .logo-image {
                        max-width: 180px;
                        max-height: 80px;
                    }
                    .header-logo {
                        color: white;
                        font-size: 42px;
                        font-weight: bold;
                    }
                    .welcome-section {
                        padding: 30px 20px;
                    }
                    .welcome-title {
                        color: #007F95;
                        font-size: 26px;
                        font-weight: bold;
                        margin: 0 0 15px 0;
                    }
                    .welcome-subtitle {
                        color: #555555;
                        font-size: 14px;
                        line-height: 1.6;
                        margin: 0;
                    }
                    .metrics-section {
                        background-color: #007F95;
                        padding: 25px 20px;
                    }
                    .metrics-grid {
                        width: 100%%;
                        border-collapse: collapse;
                    }
                    .metric-box {
                        text-align: center;
                        padding: 15px 10px;
                        width: 33.33%%;
                    }
                    .metric-label {
                        color: rgba(255, 255, 255, 0.9);
                        font-size: 12px;
                        margin-bottom: 8px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    .metric-value {
                        color: #FFFFFF;
                        font-size: 22px;
                        font-weight: bold;
                    }
                    .bets-section {
                        background-color: #005F70;
                        padding: 25px 20px;
                        text-align: center;
                    }
                    .bets-label {
                        color: rgba(255, 255, 255, 0.9);
                        font-size: 14px;
                        margin-bottom: 10px;
                    }
                    .bets-value {
                        color: #FFFFFF;
                        font-size: 42px;
                        font-weight: bold;
                    }
                    .score-section {
                        padding: 35px 20px;
                        text-align: center;
                        background-color: #F8F9FA;
                    }
                    .score-label {
                        color: #333333;
                        font-size: 16px;
                        margin-bottom: 10px;
                    }
                    .score-value {
                        font-size: 56px;
                        font-weight: bold;
                        margin: 10px 0;
                    }
                    .motivation-section {
                        padding: 30px 25px;
                        background-color: #FFFFFF;
                        border-left: 4px solid #009580;
                        margin: 20px;
                    }
                    .motivation-text {
                        color: #444444;
                        font-size: 16px;
                        font-style: italic;
                        text-align: center;
                        line-height: 1.7;
                        margin: 0;
                    }
                    .footer {
                        background-color: #009580;
                        padding: 20px;
                        text-align: center;
                        margin-top: 20px;
                    }
                    .footer-text {
                        color: #FFFFFF;
                        font-size: 11px;
                        margin: 0;
                    }
                    .divider {
                        height: 1px;
                        background-color: #E0E0E0;
                        margin: 0 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <!-- Header com Logo -->
                    <div class="header">
                        %s
                    </div>
                    
                    <!-- Welcome Section -->
                    <div class="welcome-section">
                        <h1 class="welcome-title">Olá, %s!</h1>
                        <p class="welcome-subtitle">
                            Bem-vindo ao seu Relatório Mensal de <strong>%s</strong>.<br/>
                            Confira abaixo as informações sobre o seu progresso neste período.
                        </p>
                    </div>
                    
                    <!-- Metrics Section -->
                    <div class="metrics-section">
                        <table class="metrics-grid" cellpadding="0" cellspacing="0">
                            <tr>
                                <td class="metric-box">
                                    <div class="metric-label">Maior Streak</div>
                                    <div class="metric-value">%d dias</div>
                                </td>
                                <td class="metric-box">
                                    <div class="metric-label">Horas Salvas</div>
                                    <div class="metric-value">%d horas</div>
                                </td>
                                <td class="metric-box">
                                    <div class="metric-label">Economia</div>
                                    <div class="metric-value">R$ %,.2f</div>
                                </td>
                            </tr>
                        </table>
                    </div>
                    
                    <!-- Bets Section -->
                    <div class="bets-section">
                        <div class="bets-label">Quantidade de Apostas Realizadas</div>
                        <div class="bets-value">%d</div>
                    </div>
                    
                    <!-- Score Section -->
                    <div class="score-section">
                        <div class="score-label">Seu score médio do mês foi:</div>
                        <div class="score-value" style="color: %s;">%.2f</div>
                    </div>
                    
                    <!-- Motivation Section -->
                    <div class="motivation-section">
                        <p class="motivation-text">"%s"</p>
                    </div>
                    
                    <!-- Footer -->
                    <div class="footer">
                        <p class="footer-text">
                            Relatório gerado automaticamente em %s<br/>
                            Re:Novar - Sua jornada de recuperação
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                logoHtml,
                data.patientName(),
                mesAno,
                data.maiorStreak(),
                data.horasSalvas(),
                data.dinheiroEconomizado(),
                data.quantidadeApostas(),
                scoreColor,
                data.scoreMedio(),
                data.fraseMotivar(),
                dataGeracao
            );
    }

    private String getScoreColor(double score) {
        if (score >= 701) return "#2E7D32"; // Verde escuro - Excelente
        if (score >= 501) return "#4CAF50"; // Verde - Bom
        if (score >= 301) return "#FFC107"; // Amarelo - Regular
        return "#F44336"; // Vermelho - Alto Risco
    }
}