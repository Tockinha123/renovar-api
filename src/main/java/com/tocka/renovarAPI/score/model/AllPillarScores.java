package com.tocka.renovarAPI.score.model;

/**
 * Record holding ALL 6 pillar scores calculated together.
 * 
 * MUDANÇA DE DESIGN: Substitui o padrão anterior de BetPillarScores + AssessmentPillarScores.
 * Agora tanto apostas quanto avaliações recalculam TODOS os 6 pilares de uma vez,
 * ao invés de calcular metade e copiar a outra metade do último registro.
 * 
 * Isso resolve:
 * - P3 que ficava "preso" em 0 pra sempre (agora recupera na avaliação diária)
 * - P5 que ficava "preso" em 80 pra sempre (agora usa janela de 7 dias)
 * - P4 que não decaía quando o paciente sumia (agora decai pro neutro)
 */
public record AllPillarScores(
        int p1, // Abstinência Recente (max 290)
        int p2, // Consistência/Streak (max 240)
        int p3, // Intensidade Financeira (max 210)
        int p4, // Fissura/Craving (max 120)
        int p5, // Engajamento (max 80)
        int p6  // Prevenção (max 60)
) {
    public int total() {
        return p1 + p2 + p3 + p4 + p5 + p6;
    }
}