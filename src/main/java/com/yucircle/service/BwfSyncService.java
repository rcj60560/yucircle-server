package com.yucircle.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yucircle.entity.BwfMatch;
import com.yucircle.entity.BwfMatchGame;
import com.yucircle.entity.BwfMatchPoint;
import com.yucircle.entity.BwfRankingEntry;
import com.yucircle.entity.BwfTournament;
import com.yucircle.mapper.BwfMatchGameMapper;
import com.yucircle.mapper.BwfMatchMapper;
import com.yucircle.mapper.BwfRankingEntryMapper;
import com.yucircle.mapper.BwfTournamentMapper;
import com.yucircle.util.BwfExtranetClient;
import com.yucircle.util.BwfParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BWF 抓取→解析→幂等落库（INSERT ... ON DUPLICATE KEY UPDATE，重抓即修正）。
 * 调试入口 POST /api/bwf/admin/refresh；定时入口 BwfSyncTasks。设计见 doc/06。
 */
@Service
public class BwfSyncService {

    /** rankId=2 = BWF World Rankings（9 是 Race 榜，勿动） */
    private static final int RANK_ID = 2;
    private static final Map<String, Integer> DISCIPLINE_CAT = Map.of(
            "ms", 6, "ws", 7, "md", 8, "wd", 9, "xd", 10);

    private final BwfExtranetClient client;
    private final JdbcTemplate jdbc;
    private final BwfTournamentMapper tournamentMapper;
    private final BwfMatchMapper matchMapper;
    private final BwfMatchGameMapper gameMapper;
    private final BwfRankingEntryMapper rankingMapper;

    /** 逐分只抓这些 level 的赛事（v0=major；扩全站改配置即可） */
    @Value("${bwf.sync.point-sync-levels:major}")
    private String pointSyncLevels;

    public BwfSyncService(BwfExtranetClient client, JdbcTemplate jdbc,
                          BwfTournamentMapper tournamentMapper, BwfMatchMapper matchMapper,
                          BwfMatchGameMapper gameMapper, BwfRankingEntryMapper rankingMapper) {
        this.client = client;
        this.jdbc = jdbc;
        this.tournamentMapper = tournamentMapper;
        this.matchMapper = matchMapper;
        this.gameMapper = gameMapper;
        this.rankingMapper = rankingMapper;
    }

    /** 该赛事是否需要抓逐分（纯函数，可单测） */
    boolean shouldSyncPoints(String level) {
        return Arrays.stream(pointSyncLevels.split(","))
                .map(String::trim)
                .anyMatch(level::equals);
    }

    /** 最新一期世界排名（5 单项 × Top50）。返回入库条数。 */
    public int syncRankings() throws Exception {
        JsonNode weeks = client.fetchRankingWeek(RANK_ID);
        String pubId = BwfParser.publicationId(weeks);
        LocalDate pubDate = BwfParser.publicationDate(weeks);
        int total = 0;
        for (Map.Entry<String, Integer> d : DISCIPLINE_CAT.entrySet()) {
            JsonNode table = client.fetchRankingTable(RANK_ID, d.getValue(), pubId);
            List<BwfRankingEntry> entries = BwfParser.rankings(table, d.getKey(), pubDate);
            jdbc.batchUpdate(
                    "INSERT INTO bwf_ranking_entry"
                            + "(discipline, publication_date, `rank_num`, rank_change, country, player_name, points, created_at, updated_at) "
                            + "VALUES (?,?,?,?,?,?,?,?,NOW()) "
                            + "ON DUPLICATE KEY UPDATE rank_change=VALUES(rank_change), country=VALUES(country), "
                            + "player_name=VALUES(player_name), points=VALUES(points), updated_at=NOW()",
                    entries, entries.size(),
                    (ps, e) -> {
                        ps.setString(1, e.getDiscipline());
                        ps.setDate(2, Date.valueOf(e.getPublicationDate()));
                        ps.setInt(3, e.getRankNum());
                        ps.setInt(4, e.getRankChange());
                        ps.setString(5, e.getCountry());
                        ps.setString(6, e.getPlayerName());
                        ps.setInt(7, e.getPoints());
                        ps.setTimestamp(8, Timestamp.valueOf(java.time.LocalDateTime.now()));
                    });
            total += entries.size();
        }
        return total;
    }

