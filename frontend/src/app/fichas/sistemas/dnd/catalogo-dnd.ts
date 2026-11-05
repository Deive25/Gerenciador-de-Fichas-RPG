import { AtributoDnd, EdicaoDnd, GrauDnd, PericiaDnd } from '../../ficha.model';

export interface AtributoDndCatalogo {
  codigo: AtributoDnd;
  nome: string;
  descricao: string;
}

export interface PericiaDndCatalogo {
  codigo: PericiaDnd;
  nome: string;
  atributo: AtributoDnd;
  descricao: string;
}

// Descrições curtas, escritas com palavras próprias (nada copiado dos livros).
export const ATRIBUTOS_DND: readonly AtributoDndCatalogo[] = [
  { codigo: 'FOR', nome: 'Força', descricao: 'Potência física: golpes corpo a corpo, empurrar, erguer e carregar peso.' },
  { codigo: 'DES', nome: 'Destreza', descricao: 'Agilidade, reflexos e equilíbrio. Também ajuda na CA e em ataques à distância.' },
  { codigo: 'CON', nome: 'Constituição', descricao: 'Saúde e fôlego: quanto o corpo aguenta de dano, cansaço e veneno.' },
  { codigo: 'INT', nome: 'Inteligência', descricao: 'Raciocínio, memória e estudo; base de quem aprende magia por livros.' },
  { codigo: 'SAB', nome: 'Sabedoria', descricao: 'Percepção do ambiente, intuição e bom senso; base de várias magias divinas.' },
  { codigo: 'CAR', nome: 'Carisma', descricao: 'Presença, persuasão e força de vontade; base de quem conjura pela personalidade.' },
];

// Em ordem alfabética, como a ficha mostra. O atributo é o que a perícia usa no teste.
export const PERICIAS_DND: readonly PericiaDndCatalogo[] = [
  { codigo: 'ACROBACIA', nome: 'Acrobacia', atributo: 'DES', descricao: 'Equilíbrio, piruetas e manter-se de pé em situações instáveis.' },
  { codigo: 'ARCANISMO', nome: 'Arcanismo', atributo: 'INT', descricao: 'Conhecimento sobre magia, criaturas místicas e planos de existência.' },
  { codigo: 'ATLETISMO', nome: 'Atletismo', atributo: 'FOR', descricao: 'Escalar, nadar, saltar e outros esforços físicos intensos.' },
  { codigo: 'ATUACAO', nome: 'Atuação', atributo: 'CAR', descricao: 'Entreter a plateia com música, dança, teatro ou contação de histórias.' },
  { codigo: 'ENGANACAO', nome: 'Enganação', atributo: 'CAR', descricao: 'Mentir, disfarçar a verdade e fazer os outros acreditarem no que não é.' },
  { codigo: 'FURTIVIDADE', nome: 'Furtividade', atributo: 'DES', descricao: 'Mover-se em silêncio e esconder-se sem ser notado.' },
  { codigo: 'HISTORIA', nome: 'História', atributo: 'INT', descricao: 'Saber sobre eventos passados, reinos, guerras e lendas.' },
  { codigo: 'INTIMIDACAO', nome: 'Intimidação', atributo: 'CAR', descricao: 'Impor medo com ameaças ou presença para conseguir o que quer.' },
  { codigo: 'INTUICAO', nome: 'Intuição', atributo: 'SAB', descricao: 'Ler as intenções de alguém e perceber se ele mente.' },
  { codigo: 'INVESTIGACAO', nome: 'Investigação', atributo: 'INT', descricao: 'Procurar pistas e deduzir o que elas significam.' },
  { codigo: 'LIDAR_COM_ANIMAIS', nome: 'Lidar com Animais', atributo: 'SAB', descricao: 'Acalmar, guiar e entender o comportamento de bichos.' },
  { codigo: 'MEDICINA', nome: 'Medicina', atributo: 'SAB', descricao: 'Estabilizar feridos e reconhecer doenças e ferimentos.' },
  { codigo: 'NATUREZA', nome: 'Natureza', atributo: 'INT', descricao: 'Conhecer plantas, animais, terrenos e o clima.' },
  { codigo: 'PERCEPCAO', nome: 'Percepção', atributo: 'SAB', descricao: 'Notar o que está ao redor: sons, vultos, detalhes escondidos.' },
  { codigo: 'PERSUASAO', nome: 'Persuasão', atributo: 'CAR', descricao: 'Convencer com tato e boa conversa, de forma honesta.' },
  { codigo: 'PRESTIDIGITACAO', nome: 'Prestidigitação', atributo: 'DES', descricao: 'Truques de mão: bater carteira, esconder objetos e fazer truques.' },
  { codigo: 'RELIGIAO', nome: 'Religião', atributo: 'INT', descricao: 'Saber sobre deuses, ritos, símbolos sagrados e ordens religiosas.' },
  { codigo: 'SOBREVIVENCIA', nome: 'Sobrevivência', atributo: 'SAB', descricao: 'Rastrear, caçar, orientar-se e sobreviver em ambiente selvagem.' },
];

export const NOMES_EDICAO: Record<EdicaoDnd, string> = {
  D2014: 'D&D 5e (2014)',
  D2024: 'D&D 2024',
};

/** O mesmo campo `especie` se chama "Raça" na edição de 2014 e "Espécie" na de 2024. */
export function rotuloEspecie(edicao: EdicaoDnd): 'Raça' | 'Espécie' {
  return edicao === 'D2014' ? 'Raça' : 'Espécie';
}

export const SELO_GRAU_DND: Record<GrauDnd, string> = { PROFICIENTE: 'P', ESPECIALISTA: 'E' };

export const NOME_GRAU_DND: Record<GrauDnd, string> = { PROFICIENTE: 'Proficiente', ESPECIALISTA: 'Especialista' };
