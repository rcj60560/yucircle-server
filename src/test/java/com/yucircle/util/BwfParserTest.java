package com.yucircle.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yucircle.entity.BwfMatch;
import com.yucircle.entity.BwfMatchPoint;
import com.yucircle.entity.BwfRankingEntry;
import com.yucircle.entity.BwfTournament;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 形状取自 extranet-lv 实测响应（fixture 见 doc/06 §3 坑位） */
class BwfParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void rankingWeekKeyToNumericTailAndDate() throws Exception {
        JsonNode weeks = mapper.readTree("""
            [{"key":"2026-34-4435","date":"2026-08-18 00:00:00"},
             {"key":"2026-33-4400","date":"2026-08-11 00:00:00"}]
            """);
        assertEquals("4435", BwfParser.publicationId(weeks));
        assertEquals(LocalDate.of(2026, 8, 18), BwfParser.publicationDate(weeks));
    }

    @Test
    void rankingsParseNameCountryDoubles() throws Exception {
        JsonNode table = mapper.readTree("""
            {"results":{"data":[
              {"rank":1,"rank_change":0,"points":"94255.0000",
               "player1_model":{"name_display_bold":"<span class=\\"name-2\\">SHI</span> <span class=\\"name-1\\">Yu Qi</span>"},
               "player2_model":null,
               "p1_country_model":{"name":"China"}},
              {"rank":1,"rank_change":0,"points":"90110.0000",
               "player1_model":{"name_display_bold":"<span class=\\"name-2\\">FENG</span> <span class=\\"name-1\\">Yan Zhe</span>"},
               "player2_model":{"name_display_bold":"<span class=\\"name-2\\">HUANG</span> <span class=\\"name-1\\">Dong Ping</span>"},
               "p1_country_model":{"name":"Chinese Taipei"}}
            ]}}
            """);
        List<BwfRankingEntry> entries = BwfParser.rankings(table, "ms", LocalDate.of(2026, 8, 18));
        assertEquals(2, entries.size());
        assertEquals("SHI Yu Qi", entries.get(0).getPlayerName());
        assertEquals("CHN", entries.get(0).getCountry());
        assertEquals(94255, entries.get(0).getPoints());
        // 双打拼接 + 未映射国家回退全名
        assertEquals("FENG Yan Zhe / HUANG Dong Ping", entries.get(1).getPlayerName());
        assertEquals("TPE", entries.get(1).getCountry());
    }

    @Test
    void tournamentsLevelMapping() throws Exception {
        JsonNode grouped = mapper.readTree("""
            {"results":[{"tournaments":[
              {"name":"BWF World Championships 2026","category":"Grade 1 \\u2013 Individual Tournaments",
               "start_date":"2026-08-17 00:00:00","end_date":"2026-08-23 00:00:00",
               "location":"New Delhi, India","country":"India","prize_money":null,
               "id":5601,"code":"B671FB97","has_live_scores":true},
              {"name":"Junior X","category":"Grade 1 \\u2013 Individual Junior Tournaments",
               "start_date":"2026-10-11 00:00:00","end_date":"2026-10-17 00:00:00",
               "location":"Cairo","country":"Egypt","id":5699},
              {"name":"LIC India Open","category":"HSBC BWF World Tour Super 750",
               "start_date":"2026-01-13 00:00:00","end_date":"2026-01-18 00:00:00",
               "location":"New Delhi","country":"India","prize_money":950000,"id":4321}
            ]}]}
            """);
        List<BwfTournament> list = BwfParser.tournaments(grouped, 2026);
        // Junior 被过滤，剩 2 站且按开始日期升序
        assertEquals(2, list.size());
        assertEquals("LIC India Open", list.get(0).getName());
        assertEquals("super750", list.get(0).getLevel());
        assertEquals(950000, list.get(0).getPrizeMoney());
        BwfTournament worlds = list.get(1);
        assertEquals("major", worlds.getLevel());
        assertEquals(5601, worlds.getTmtId());
        assertEquals(Boolean.TRUE, worlds.getHasLiveScores());
        assertEquals("B671FB97", worlds.getCode());
    }

    @Test
    void dayMatchesKeepOfficialOrderAndParse() throws Exception {
        JsonNode arr = mapper.readTree("""
            [{"code":"553","duration":33,"eventName":"XD","roundName":"R64","courtName":"Court 2",
              "matchTime":"2026-08-18 10:45:00","matchStatusValue":"Finished","winner":2,
              "team1seed":null,"team2seed":"3",
              "score":[{"set":1,"home":16,"away":21},{"set":2,"home":19,"away":21}],
              "team1":{"countryCode":"MAS","players":[{"nameShort":"WONG T C "},{"nameShort":"LIM C S "}]},
              "team2":{"countryCode":"INA","players":[{"nameShort":"KUSHARJANTO"}]}},
             {"code":"49","eventName":"MS","roundName":"R64","courtName":"Court 2",
              "matchTime":"2026-08-18 10:25:00","matchStatusValue":"none","winner":0,
              "score":[],
              "team1":{"countryCode":"HKG","players":[{"nameShort":"B YANG"}]},
              "team2":{"countryCode":"SGP","players":[{"nameShort":"LOH K Y "}]}}]
            """);
        List<BwfMatch> list = BwfParser.dayMatches(arr, 5601, LocalDate.of(2026, 8, 18));
        // 顺序保持 API 官方顺序（10:45 在 10:25 前，勿按时间重排）
        assertEquals("553", list.get(0).getMatchCode());
        assertEquals("49", list.get(1).getMatchCode());
        BwfMatch m = list.get(0);
        assertEquals("F", m.getStatus());
        assertEquals("N", list.get(1).getStatus()); // none → 未开赛
        assertEquals("WONG T C / LIM C S", m.getTeam1Players());
        assertEquals("3", m.getTeam2Seed());
        assertNull(m.getTeam1Seed());
        assertEquals("16-21 19-21", m.getScoreText());
        assertEquals(33, m.getDurationMin());
    }

    @Test
    void h2hGamesAndCumulativePoints() throws Exception {
        JsonNode h2h = mapper.readTree("""
            {"games":[
              {"ordering":2,"team1":19,"team2":21,
               "match_set_details_model":[{"ordering":1,"team1":1,"team2":0}]},
              {"ordering":1,"team1":16,"team2":21,
               "match_set_details_model":[
                 {"ordering":2,"team1":2,"team2":0},
                 {"ordering":1,"team1":1,"team2":0},
                 {"ordering":4,"team1":3,"team2":1},
                 {"ordering":3,"team1":3,"team2":0}
               ]}
            ]}
            """);
        var games = BwfParser.games(h2h, 99L);
        assertEquals(2, games.size());
        assertEquals(1, games.get(0).getGameNo()); // 按 ordering 排：16-21 是局1
        assertEquals(16, games.get(0).getTeam1Score());
        assertEquals(21, games.get(0).getTeam2Score());

        List<BwfMatchPoint> pts = BwfParser.points(BwfParser.nthGame(h2h, 1), 7L);
        assertEquals(4, pts.size());
        // 按 ordering 升序，值为累计得分
        assertEquals(1, pts.get(0).getTeam1());
        assertEquals(2, pts.get(1).getTeam1());
        assertEquals(0, pts.get(1).getTeam2());
        assertEquals(3, pts.get(3).getTeam1());
    }
}
