import { ChaveAtributo, CodigoPericia, ResultadoTeste } from './ficha.model';

export interface ItemCatalogo<K extends string> {
  chave: K;
  nome: string;
  descricao: string;
}

// Textos de ajuda vindos da versão antiga do frontend (dash-board, até o commit b4a6cb7),
// com acentuação e erros de digitação corrigidos. Os nomes de exibição seguem o enum da API.
export const ATRIBUTOS: readonly ItemCatalogo<ChaveAtributo>[] = [
  {
    chave: 'forca',
    nome: 'Força',
    descricao:
      'A Força mede o poder muscular de um investigador. Quanto maior for, mais o investigador conseguirá levantar ou se agarrar a algo. Este atributo determina o dano que o investigador causa no combate corpo a corpo. Reduzido a FOR 0, um investigador estará inválido, incapaz de sair da cama.',
  },
  {
    chave: 'destreza',
    nome: 'Destreza',
    descricao:
      'Investigadores com valores altos de Destreza são mais rápidos, mais ágeis e mais fisicamente flexíveis. Um teste de DES pode ser feito para se agarrar em algum lugar e evitar uma queda, para se mover mais rápido do que um adversário ou para realizar alguma tarefa delicada. Um investigador com zero de DES é descoordenado e incapaz de executar tarefas físicas.',
  },
  {
    chave: 'constituicao',
    nome: 'Constituição',
    descricao:
      'A Constituição representa saúde, vigor e vitalidade. Venenos e doenças podem desafiar diretamente a constituição de um investigador. Investigadores com uma constituição alta muitas vezes têm mais pontos de vida, o que é melhor para resistir a ferimentos e ataques. Lesões físicas graves ou ataques mágicos podem diminuir esse atributo e, se a constituição chegar a zero, o investigador morre.',
  },
  {
    chave: 'inteligencia',
    nome: 'Inteligência',
    descricao:
      'A Inteligência representa o quão bem os investigadores aprendem, lembram, analisam informações e resolvem enigmas complexos. Um investigador com INT zero é um idiota balbuciante e babão.',
  },
  {
    chave: 'carisma',
    nome: 'Carisma',
    descricao:
      'O Carisma mede a força de personalidade e a capacidade de persuasão, além de uma mistura de simpatia e beleza física. Ele pesa em testes de Adestramento, Atuação, Diplomacia, Enganação, Intimidação e Jogatina.',
  },
  {
    chave: 'sabedoria',
    nome: 'Sabedoria',
    descricao:
      'A Sabedoria representa a percepção e a força de vontade, além de bom senso e intuição. Ela pesa em testes de Cura, Intuição, Percepção, Religião, Sobrevivência e Vontade.',
  },
  {
    chave: 'tamanho',
    nome: 'Tamanho',
    descricao:
      'O Tamanho representa altura e peso, ambos em um único número. Para enxergar por sobre um muro, espremer-se através de uma pequena abertura ou até mesmo para saber a cabeça de quem pode estar despontando na grama, use o Tamanho.',
  },
  {
    chave: 'sorte',
    nome: 'Sorte',
    // A versão antiga tinha aqui, por engano, um pedaço do texto do Poder; este texto é novo.
    descricao:
      'A Sorte mede o quanto o acaso favorece o investigador quando nenhuma perícia se aplica: quanto maior, mais provável que o imprevisto jogue a seu favor.',
  },
  {
    chave: 'poder',
    nome: 'Poder',
    descricao:
      'O Poder indica a força de vontade: quanto maior o POD, maior a aptidão mágica e a resistência à magia. Um investigador com POD zero é apático e sem propósito, além de ser incapaz de usar magia. Salvo quando indicado o contrário, o POD perdido durante o jogo não pode ser recuperado.',
  },
];

