package com.yucircle.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.yucircle.entity.BwfMatch;
import com.yucircle.entity.BwfMatchGame;
import com.yucircle.entity.BwfMatchPoint;
import com.yucircle.entity.BwfRankingEntry;
import com.yucircle.entity.BwfTournament;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * extranet-lv JSON → 实体的纯解析（无网络/无 Spring，直接单测）。
 * 坑位：grade1 匹配用子串且排除 junior；dayMatches 顺序即官方对阵顺序；
 * h2h 逐分值为累计得分。设计见 doc/06。
 */
public final class BwfParser {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 国家全名 → BWF 三字码（未收录回退全名） */
    private static final Map<String, String> COUNTRY_CODES = Map.ofEntries(
            Map.entry("China", "CHN"), Map.entry("Chinese Taipei", "TPE"),
            Map.entry("Korea", "KOR"), Map.entry("Japan", "JPN"),
            Map.entry("Indonesia", "INA"), Map.entry("India", "IND"),
            Map.entry("Thailand", "THA"), Map.entry("Malaysia", "MAS"),
            Map.entry("Denmark", "DEN"), Map.entry("France", "FRA"),
            Map.entry("Hong Kong China", "HKG"), Map.entry("Canada", "CAN"),
            Map.entry("USA", "USA"), Map.entry("Singapore", "SIN"),
            Map.entry("Scotland", "SCO"), Map.entry("Turkiye", "TUR"),
            Map.entry("Turkey", "TUR"), Map.entry("Bulgaria", "BUL"),
            Map.entry("Ukraine", "UKR"), Map.entry("Germany", "GER"),
            Map.entry("England", "ENG"), Map.entry("Spain", "ESP"),
            Map.entry("Ireland", "IRL"), Map.entry("Belgium", "BEL"),
            Map.entry("Vietnam", "VIE"), Map.entry("Czechia", "CZE"),
            Map.entry("Brazil", "BRA"), Map.entry("Sweden", "SWE"),
            Map.entry("Switzerland", "SUI"), Map.entry("Austria", "AUT"),
            Map.entry("Poland", "POL"), Map.entry("Italy", "ITA"),
            Map.entry("Netherlands", "NED"), Map.entry("Australia", "AUS"),
            Map.entry("New Zealand", "NZL"), Map.entry("Macau China", "MAC"),
            Map.entry("Philippines", "PHI"), Map.entry("Hungary", "HUN"),
            Map.entry("Finland", "FIN"), Map.entry("Norway", "NOR"),
            Map.entry("Portugal", "POR"), Map.entry("Wales", "WAL"),
            Map.entry("Israel", "ISR"), Map.entry("Egypt", "EGY"),
            Map.entry("South Africa", "RSA"), Map.entry("Algeria", "ALG"),
            Map.entry("Peru", "PER"), Map.entry("Mexico", "MEX"),
            Map.entry("Cuba", "CUB"), Map.entry("Chile", "CHI"),
            Map.entry("Argentina", "ARG"), Map.entry("Sri Lanka", "SRI"),
            Map.entry("Nepal", "NEP"), Map.entry("Pakistan", "PAK"),
            Map.entry("Azerbaijan", "AZE"), Map.entry("Georgia", "GEO"),
            Map.entry("Kazakhstan", "KAZ"), Map.entry("Uzbekistan", "UZB"),
            Map.entry("Iran", "IRI"), Map.entry("Luxembourg", "LUX"));

    private BwfParser() {
    }

    /** 周 key "2026-34-4435" → 数字尾 "4435"（API 只认数字尾） */
    public static String publicationId(JsonNode weekArray) {
        String key = weekArray.get(0).path("key").asText();
        return key.substring(key.lastIndexOf('-') + 1);
    }

    /** 期次发布日期（排名快照的 publication_date） */
    public static LocalDate publicationDate(JsonNode weekArray) {
        String date = weekArray.get(0).path("date").asText(); // 2026-08-18 00:00:00
        return LocalDate.parse(date.substring(0, 10));
    }

