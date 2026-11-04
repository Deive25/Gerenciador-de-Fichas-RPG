// Contrato da API /fichas (ADR 0012). Os codigos sao os do enum Pericia do backend.
export type CodigoPericia =
	'ACROBACIA' | 'ATLETISMO' | 'ADESTRAR_ANIMAIS' | 'ATUACAO_LABIA' | 'CHARME' | 'ESQUIVA' |
	'FURTIVIDADE' | 'LUTA_CORPO_A_CORPO' | 'LUTA_ARMA_BRANCA' | 'INTIMIDACAO' | 'INTUICAO' |
	'INVESTIGACAO' | 'ARMAS_DE_FOGO_LEVES' | 'ARMAS_DE_FOGO_MEDIAS' | 'ARMAS_DE_FOGO_PESADAS' |
	'PERCEPCAO' | 'USAR_BIBLIOTECAS' | 'HISTORIA' | 'CURA' | 'MALANDRAGEM' | 'OCULTISMO' |
	'PSICOLOGIA' | 'ARREMESSAR'

export interface Sheet{
    id:number;
    jogador:String
    personagem:String
	genero:String
	idade: number
	vidaAtual:number
    vidaMaxima:number
	sanidadeAtual:number
    sanidadeMaxima:number
	/*ATRIBUTOS*/
	forca:number
    destreza:number
    constituicao:number
    inteligencia:number
    carisma:number
    sabedoria:number
    tamanho:number
    sorte:number
    poder:number
	/*PERICIAS: a API devolve so as preenchidas; o formulario envia as 23 (null nas vazias)*/
	pericias: { [codigo in CodigoPericia]?: number | null }
}
