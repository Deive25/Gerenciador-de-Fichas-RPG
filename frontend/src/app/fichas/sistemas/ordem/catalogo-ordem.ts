import { AtributoOrdem, ClasseOrdem, GrauOrdem, PericiaOrdem } from '../../ficha.model';

export interface AtributoCatalogo {
  codigo: AtributoOrdem;
  nome: string;
  descricao: string;
}

export interface PericiaCatalogo {
  codigo: PericiaOrdem;
  nome: string;
  atributo: AtributoOrdem;
  descricao: string;
}

// Descrições curtas, escritas com palavras próprias (nada copiado dos livros).
export const ATRIBUTOS_ORDEM: readonly AtributoCatalogo[] = [
  { codigo: 'AGI', nome: 'Agilidade', descricao: 'Rapidez, equilíbrio e coordenação. Também soma na Defesa.' },
  { codigo: 'FOR', nome: 'Força', descricao: 'Potência dos músculos: golpes, saltos e carregar peso.' },
  { codigo: 'INT', nome: 'Intelecto', descricao: 'Raciocínio, memória e conhecimento acumulado.' },
  { codigo: 'PRE', nome: 'Presença', descricao: 'Força de personalidade, carisma e controle da própria mente.' },
  { codigo: 'VIG', nome: 'Vigor', descricao: 'Resistência do corpo a dor, cansaço, doenças e venenos.' },
];

// Em ordem alfabética, como a ficha mostra. O atributo é o que a perícia usa na rolagem.
export const PERICIAS_ORDEM: readonly PericiaCatalogo[] = [
  { codigo: 'ACROBACIA', nome: 'Acrobacia', atributo: 'AGI', descricao: 'Equilíbrio, piruetas, quedas e passagens apertadas.' },
  { codigo: 'ADESTRAMENTO', nome: 'Adestramento', atributo: 'PRE', descricao: 'Lidar com animais: acalmar, treinar e comandar.' },
  { codigo: 'ARTES', nome: 'Artes', atributo: 'PRE', descricao: 'Criar ou executar obras: pintar, tocar, atuar, escrever.' },
  { codigo: 'ATLETISMO', nome: 'Atletismo', atributo: 'FOR', descricao: 'Correr, saltar, escalar e nadar com esforço físico.' },
  { codigo: 'ATUALIDADES', nome: 'Atualidades', atributo: 'INT', descricao: 'Estar por dentro de notícias, cultura e curiosidades gerais.' },
  { codigo: 'CIENCIAS', nome: 'Ciências', atributo: 'INT', descricao: 'Saber científico: química, física, biologia e matemática.' },
  { codigo: 'CRIME', nome: 'Crime', atributo: 'AGI', descricao: 'Truques de ladrão: abrir fechaduras, bater carteiras, desarmar travas.' },
  { codigo: 'DIPLOMACIA', nome: 'Diplomacia', atributo: 'PRE', descricao: 'Convencer com educação, negociar e causar boa impressão.' },
  { codigo: 'ENGANACAO', nome: 'Enganação', atributo: 'PRE', descricao: 'Mentir, disfarçar-se e fazer os outros acreditarem no que não é.' },
  { codigo: 'FORTITUDE', nome: 'Fortitude', atributo: 'VIG', descricao: 'Aguentar esforço, dor, doenças e venenos sem ceder.' },
  { codigo: 'FURTIVIDADE', nome: 'Furtividade', atributo: 'AGI', descricao: 'Mover-se sem ser notado e esconder-se.' },
  { codigo: 'INICIATIVA', nome: 'Iniciativa', atributo: 'AGI', descricao: 'Reagir primeiro quando o perigo começa.' },
  { codigo: 'INTIMIDACAO', nome: 'Intimidação', atributo: 'PRE', descricao: 'Impor medo para forçar alguém a recuar ou obedecer.' },
  { codigo: 'INTUICAO', nome: 'Intuição', atributo: 'PRE', descricao: 'Perceber mentiras, intenções e o clima de uma situação.' },
  { codigo: 'INVESTIGACAO', nome: 'Investigação', atributo: 'INT', descricao: 'Procurar pistas, ligar fatos e descobrir o que aconteceu.' },
  { codigo: 'LUTA', nome: 'Luta', atributo: 'FOR', descricao: 'Combater corpo a corpo, com as mãos ou armas brancas.' },
  { codigo: 'MEDICINA', nome: 'Medicina', atributo: 'INT', descricao: 'Tratar ferimentos e doenças e entender o corpo humano.' },
  { codigo: 'OCULTISMO', nome: 'Ocultismo', atributo: 'INT', descricao: 'Conhecimento sobre o paranormal, seus símbolos e criaturas.' },
  { codigo: 'PERCEPCAO', nome: 'Percepção', atributo: 'PRE', descricao: 'Notar detalhes com os sentidos: ver, ouvir e farejar o que passa batido.' },
  { codigo: 'PILOTAGEM', nome: 'Pilotagem', atributo: 'AGI', descricao: 'Conduzir carros, barcos, aviões e outros veículos.' },
  { codigo: 'PONTARIA', nome: 'Pontaria', atributo: 'AGI', descricao: 'Acertar alvos à distância com armas de disparo ou arremesso.' },
  { codigo: 'PROFISSAO', nome: 'Profissão', atributo: 'INT', descricao: 'Saber técnico do ofício que o personagem exerce.' },
  { codigo: 'REFLEXOS', nome: 'Reflexos', atributo: 'AGI', descricao: 'Desviar de golpes, explosões e armadilhas no último instante.' },
  { codigo: 'RELIGIAO', nome: 'Religião', atributo: 'PRE', descricao: 'Conhecer crenças, rituais e tradições espirituais.' },
  { codigo: 'SOBREVIVENCIA', nome: 'Sobrevivência', atributo: 'INT', descricao: 'Se virar na natureza: orientar-se, rastrear e achar abrigo.' },
  { codigo: 'TATICA', nome: 'Tática', atributo: 'INT', descricao: 'Planejar e ler situações de combate e de risco.' },
  { codigo: 'TECNOLOGIA', nome: 'Tecnologia', atributo: 'INT', descricao: 'Mexer com computadores, eletrônicos e sistemas de segurança.' },
  { codigo: 'VONTADE', nome: 'Vontade', atributo: 'PRE', descricao: 'Resistir ao medo, à pressão e a influências sobre a mente.' },
];

const BONUS: Record<GrauOrdem, number> = { TREINADO: 5, VETERANO: 10, EXPERT: 15 };

/** Letra do selo ao lado do bônus. */
export const SELO_GRAU: Record<GrauOrdem, string> = { TREINADO: 'T', VETERANO: 'V', EXPERT: 'E' };

export const NOME_GRAU: Record<GrauOrdem, string> = { TREINADO: 'Treinado', VETERANO: 'Veterano', EXPERT: 'Expert' };

const NOMES_CLASSE: Record<ClasseOrdem, string> = {
  COMBATENTE: 'Combatente',
  ESPECIALISTA: 'Especialista',
  OCULTISTA: 'Ocultista',
};

/** Bônus do grau; sem grau a perícia é destreinada (+0). */
export function bonusDoGrau(grau: GrauOrdem | undefined): number {
  return grau === undefined ? 0 : BONUS[grau];
}

/** Defesa de Ordem: 10 + Agilidade. */
export function defesa(agilidade: number): number {
  return 10 + agilidade;
}

export function rotuloClasse(classe: ClasseOrdem | null): string {
  return classe === null ? 'A definir' : NOMES_CLASSE[classe];
}