    /** 当年赛程（World Tour + Grade 1 大赛）。返回入库条数。 */
    public int syncSchedule() throws Exception {
        int year = LocalDate.now().getYear();
        JsonNode grouped = client.fetchYearTournaments(year);
        List<BwfTournament> list = BwfParser.tournaments(grouped, year);
        jdbc.batchUpdate(
                "INSERT INTO bwf_tournament"
                        + "(tmt_id, name, level, start_date, end_date, city, country, prize_money, code, has_live_scores, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW()) "
                        + "ON DUPLICATE KEY UPDATE name=VALUES(name), level=VALUES(level), start_date=VALUES(start_date), "
                        + "end_date=VALUES(end_date), city=VALUES(city), country=VALUES(country), prize_money=VALUES(prize_money), "
                        + "code=VALUES(code), has_live_scores=VALUES(has_live_scores), updated_at=NOW()",
                list, list.size(),
                (ps, t) -> {
                    ps.setInt(1, t.getTmtId());
                    ps.setString(2, t.getName());
                    ps.setString(3, t.getLevel());
                    ps.setDate(4, Date.valueOf(t.getStartDate()));
                    ps.setDate(5, Date.valueOf(t.getEndDate()));
                    ps.setString(6, t.getCity());
                    ps.setString(7, t.getCountry());
                    ps.setInt(8, t.getPrizeMoney() == null ? 0 : t.getPrizeMoney());
                    ps.setString(9, t.getCode());
                    ps.setBoolean(10, Boolean.TRUE.equals(t.getHasLiveScores()));
                    ps.setTimestamp(11, Timestamp.valueOf(java.time.LocalDateTime.now()));
                });
        return list.size();
    }

    /** 某日赛果：对当天在赛期内的 live 赛事抓当日对阵；级别达标的已结束场次顺带抓逐分。 */
    public int syncDayMatches(LocalDate date) throws Exception {
        List<BwfTournament> ongoing = tournamentMapper.selectList(new QueryWrapper<BwfTournament>()
                .eq("has_live_scores", 1)
                .le("start_date", date)
                .ge("end_date", date));
        int total = 0;
        for (BwfTournament t : ongoing) {
            JsonNode arr = client.fetchDayMatches(t.getCode(), date.toString());
            List<BwfMatch> matches = BwfParser.dayMatches(arr, t.getTmtId(), date);
            upsertMatches(matches);
            total += matches.size();
            if (shouldSyncPoints(t.getLevel())) {
                for (BwfMatch m : matches) {
                    if ("F".equals(m.getStatus()) && m.getMatchCode() != null && !m.getMatchCode().isBlank()) {
                        syncMatchPoints(t.getTmtId(), m.getMatchCode());
                    }
                }
            }
        }
        return total;
    }

    /** 单场逐分（含局终分校正）。match 按 (tmt_id, match_code) 定位。 */
    public int syncMatchPoints(int tmtId, String matchCode) throws Exception {
        BwfMatch match = matchMapper.selectOne(new QueryWrapper<BwfMatch>()
                .eq("tmt_id", tmtId).eq("match_code", matchCode));
        if (match == null) {
            return 0; // 先 syncDayMatches 再抓逐分
        }
        JsonNode h2h = client.fetchH2hMatch(tmtId, matchCode);
        List<BwfMatchGame> games = BwfParser.games(h2h, match.getId());
        int points = 0;
        for (BwfMatchGame g : games) {
            jdbc.update(
                    "INSERT INTO bwf_match_game(match_id, game_no, team1_score, team2_score, created_at, updated_at) "
                            + "VALUES (?,?,?,?,NOW(),NOW()) "
                            + "ON DUPLICATE KEY UPDATE team1_score=VALUES(team1_score), team2_score=VALUES(team2_score), updated_at=NOW()",
                    g.getMatchId(), g.getGameNo(), g.getTeam1Score(), g.getTeam2Score());
            BwfMatchGame saved = gameMapper.selectOne(new QueryWrapper<BwfMatchGame>()
                    .eq("match_id", g.getMatchId()).eq("game_no", g.getGameNo()));
            List<BwfMatchPoint> pts = BwfParser.points(BwfParser.nthGame(h2h, g.getGameNo()), saved.getId());
            jdbc.batchUpdate(
                    "INSERT INTO bwf_match_point(game_id, ordering, team1, team2, created_at, updated_at) "
                            + "VALUES (?,?,?,?,NOW(),NOW()) "
                            + "ON DUPLICATE KEY UPDATE team1=VALUES(team1), team2=VALUES(team2), updated_at=NOW()",
                    pts, pts.size(),
                    (ps, p) -> {
                        ps.setLong(1, p.getGameId());
                        ps.setInt(2, p.getOrdering());
                        ps.setInt(3, p.getTeam1());
                        ps.setInt(4, p.getTeam2());
                    });
            points += pts.size();
        }
        return points;
    }