    /** 排名表 → 快照明细（pageKey=50 时 results.data 为 Top50） */
    public static List<BwfRankingEntry> rankings(JsonNode table, String discipline, LocalDate pubDate) {
        List<BwfRankingEntry> list = new ArrayList<>();
        for (JsonNode row : table.path("results").path("data")) {
            String player = displayName(row.path("player1_model"));
            JsonNode p2 = row.path("player2_model");
            if (p2.isObject() && !p2.isEmpty()) {
                player += " / " + displayName(p2);
            }
            String countryName = row.path("p1_country_model").path("name").asText("");
            BwfRankingEntry e = new BwfRankingEntry();
            e.setDiscipline(discipline);
            e.setPublicationDate(pubDate);
            e.setRankNum(row.path("rank").asInt());
            e.setRankChange(row.path("rank_change").asInt());
            e.setCountry(COUNTRY_CODES.getOrDefault(countryName, countryName));
            e.setPlayerName(player);
            e.setPoints((int) row.path("points").asDouble());
            list.add(e);
        }
        return list;
    }

    /** name_display_bold 去标签：&lt;span class="name-2"&gt;SHI&lt;/span&gt; … → SHI Yu Qi */
    private static String displayName(JsonNode playerModel) {
        return playerModel.path("name_display_bold").asText("").replaceAll("<[^>]+>", "").trim();
    }

    /** 全年赛程（grouped by month 拍平 + level 映射，升序） */
    public static List<BwfTournament> tournaments(JsonNode groupedYear, int year) {
        List<BwfTournament> list = new ArrayList<>();
        for (JsonNode month : groupedYear.path("results")) {
            for (JsonNode t : month.path("tournaments")) {
                String level = levelOf(t.path("category").asText());
                if ("other".equals(level)) {
                    continue; // 只收 World Tour + Grade 1 大赛（同工具箱口径）
                }
                BwfTournament e = new BwfTournament();
                e.setTmtId(t.path("id").asInt());
                e.setName(t.path("name").asText());
                e.setLevel(level);
                e.setStartDate(LocalDate.parse(t.path("start_date").asText().substring(0, 10)));
                e.setEndDate(LocalDate.parse(t.path("end_date").asText().substring(0, 10)));
                e.setCity(t.path("location").asText(""));
                e.setCountry(t.path("country").asText(""));
                e.setPrizeMoney(t.path("prize_money").isNumber()
                        ? t.path("prize_money").asInt() : 0);
                e.setCode(t.path("code").asText(null));
                e.setHasLiveScores(t.path("has_live_scores").asBoolean(false));
                list.add(e);
            }
        }
        list.sort(Comparator.comparing(BwfTournament::getStartDate));
        return list;
    }

    /** category → level；Grade 1 非 Junior 为 major（世锦赛/汤尤杯） */
    static String levelOf(String category) {
        String low = category.toLowerCase();
        if (low.contains("grade 1") && !low.contains("junior")) {
            return "major";
        }
        String[][] levels = {
                {"World Tour Finals", "finals"},
                {"Super 1000", "super1000"},
                {"Super 750", "super750"},
                {"Super 500", "super500"},
                {"Super 300", "super300"},
        };
        for (String[] lv : levels) {
            if (low.contains(lv[0].toLowerCase())) {
                return lv[1];
            }
        }
        return "other";
    }

