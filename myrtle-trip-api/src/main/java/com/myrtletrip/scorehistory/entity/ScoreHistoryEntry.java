package com.myrtletrip.scorehistory.entity;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "score_history_entry")
public class ScoreHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "course_name", length = 150)
    private String courseName;

    @Column(name = "course_rating", precision = 5, scale = 2)
    private BigDecimal courseRating;

    @Column(name = "slope")
    private Integer slope;

    @Column(name = "gross_score")
    private Integer grossScore;

    @Column(name = "adjusted_gross_score")
    private Integer adjustedGrossScore;

    @Column(name = "used_alternate_tee", nullable = false)
    private Boolean usedAlternateTee = false;
    
    @Column(name = "differential", precision = 5, scale = 1)
    private BigDecimal differential;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "included_in_myrtle_calc")
    private Boolean includedInMyrtleCalc;

    @Column(name = "handicap_group_code", length = 50)
    private String handicapGroupCode;

    @Column(name = "posting_order")
    private Integer postingOrder;

    @Column(name = "score_type", length = 20)
    private String scoreType;

    @Column(name = "holes_played")
    private Integer holesPlayed;

    @Column(name = "manual_differential_required")
    private Boolean manualDifferentialRequired = false;

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public LocalDate getScoreDate() {
        return scoreDate;
    }

    public void setScoreDate(LocalDate scoreDate) {
        this.scoreDate = scoreDate;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public BigDecimal getCourseRating() {
        return courseRating;
    }

    public void setCourseRating(BigDecimal courseRating) {
        this.courseRating = courseRating;
    }

    public Integer getSlope() {
        return slope;
    }

    public void setSlope(Integer slope) {
        this.slope = slope;
    }

    public Integer getGrossScore() {
        return grossScore;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public Integer getAdjustedGrossScore() {
        return adjustedGrossScore;
    }

    public void setAdjustedGrossScore(Integer adjustedGrossScore) {
        this.adjustedGrossScore = adjustedGrossScore;
    }

    public BigDecimal getDifferential() {
        return differential;
    }

    public void setDifferential(BigDecimal differential) {
        this.differential = differential;
    }

    public Boolean getUsedAlternateTee() {
        return usedAlternateTee;
    }

    public void setUsedAlternateTee(Boolean usedAlternateTee) {
        this.usedAlternateTee = usedAlternateTee;
    }
    
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Boolean getIncludedInMyrtleCalc() {
        return includedInMyrtleCalc;
    }

    public void setIncludedInMyrtleCalc(Boolean includedInMyrtleCalc) {
        this.includedInMyrtleCalc = includedInMyrtleCalc;
    }

    public String getHandicapGroupCode() {
        return handicapGroupCode;
    }

    public void setHandicapGroupCode(String handicapGroupCode) {
        this.handicapGroupCode = handicapGroupCode;
    }

    public Integer getPostingOrder() {
        return postingOrder;
    }

    public void setPostingOrder(Integer postingOrder) {
        this.postingOrder = postingOrder;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public Integer getHolesPlayed() {
        return holesPlayed;
    }

    public void setHolesPlayed(Integer holesPlayed) {
        this.holesPlayed = holesPlayed;
    }

    public Boolean getManualDifferentialRequired() {
        return manualDifferentialRequired;
    }

    public void setManualDifferentialRequired(Boolean manualDifferentialRequired) {
        this.manualDifferentialRequired = manualDifferentialRequired;
    }
}