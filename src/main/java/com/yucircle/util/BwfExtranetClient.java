package com.yucircle.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * extranet-lv BWF 公开 JSON API 客户端（唯一网络出口）。
 * fetch 可注入（单测喂 fixture JSON）；坑位记录见 doc/06 §3：
 * publicationId 只认数字尾、h2h 只认数字 tmt_id。
 */
@Component
public class BwfExtranetClient {

    public static final String BASE = "https://extranet-lv.bwfbadminton.com/api";

    public interface JsonFetcher {
        String get(String url, Map<String, String> params) throws IOException, InterruptedException;
    }

    private final JsonFetcher fetcher;
    private final ObjectMapper mapper = new ObjectMapper();

    public BwfExtranetClient() {
        this(defaultFetcher());
    }

    public BwfExtranetClient(JsonFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /** 排名期次（周 key 形如 2026-34-4435） */
    public JsonNode fetchRankingWeek(int rankId) throws Exception {
        return get("/vue-rankingweek", Map.of("rankId", String.valueOf(rankId)));
    }

    /** 排名表（rankId=2 世界排名；catId 6..10 = 男单..混双） */
    public JsonNode fetchRankingTable(int rankId, int catId, String publicationId) throws Exception {
        return get("/vue-rankingtable", Map.of(
                "rankId", String.valueOf(rankId),
                "catId", String.valueOf(catId),
                "publicationId", publicationId,
                "pageKey", "50",
                "page", "1"));
    }

    /** 全年赛程（grouped by month，解析见 BwfParser） */
    public JsonNode fetchYearTournaments(int year) throws Exception {
        return get("/vue-grouped-year-tournaments", Map.of("year", String.valueOf(year)));
    }

    /** 当日对阵（返回顺序即官方对阵顺序，勿重排） */
    public JsonNode fetchDayMatches(String tournamentCode, String date) throws Exception {
        return get("/tournaments/day-matches", Map.of(
                "tournamentCode", tournamentCode,
                "date", date,
                "order", "1",
                "court", "0"));
    }

    /** 单场逐分（tmt_id 必须数字 id，UUID 会 500） */
    public JsonNode fetchH2hMatch(int tmtId, String matchCode) throws Exception {
        return get("/h2h/match", Map.of(
                "tmt_id", String.valueOf(tmtId),
                "match_code", matchCode));
    }

    private JsonNode get(String path, Map<String, String> params) throws Exception {
        String body = fetcher.get(BASE + path, params);
        return mapper.readTree(body);
    }

    private static JsonFetcher defaultFetcher() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        return (url, params) -> {
            String full = url + (params.isEmpty() ? "" : "?" + encode(params));
            HttpRequest req = HttpRequest.newBuilder(URI.create(full))
                    .header("Accept", "application/json")
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126 Safari/537.36")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IOException("extranet " + resp.statusCode() + ": " + full);
            }
            return resp.body();
        };
    }

    private static String encode(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