    /** 当日对阵 → 赛果行（顺序保持官方对阵顺序；scoreText 同步生成） */
    public static List<BwfMatch> dayMatches(JsonNode array, int tmtId, LocalDate matchDate) {
        List<BwfMatch> list = new ArrayList<>();
        int orderNo = 0;
        for (JsonNode m : array) {
            BwfMatch e = new BwfMatch();
            e.setTmtId(tmtId);
            e.setMatchCode(m.path("code").asText());
            e.setOrderNo(++orderNo);
            e.setMatchDate(matchDate);
            e.setEvent(m.path("eventName").asText());
            e.setRoundName(m.path("roundName").asText(""));
            e.setCourtName(m.path("courtName").asText(""));
            String time = m.path("matchTime").asText("");
            if (time.length() >= 19) {
                e.setMatchTime(LocalDateTime.parse(time, DT));
            }
            String st = m.path("matchStatusValue").asText();
            e.setStatus(switch (st) {
                case "Finished" -> "F";
                case "In Progress" -> "P";
                default -> "N";
            });
            e.setWinner(m.path("winner").asInt());
            e.setDurationMin(m.path("duration").isNumber() ? m.path("duration").asInt() : null);
            e.setTeam1Country(m.path("team1").path("countryCode").asText(""));
            e.setTeam1Players(playersText(m.path("team1")));
            e.setTeam1Seed(textOrNull(m.path("team1seed")));
            e.setTeam2Country(m.path("team2").path("countryCode").asText(""));
            e.setTeam2Players(playersText(m.path("team2")));
            e.setTeam2Seed(textOrNull(m.path("team2seed")));
            e.setScoreText(scoreText(m.path("score")));
            list.add(e);
        }
        return list;
    }

    /** h2h games[] → 局（gameNo 按 ordering） */
    public static List<BwfMatchGame> games(JsonNode h2h, Long matchId) {
        List<JsonNode> raw = new ArrayList<>();
        h2h.path("games").forEach(raw::add);
        raw.sort(Comparator.comparingInt(g -> g.path("ordering").asInt()));
        List<BwfMatchGame> list = new ArrayList<>();
        int no = 1;
        for (JsonNode g : raw) {
            BwfMatchGame e = new BwfMatchGame();
            e.setMatchId(matchId);
            e.setGameNo(no++);
            e.setTeam1Score(g.path("team1").asInt());
            e.setTeam2Score(g.path("team2").asInt());
            list.add(e);
        }
        return list;
    }

    /** 单局 match_set_details_model → 逐分（按 ordering 升序；值为累计得分） */
    public static List<BwfMatchPoint> points(JsonNode game, Long gameId) {
        List<JsonNode> raw = new ArrayList<>();
        game.path("match_set_details_model").forEach(raw::add);
        raw.sort(Comparator.comparingInt(p -> p.path("ordering").asInt()));
        List<BwfMatchPoint> list = new ArrayList<>();
        for (JsonNode p : raw) {
            BwfMatchPoint e = new BwfMatchPoint();
            e.setGameId(gameId);
            e.setOrdering(p.path("ordering").asInt());
            e.setTeam1(p.path("team1").asInt());
            e.setTeam2(p.path("team2").asInt());
            list.add(e);
        }
        return list;
    }

    /** 从 h2h games 数组中取指定局（与 games() 同序） */
    public static JsonNode nthGame(JsonNode h2h, int gameNo) {
        List<JsonNode> raw = new ArrayList<>();
        h2h.path("games").forEach(raw::add);
        raw.sort(Comparator.comparingInt(g -> g.path("ordering").asInt()));
        return gameNo - 1 < raw.size() ? raw.get(gameNo - 1) : null;
    }

    private static String playersText(JsonNode team) {
        return StreamSupport.stream(team.path("players").spliterator(), false)
                .map(p -> p.path("nameShort").asText("").trim())
                .filter(s -> !s.isEmpty())
                .reduce((a, b) -> a + " / " + b)
                .orElse("");
    }

    private static String scoreText(JsonNode score) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode s : score) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s.path("home").asInt()).append('-').append(s.path("away").asInt());
        }
        return sb.toString();
    }

    private static String textOrNull(JsonNode n) {
        return n.isTextual() && !n.asText().isBlank() ? n.asText() : null;
    }
}
