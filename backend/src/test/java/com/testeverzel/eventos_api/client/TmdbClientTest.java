package com.testeverzel.eventos_api.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.testeverzel.eventos_api.dto.MovieSummary;
import com.testeverzel.eventos_api.exception.TmdbNotConfiguredException;

/**
 * Verifica o contrato com o TMDb sem depender de rede nem de chave: a resposta abaixo é o formato
 * real de /search/movie, recortado. Serve para pegar erro de mapeamento de campo antes que ele
 * apareça na primeira chamada de verdade.
 */
class TmdbClientTest {

    private static final String SEARCH_RESPONSE = """
            {
              "page": 1,
              "results": [
                {
                  "adult": false,
                  "backdrop_path": "/xJHokMbljvjADYdit5fK5VQsXEG.jpg",
                  "genre_ids": [12, 18, 878],
                  "id": 157336,
                  "original_language": "en",
                  "original_title": "Interstellar",
                  "overview": "Um grupo de exploradores atravessa um buraco de minhoca.",
                  "popularity": 142.5,
                  "poster_path": "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                  "release_date": "2014-11-05",
                  "title": "Interstellar",
                  "video": false,
                  "vote_average": 8.4,
                  "vote_count": 32000
                }
              ],
              "total_pages": 1,
              "total_results": 1
            }
            """;

    private static final String BASE_URL = "https://api.themoviedb.org/3";

    @Test
    void mapeiaCamposDaRespostaDoTmdb() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(Matchers.containsString("/search/movie")))
                .andRespond(withSuccess(SEARCH_RESPONSE, MediaType.APPLICATION_JSON));

        TmdbClient client = new TmdbClient(builder, BASE_URL, "chave-de-teste");
        List<MovieSummary> movies = client.searchMovies("interstellar");

        assertThat(movies).hasSize(1);
        MovieSummary movie = movies.get(0);
        assertThat(movie.id()).isEqualTo(157336L);
        assertThat(movie.titulo()).isEqualTo("Interstellar");
        assertThat(movie.sinopse()).startsWith("Um grupo de exploradores");
        assertThat(movie.dataLancamento()).isEqualTo("2014-11-05");
        assertThat(movie.posterPath()).isEqualTo("/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg");
        server.verify();
    }

    @Test
    void enviaQueryEChaveNaRequisicao() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(Matchers.allOf(
                Matchers.containsString("query=duna"),
                Matchers.containsString("api_key=chave-de-teste"))))
                .andRespond(withSuccess(SEARCH_RESPONSE, MediaType.APPLICATION_JSON));

        new TmdbClient(builder, BASE_URL, "chave-de-teste").searchMovies("duna");

        server.verify();
    }

    @Test
    void falhaExplicitaQuandoAChaveNaoEstaConfigurada() {
        TmdbClient client = new TmdbClient(RestClient.builder(), BASE_URL, "");

        assertThatThrownBy(() -> client.searchMovies("duna"))
                .isInstanceOf(TmdbNotConfiguredException.class);
    }

    @Test
    void propagaFalhaDoUpstreamComoRestClientException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(Matchers.containsString("/search/movie")))
                .andRespond(withServerError());

        TmdbClient client = new TmdbClient(builder, BASE_URL, "chave-de-teste");

        assertThatThrownBy(() -> client.searchMovies("duna"))
                .isInstanceOf(RestClientException.class);
    }
}
