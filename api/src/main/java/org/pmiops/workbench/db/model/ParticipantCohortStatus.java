package org.pmiops.workbench.db.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.CohortStatus;

import javax.persistence.*;
import java.sql.Date;
import java.util.Objects;

@Entity
@Table(name = "participant_cohort_status")
public class ParticipantCohortStatus {

    private ParticipantCohortStatusKey participantKey;
    private CohortStatus status;
    private Long genderConceptId;
    private Date birthDate;
    private Long raceConceptId;
    private Long ethnicityConceptId;

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name="cohortReviewId",
                    column=@Column(name="cohort_review_id")),
            @AttributeOverride(name="participantId",
                    column=@Column(name="participant_id"))
    })
    public ParticipantCohortStatusKey getParticipantKey() {
        return participantKey;
    }

    public void setParticipantKey(ParticipantCohortStatusKey participantKey) {
        this.participantKey = participantKey;
    }

    public ParticipantCohortStatus participantKey(ParticipantCohortStatusKey participantKey) {
        this.participantKey = participantKey;
        return this;
    }

    @Column(name = "status")
    public CohortStatus getStatus() {
        return status;
    }

    public void setStatus(CohortStatus status) {
        this.status = status;
    }

    public ParticipantCohortStatus status(CohortStatus status) {
        this.status = status;
        return this;
    }

    @Column(name = "gender_concept_id")
    public Long getGenderConceptId() {
        return genderConceptId;
    }

    public void setGenderConceptId(Long genderConceptId) {
        this.genderConceptId = genderConceptId;
    }

    public ParticipantCohortStatus genderConceptId(Long genderConceptId) {
        this.genderConceptId = genderConceptId;
        return this;
    }

    @Column(name = "birth_date")
    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public ParticipantCohortStatus birthDate(Date birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    @Column(name = "race_concept_id")
    public Long getRaceConceptId() {
        return raceConceptId;
    }

    public void setRaceConceptId(Long raceConceptId) {
        this.raceConceptId = raceConceptId;
    }

    public ParticipantCohortStatus raceConceptId(Long raceConceptId) {
        this.raceConceptId = raceConceptId;
        return this;
    }

    @Column(name = "ethnicity_concept_id")
    public Long getEthnicityConceptId() {
        return ethnicityConceptId;
    }

    public void setEthnicityConceptId(Long ethnicityConceptId) {
        this.ethnicityConceptId = ethnicityConceptId;
    }

    public ParticipantCohortStatus ethnicityConceptId(Long ethnicityConceptId) {
        this.ethnicityConceptId = ethnicityConceptId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantCohortStatus that = (ParticipantCohortStatus) o;
        return Objects.equals(participantKey, that.participantKey) &&
                status == that.status &&
                Objects.equals(genderConceptId, that.genderConceptId) &&
                Objects.equals(birthDate, that.birthDate) &&
                Objects.equals(raceConceptId, that.raceConceptId) &&
                Objects.equals(ethnicityConceptId, that.ethnicityConceptId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participantKey, status, genderConceptId, birthDate, raceConceptId, ethnicityConceptId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("participantKey", participantKey)
                .append("status", status)
                .append("genderConceptId", genderConceptId)
                .append("birthDate", birthDate)
                .append("raceConceptId", raceConceptId)
                .append("ethnicityConceptId", ethnicityConceptId)
                .toString();
    }
}
