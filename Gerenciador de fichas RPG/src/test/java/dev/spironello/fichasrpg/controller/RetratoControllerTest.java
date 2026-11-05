package dev.spironello.fichasrpg.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.spironello.fichasrpg.FichasDeTeste;
import dev.spironello.fichasrpg.ImagensDeTeste;
import dev.spironello.fichasrpg.IntegrationTestBase;
import dev.spironello.fichasrpg.RelogioDeTesteConfig.RelogioAjustavel;
import dev.spironello.fichasrpg.repository.FichaRepository;
import dev.spironello.fichasrpg.repository.RetratoFichaRepository;

/** Rotas do retrato (spec do redesign §4.2 e §4.5). */
@DisplayName("API /fichas/{id}/retrato")
class RetratoControllerTest extends IntegrationTestBase {

	private static final Long ID_INEXISTENTE = 999_999L;
	private static final Instant INSTANTE = Instant.parse("2026-03-01T12:00:00.250Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FichaRepository fichas;

	@Autowired
	private RetratoFichaRepository retratos;

	@Autowired
	private RelogioAjustavel relogio;

	private Long fichaId;

	@BeforeEach
	void prepararFicha() {
		fichas.deleteAll();
		relogio.definir(INSTANTE);
		fichaId = fichas.save(FichasDeTeste.ordem("Lia")).getId();
	}

	@Test
	@DisplayName("PUT de um JPEG grava (204) e o GET devolve os mesmos bytes com os cabeçalhos de cache e nosniff")
	void enviaEBusca() throws Exception {
		byte[] jpeg = ImagensDeTeste.jpeg(400, 500);

		mockMvc.perform(put("/fichas/{id}/retrato", fichaId).contentType(MediaType.IMAGE_JPEG).content(jpeg))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/fichas/{id}/retrato", fichaId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(content().bytes(jpeg))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Cache-Control", containsString("private")))
				.andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
				.andExpect(header().string("Cache-Control", containsString("immutable")));
		assertThat(retratos.findById(fichaId).orElseThrow().getAtualizadoEm()).isEqualTo(INSTANTE);
	}

	@Test
	@DisplayName("um segundo envio no mesmo instante ganha a versão anterior + 1 ms (a versão nunca se repete)")
	void segundoEnvioNoMesmoInstante() throws Exception {
		enviar(ImagensDeTeste.jpeg(10, 10));
		enviar(ImagensDeTeste.jpeg(20, 20));

		assertThat(retratos.findById(fichaId).orElseThrow().getAtualizadoEm()).isEqualTo(INSTANTE.plusMillis(1));
	}

	@Test
	@DisplayName("remover e enviar de novo mais tarde gera outra versão")
	void removerEEnviarDeNovo() throws Exception {
		enviar(ImagensDeTeste.jpeg(10, 10));
		mockMvc.perform(delete("/fichas/{id}/retrato", fichaId)).andExpect(status().isNoContent());
		relogio.avancar(Duration.ofSeconds(1));
		enviar(ImagensDeTeste.jpeg(10, 10));

		assertThat(retratos.findById(fichaId).orElseThrow().getAtualizadoEm()).isEqualTo(INSTANTE.plusSeconds(1));
	}

	@Test
	@DisplayName("PNG dá 415 em ProblemDetail, dizendo o tipo aceito")
	void pngDa415() throws Exception {
		mockMvc.perform(put("/fichas/{id}/retrato", fichaId).contentType(MediaType.IMAGE_PNG)
				.content(ImagensDeTeste.png(10, 10)))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.title").value("Tipo de conteúdo não aceito"))
				.andExpect(jsonPath("$.detail", containsString("image/jpeg")));
	}

	@Test
	@DisplayName("acima de 200 KB dá 413, sem gravar nada")
	void grandeDemaisDa413() throws Exception {
		byte[] grande = new byte[200 * 1024 + 1];
		grande[0] = (byte) 0xFF;
		grande[1] = (byte) 0xD8;
		grande[2] = (byte) 0xFF;

		mockMvc.perform(put("/fichas/{id}/retrato", fichaId).contentType(MediaType.IMAGE_JPEG).content(grande))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.title").value("Imagem grande demais"))
				.andExpect(jsonPath("$.detail").value("A imagem pode ter no máximo 200 KB."));
		assertThat(retratos.existsById(fichaId)).isFalse();
	}

	@Test
	@DisplayName("bytes que não são JPEG, enviados como image/jpeg, dão 422 com o motivo")
	void lixoDa422() throws Exception {
		mockMvc.perform(put("/fichas/{id}/retrato", fichaId).contentType(MediaType.IMAGE_JPEG)
				.content(new byte[] { 1, 2, 3, 4 }))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.title").value("Imagem inválida"))
				.andExpect(jsonPath("$.detail").value("O arquivo não é um JPEG."));
	}

	@Test
	@DisplayName("JPEG minúsculo com cabeçalho de 50000×50000 dá 422, sem derrubar a API por falta de memória")
	void bombaDeDecodificacaoDa422() throws Exception {
		mockMvc.perform(put("/fichas/{id}/retrato", fichaId).contentType(MediaType.IMAGE_JPEG)
				.content(ImagensDeTeste.jpegComDimensoesFalsas(50_000, 50_000)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("A imagem passa de 1000 px de largura ou altura."));
		assertThat(retratos.existsById(fichaId)).isFalse();
	}

	@Test
	@DisplayName("ficha inexistente dá 404 nas três rotas")
	void fichaInexistente() throws Exception {
		mockMvc.perform(put("/fichas/{id}/retrato", ID_INEXISTENTE).contentType(MediaType.IMAGE_JPEG)
				.content(ImagensDeTeste.jpeg(10, 10)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
		mockMvc.perform(get("/fichas/{id}/retrato", ID_INEXISTENTE))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
		mockMvc.perform(delete("/fichas/{id}/retrato", ID_INEXISTENTE))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Ficha não encontrada"));
	}

	@Test
	@DisplayName("GET de ficha sem retrato dá 404 Retrato não encontrado")
	void semRetratoDa404() throws Exception {
		mockMvc.perform(get("/fichas/{id}/retrato", fichaId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Retrato não encontrado"));
	}

	@Test
	@DisplayName("DELETE remove (204) e o GET seguinte dá 404")
	void removeRetrato() throws Exception {
		enviar(ImagensDeTeste.jpeg(10, 10));

		mockMvc.perform(delete("/fichas/{id}/retrato", fichaId)).andExpect(status().isNoContent());

		mockMvc.perform(get("/fichas/{id}/retrato", fichaId)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("DELETE de ficha sem retrato também dá 204")
	void removerSemRetrato() throws Exception {
		mockMvc.perform(delete("/fichas/{id}/retrato", fichaId)).andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("a ficha informa retratoVersao (instante em ms) na busca e na listagem, e null sem retrato")
	void retratoVersaoNoJson() throws Exception {
		Long semRetrato = fichas.save(FichasDeTeste.ordem("Hale")).getId();

		enviar(ImagensDeTeste.jpeg(10, 10));

		mockMvc.perform(get("/fichas/{id}", fichaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.retratoVersao").value(INSTANTE.toEpochMilli()));
		mockMvc.perform(get("/fichas/{id}", semRetrato))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.retratoVersao").isEmpty());
		mockMvc.perform(get("/fichas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + fichaId + ")].retratoVersao").value(INSTANTE.toEpochMilli()))
				.andExpect(jsonPath("$[?(@.id == " + semRetrato + ")].retratoVersao")
						.value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
	}

	@Test
	@DisplayName("depois de remover o retrato, a ficha volta a retratoVersao null")
	void retratoVersaoVoltaANull() throws Exception {
		enviar(ImagensDeTeste.jpeg(10, 10));
		mockMvc.perform(delete("/fichas/{id}/retrato", fichaId)).andExpect(status().isNoContent());

		mockMvc.perform(get("/fichas/{id}", fichaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.retratoVersao").isEmpty());
	}

	private void enviar(byte[] jpeg) throws Exception {
		mockMvc.perform(put("/fichas/{id}/retrato", fichaId).contentType(MediaType.IMAGE_JPEG).content(jpeg))
				.andExpect(status().isNoContent());
	}
}