export const PERICIAS: readonly ItemCatalogo<CodigoPericia>[] = [
  { chave: 'ACROBACIA', nome: 'Acrobacia', descricao: 'Você consegue fazer proezas acrobáticas.' },
  {
    chave: 'ATLETISMO',
    nome: 'Atletismo',
    descricao: 'Usada para façanhas atléticas, como escalar montanhas, cruzar rios e saltar sobre desfiladeiros.',
  },
  { chave: 'ADESTRAR_ANIMAIS', nome: 'Adestrar animais', descricao: 'Você sabe lidar com animais.' },
  {
    chave: 'ATUACAO_LABIA',
    nome: 'Atuação/Lábia',
    descricao: 'Você sabe fazer apresentações artísticas, incluindo música, dança e dramaturgia.',
  },
  {
    chave: 'CHARME',
    nome: 'Charme',
    descricao:
      'O Charme assume muitas formas, incluindo atração física, sedução, lisonja ou simplesmente uma personalidade calorosa. Pode ser usado para fazer alguém agir de determinada maneira, mas não de uma forma completamente contrária ao comportamento normal dessa pessoa. É resistido pelas perícias Charme ou Psicologia.',
  },
  {
    chave: 'ESQUIVA',
    nome: 'Esquiva',
    descricao: 'Permite que um investigador evite por instinto golpes, objetos atirados contra ele e coisas do gênero.',
  },
  {
    chave: 'FURTIVIDADE',
    nome: 'Furtividade',
    descricao: 'Você pode se esconder nas sombras, andar sem fazer barulho, seguir alguém sem ser notado etc.',
  },
  {
    chave: 'LUTA_CORPO_A_CORPO',
    nome: 'Luta corpo a corpo',
    descricao: 'Mede a sua habilidade de enfrentar inimigos corpo a corpo.',
  },
  {
    chave: 'LUTA_ARMA_BRANCA',
    nome: 'Luta com arma branca',
    descricao: 'Mede a sua habilidade de luta com armas brancas, como facas, katanas e machados.',
  },
  { chave: 'INTIMIDACAO', nome: 'Intimidação', descricao: 'Você pode assustar ou coagir outras pessoas.' },
  { chave: 'INTUICAO', nome: 'Intuição', descricao: 'Esta perícia mede o seu "sexto sentido".' },
  { chave: 'INVESTIGACAO', nome: 'Investigação', descricao: 'Você sabe como descobrir pistas e informações.' },
  {
    chave: 'ARMAS_DE_FOGO_LEVES',
    nome: 'Armas de fogo leves',
    descricao: 'Permite usar armas de fogo leves, como pistolas.',
  },
  {
    chave: 'ARMAS_DE_FOGO_MEDIAS',
    nome: 'Armas de fogo médias',
    descricao: 'Permite usar armas de fogo médias, como submetralhadoras.',
  },
  {
    chave: 'ARMAS_DE_FOGO_PESADAS',
    nome: 'Armas de fogo pesadas',
    descricao: 'Permite usar armas de fogo pesadas, como rifles.',
  },
  { chave: 'PERCEPCAO', nome: 'Percepção', descricao: 'Usada para perceber coisas através dos sentidos.' },
  {
    chave: 'USAR_BIBLIOTECAS',
    nome: 'Usar bibliotecas',
    descricao:
      'Permite encontrar uma informação, como um livro, jornal ou referência específica, numa biblioteca, coleção de documentos ou banco de dados, desde que o que se procura esteja lá.',
  },
  {
    chave: 'HISTORIA',
    nome: 'História',
    descricao: 'Permite recordar a importância de um país, cidade, região ou pessoa.',
  },
  {
    chave: 'CURA',
    nome: 'Cura',
    descricao:
      'Permite diagnosticar e tratar ferimentos, doenças e envenenamentos, e fazer recomendações sobre saúde pública. Se a época não tiver um bom tratamento para uma doença, o esforço é limitado, incerto ou inconclusivo.',
  },
  {
    chave: 'MALANDRAGEM',
    nome: 'Malandragem',
    descricao: 'Mede as suas habilidades de malandro: com mãos leves e mente suja, você sabe exercer as tarefas de um ladrão.',
  },
  {
    chave: 'OCULTISMO',
    nome: 'Ocultismo',
    descricao:
      'Permite reconhecer objetos, palavras e conceitos do ocultismo e das tradições populares, e identificar grimórios de magia e códices de ocultismo.',
  },
  {
    chave: 'PSICOLOGIA',
    nome: 'Psicologia',
    descricao:
      'Uma perícia de percepção comum a todos os seres humanos: permite estudar um indivíduo e formar uma ideia sobre as motivações e o caráter dele.',
  },
  {
    chave: 'ARREMESSAR',
    nome: 'Arremessar',
    descricao:
      'Usada para acertar um alvo com um objeto. Um objeto que caiba na mão e tenha peso razoável (ou uma arma feita para ser arremessada) pode ser atirado até uma distância igual à Força dividida por 5, em metros.',
  },
];

/** Faixas do teste de perícia (mesma regra do servidor): extremo ≤ ⌊valor/5⌋, sucesso ≤ valor. */
export function faixas(valor: number): { extremo: number; sucesso: number } {
  return { extremo: Math.floor(valor / 5), sucesso: valor };
}

const ROTULOS: Record<ResultadoTeste, string> = {
  SUCESSO_EXTREMO: 'Sucesso extremo',
  SUCESSO: 'Sucesso',
  FALHA: 'Falha',
};

export function rotuloResultado(resultado: ResultadoTeste): string {
  return ROTULOS[resultado];
}