    private void upsertMatches(List<BwfMatch> matches) {
        if (matches.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(
                "INSERT INTO bwf_match"
                        + "(tmt_id, match_code, order_no, match_date, event, round_name, court_name, match_time, status, winner, duration_min, "
                        + "team1_country, team1_players, team1_seed, team2_country, team2_players, team2_seed, score_text, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW()) "
                        + "ON DUPLICATE KEY UPDATE order_no=VALUES(order_no), match_date=VALUES(match_date), event=VALUES(event), round_name=VALUES(round_name), "
                        + "court_name=VALUES(court_name), match_time=VALUES(match_time), status=VALUES(status), winner=VALUES(winner), "
                        + "duration_min=VALUES(duration_min), team1_country=VALUES(team1_country), team1_players=VALUES(team1_players), "
                        + "team1_seed=VALUES(team1_seed), team2_country=VALUES(team2_country), team2_players=VALUES(team2_players), "
                        + "team2_seed=VALUES(team2_seed), score_text=VALUES(score_text), updated_at=NOW()",
                matches, matches.size(),
                (ps, m) -> {
                    ps.setInt(1, m.getTmtId());
                    ps.setString(2, m.getMatchCode());
                    ps.setInt(3, m.getOrderNo());
                    ps.setDate(4, Date.valueOf(m.getMatchDate()));
                    ps.setString(5, m.getEvent());
                    ps.setString(6, m.getRoundName());
                    ps.setString(7, m.getCourtName());
                    ps.setTimestamp(8, m.getMatchTime() == null ? null : Timestamp.valueOf(m.getMatchTime()));
                    ps.setString(9, m.getStatus());
                    ps.setInt(10, m.getWinner());
                    if (m.getDurationMin() == null) {
                        ps.setNull(11, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(11, m.getDurationMin());
                    }
                    ps.setString(12, m.getTeam1Country());
                    ps.setString(13, m.getTeam1Players());
                    ps.setString(14, m.getTeam1Seed());
                    ps.setString(15, m.getTeam2Country());
                    ps.setString(16, m.getTeam2Players());
                    ps.setString(17, m.getTeam2Seed());
                    ps.setString(18, m.getScoreText());
                    ps.setTimestamp(19, Timestamp.valueOf(java.time.LocalDateTime.now()));
                });
    }

    // ================= 查询（供 BwfController）=================

    /** 最新一期排名（无参默认 ms） */
    public List<BwfRankingEntry> latestRankings(String discipline) {
        String disc = discipline == null || discipline.isBlank() ? "ms" : discipline;
        BwfRankingEntry latest = rankingMapper.selectOne(new QueryWrapper<BwfRankingEntry>()
                .eq("discipline", disc).orderByDesc("publication_date").last("LIMIT 1"));
        if (latest == null) {
            return List.of();
        }
        return rankingMapper.selectList(new QueryWrapper<BwfRankingEntry>()
                .eq("discipline", disc)
                .eq("publication_date", latest.getPublicationDate())
                .orderByAsc("rank_num"));
    }

    /** 单球员走势（本年，按期升序） */
    public List<BwfRankingEntry> rankingHistory(String discipline, String player) {
        return rankingMapper.selectList(new QueryWrapper<BwfRankingEntry>()
                .eq("discipline", discipline)
                .like("player_name", player)
                .ge("publication_date", LocalDate.now().withDayOfYear(1))
                .orderByAsc("publication_date"));
    }

    public List<BwfTournament> schedule() {
        return tournamentMapper.selectList(new QueryWrapper<BwfTournament>()
                .ge("end_date", LocalDate.now().withDayOfYear(1))
                .orderByAsc("start_date"));
    }

    /** 某赛事某日全部对阵（按官方对阵顺序 order_no） */
    public List<BwfMatch> matches(int tmtId, LocalDate date) {
        return matchMapper.selectList(new QueryWrapper<BwfMatch>()
                .eq("tmt_id", tmtId)
                .eq(date != null, "match_date", date)
                .orderByAsc("order_no"));
    }

    public Map<String, Object> matchDetail(int tmtId, String matchCode) {
        BwfMatch match = matchMapper.selectOne(new QueryWrapper<BwfMatch>()
                .eq("tmt_id", tmtId).eq("match_code", matchCode));
        if (match == null) {
            return null;
        }
        List<BwfMatchGame> games = gameMapper.selectList(new QueryWrapper<BwfMatchGame>()
                .eq("match_id", match.getId()).orderByAsc("game_no"));
        List<Map<String, Object>> gameList = new ArrayList<>();
        for (BwfMatchGame g : games) {
            List<BwfMatchPoint> pts = jdbc.query(
                    "SELECT id, game_id, ordering, team1, team2 FROM bwf_match_point WHERE game_id=? ORDER BY ordering",
                    (rs, i) -> {
                        BwfMatchPoint p = new BwfMatchPoint();
                        p.setGameId(rs.getLong("game_id"));
                        p.setOrdering(rs.getInt("ordering"));
                        p.setTeam1(rs.getInt("team1"));
                        p.setTeam2(rs.getInt("team2"));
                        return p;
                    }, g.getId());
            Map<String, Object> gm = new HashMap<>();
            gm.put("gameNo", g.getGameNo());
            gm.put("team1Score", g.getTeam1Score());
            gm.put("team2Score", g.getTeam2Score());
            gm.put("points", pts);
            gameList.add(gm);
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("match", match);
        detail.put("games", gameList);
        return detail;
    }
}
