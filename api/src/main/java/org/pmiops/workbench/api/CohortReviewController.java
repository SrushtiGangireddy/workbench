package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.gson.Gson;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityType;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CohortSummaryListResponse;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.ParticipantCohortStatusesRequest;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortReviewController implements CohortReviewApiDelegate {

    public static final Integer PAGE = 0;
    public static final Integer PAGE_SIZE = 25;
    public static final Integer MAX_REVIEW_SIZE = 10000;

    private CohortReviewService cohortReviewService;
    private BigQueryService bigQueryService;
    private CodeDomainLookupService codeDomainLookupService;
    private ParticipantCounter participantCounter;
    private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<ParticipantCohortStatus, org.pmiops.workbench.model.ParticipantCohortStatus>
            TO_CLIENT_PARTICIPANT =
            new Function<ParticipantCohortStatus, org.pmiops.workbench.model.ParticipantCohortStatus>() {
                @Override
                public org.pmiops.workbench.model.ParticipantCohortStatus apply(ParticipantCohortStatus participant) {
                    return new org.pmiops.workbench.model.ParticipantCohortStatus()
                            .participantId(participant.getParticipantKey().getParticipantId())
                            .status(participant.getStatus())
                            .birthDatetime(participant.getBirthDate().getTime())
                            .ethnicityConceptId(participant.getEthnicityConceptId())
                            .ethnicity(participant.getEthnicity())
                            .genderConceptId(participant.getGenderConceptId())
                            .gender(participant.getGender())
                            .raceConceptId(participant.getRaceConceptId())
                            .race(participant.getRace());
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final BiFunction<CohortReview, PageRequest, org.pmiops.workbench.model.CohortReview>
            TO_CLIENT_COHORTREVIEW =
            new BiFunction<CohortReview, PageRequest, org.pmiops.workbench.model.CohortReview>() {
                @Override
                public org.pmiops.workbench.model.CohortReview apply(CohortReview cohortReview, PageRequest pageRequest) {
                    return new org.pmiops.workbench.model.CohortReview()
                            .cohortReviewId(cohortReview.getCohortReviewId())
                            .cohortId(cohortReview.getCohortId())
                            .cdrVersionId(cohortReview.getCdrVersionId())
                            .creationTime(cohortReview.getCreationTime().toString())
                            .matchedParticipantCount(cohortReview.getMatchedParticipantCount())
                            .reviewedCount(cohortReview.getReviewedCount())
                            .reviewStatus(cohortReview.getReviewStatus())
                            .reviewSize(cohortReview.getReviewSize())
                            .page(pageRequest.getPageNumber())
                            .pageSize(pageRequest.getPageSize())
                            .sortOrder(pageRequest.getSortOrder().toString())
                            .sortColumn(pageRequest.getSortColumn().toString());
                }
            };

    @Autowired
    CohortReviewController(CohortReviewService cohortReviewService,
        BigQueryService bigQueryService,
        CodeDomainLookupService codeDomainLookupService,
        ParticipantCounter participantCounter,
        Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider) {
        this.cohortReviewService = cohortReviewService;
        this.bigQueryService = bigQueryService;
        this.codeDomainLookupService = codeDomainLookupService;
        this.participantCounter = participantCounter;
        this.genderRaceEthnicityConceptProvider = genderRaceEthnicityConceptProvider;
    }

    /**
     * Create a cohort review per the specified workspaceId, cohortId, cdrVersionId and size. If participant cohort status
     * data exists for a review or no cohort review exists for cohortReviewId then throw a
     * {@link BadRequestException}.
     *
     * @param workspaceNamespace
     * @param workspaceId
     * @param cohortId
     * @param cdrVersionId
     * @param request
     */
    @Override
    public ResponseEntity<org.pmiops.workbench.model.CohortReview> createCohortReview(String workspaceNamespace,
                                                                                      String workspaceId,
                                                                                      Long cohortId,
                                                                                      Long cdrVersionId,
                                                                                      CreateReviewRequest request) {
        if (request.getSize() <= 0 || request.getSize() > MAX_REVIEW_SIZE) {
            throw new BadRequestException(
                    String.format("Invalid Request: Cohort Review size must be between %s and %s", 0, MAX_REVIEW_SIZE));
        }
        CohortReview cohortReview = null;
        try {
            cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
        } catch (NotFoundException nfe) {
            cohortReview = initializeAndSaveCohortReview(workspaceNamespace, workspaceId, cohortId, cdrVersionId);
        }
        if(cohortReview.getReviewSize() > 0) {
            throw new BadRequestException(
                    String.format("Invalid Request: Cohort Review already created for cohortId: %s, cdrVersionId: %s",
                            cohortId, cdrVersionId));
        }

        Cohort cohort = cohortReviewService.findCohort(cohortId);
        //this validates that the user is in the proper workspace
        Workspace workspace = cohortReviewService.validateMatchingWorkspace(workspaceNamespace,
            workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);

        CdrVersionContext.setCdrVersion(workspace.getCdrVersion());
        SearchRequest searchRequest = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);


        codeDomainLookupService.findCodesForEmptyDomains(searchRequest.getIncludes());
        codeDomainLookupService.findCodesForEmptyDomains(searchRequest.getExcludes());

        QueryResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(
                participantCounter.buildParticipantIdQuery(searchRequest, request.getSize(), 0L)));
        Map<String, Integer> rm = bigQueryService.getResultMapper(result);

        List<ParticipantCohortStatus> participantCohortStatuses =
                createParticipantCohortStatusesList(cohortReview.getCohortReviewId(), result, rm);

        cohortReview
                .reviewSize(participantCohortStatuses.size())
                .reviewedCount(0L)
                .reviewStatus(ReviewStatus.CREATED);

        //when saving ParticipantCohortStatuses to the database the long value of birthdate is mutated.
        cohortReviewService.saveFullCohortReview(cohortReview, participantCohortStatuses);

        List<ParticipantCohortStatus> paginatedPCS =
                cohortReviewService.findAll(cohortReview.getCohortReviewId(),
                        Collections.<Filter>emptyList(),
                        createPageRequest(PAGE, PAGE_SIZE, SortOrder.ASC, ParticipantCohortStatusColumns.PARTICIPANTID));
        lookupGenderRaceEthnicityValues(paginatedPCS);

        org.pmiops.workbench.model.CohortReview responseReview =
                TO_CLIENT_COHORTREVIEW.apply(cohortReview, createPageRequest(PAGE, PAGE_SIZE, SortOrder.ASC, ParticipantCohortStatusColumns.PARTICIPANTID));
        responseReview.setParticipantCohortStatuses(paginatedPCS.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));

        return ResponseEntity.ok(responseReview);
    }

    @Override
    public ResponseEntity<ParticipantCohortAnnotation> createParticipantCohortAnnotation(String workspaceNamespace,
                                                                                         String workspaceId,
                                                                                         Long cohortReviewId,
                                                                                         Long participantId,
                                                                                         ParticipantCohortAnnotation request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ParticipantCohortAnnotation());
    }

    @Override
    public ResponseEntity<EmptyResponse> deleteParticipantCohortAnnotation(String workspaceNamespace,
                                                                           String workspaceId,
                                                                           Long cohortReviewId,
                                                                           Long participantId,
                                                                           Long annotationId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new EmptyResponse());
    }

    @Override
    public ResponseEntity<CohortSummaryListResponse> getCohortSummary(String workspaceNamespace,
                                                                      String workspaceId,
                                                                      Long cohortId,
                                                                      Long cdrVersionId,
                                                                      String domain) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new CohortSummaryListResponse());
    }

    @Override
    public ResponseEntity<ParticipantCohortAnnotationListResponse> getParticipantCohortAnnotations(String workspaceNamespace,
                                                                                                   String workspaceId,
                                                                                                   Long cohortReviewId,
                                                                                                   Long participantId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ParticipantCohortAnnotationListResponse());
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus> getParticipantCohortStatus(String workspaceNamespace,
                                                                                                         String workspaceId,
                                                                                                         Long cohortId,
                                                                                                         Long cdrVersionId,
                                                                                                         Long participantId) {
        Cohort cohort = cohortReviewService.findCohort(cohortId);
        //this validates that the user is in the proper workspace
        Workspace workspace =
            cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceId,
                cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);
        CdrVersionContext.setCdrVersion(workspace.getCdrVersion());
        CohortReview review = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
        ParticipantCohortStatus status =
                cohortReviewService.findParticipantCohortStatus(review.getCohortReviewId(), participantId);
        lookupGenderRaceEthnicityValues(Arrays.asList(status));
        return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(status));
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.ParticipantCohortStatus> updateParticipantCohortStatus(String workspaceNamespace,
                                                                                                            String workspaceId,
                                                                                                            Long cohortId,
                                                                                                            Long cdrVersionId,
                                                                                                            Long participantId,
                                                                                                            ModifyCohortStatusRequest cohortStatusRequest) {
        Cohort cohort = cohortReviewService.findCohort(cohortId);
        //this validates that the user is in the proper workspace
        cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);

        CohortReview cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);

        ParticipantCohortStatus participantCohortStatus
                = cohortReviewService.findParticipantCohortStatus(cohortReview.getCohortReviewId(), participantId);

        participantCohortStatus.setStatus(cohortStatusRequest.getStatus());
        cohortReviewService.saveParticipantCohortStatus(participantCohortStatus);

        cohortReview.lastModifiedTime(new Timestamp(System.currentTimeMillis()));
        cohortReview.incrementReviewedCount();
        cohortReviewService.saveCohortReview(cohortReview);

        return ResponseEntity.ok(TO_CLIENT_PARTICIPANT.apply(participantCohortStatus));
    }

    /**
     * Get all participants for the specified cohortId and cdrVersionId. This endpoint does pagination
     * based on page, pageSize, sortOrder and sortColumn.
     */
    @Override
    public ResponseEntity<org.pmiops.workbench.model.CohortReview> getParticipantCohortStatuses(String workspaceNamespace,
                                                                                                String workspaceId,
                                                                                                Long cohortId,
                                                                                                Long cdrVersionId,
                                                                                                ParticipantCohortStatusesRequest request) {
        CohortReview cohortReview = null;
        Cohort cohort = cohortReviewService.findCohort(cohortId);

        Workspace workspace = cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceId,
            cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);
        CdrVersionContext.setCdrVersion(workspace.getCdrVersion());
        try {
            cohortReview = cohortReviewService.findCohortReview(cohortId, cdrVersionId);
        } catch (NotFoundException nfe) {
            cohortReview = initializeAndSaveCohortReview(workspaceNamespace, workspaceId, cohortId, cdrVersionId);
        }

        PageRequest pageRequest = createPageRequest(request.getPage(),
                request.getPageSize(),
                request.getSortOrder(),
                request.getSortColumn());

        List<Filter> filters = request.getFilters() == null ? Collections.<Filter>emptyList() : request.getFilters().getItems();
        List<ParticipantCohortStatus> participantCohortStatuses =
                cohortReviewService.findAll(cohortReview.getCohortReviewId(), filters, pageRequest);

        org.pmiops.workbench.model.CohortReview responseReview = TO_CLIENT_COHORTREVIEW.apply(cohortReview, pageRequest);
        responseReview.setParticipantCohortStatuses(
                participantCohortStatuses.stream().map(TO_CLIENT_PARTICIPANT).collect(Collectors.toList()));

        return ResponseEntity.ok(responseReview);
    }

    @Override
    public ResponseEntity<ParticipantDemographics> getParticipantDemographics(String workspaceNamespace, String workspaceId, Long cohortId, Long cdrVersionId) {
        Cohort cohort = cohortReviewService.findCohort(cohortId);

        Workspace workspace = cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceId,
                cohort.getWorkspaceId(), WorkspaceAccessLevel.READER);
        CdrVersionContext.setCdrVersion(workspace.getCdrVersion());

        Map<String, Map<Long, String>> concepts = genderRaceEthnicityConceptProvider.get().getConcepts();
        List<ConceptIdName> genderList = concepts.get(GenderRaceEthnicityType.GENDER.name()).entrySet().stream()
                .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
                .collect(Collectors.toList());
        List<ConceptIdName> raceList = concepts.get(GenderRaceEthnicityType.RACE.name()).entrySet().stream()
                .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
                .collect(Collectors.toList());
        List<ConceptIdName> ethnicityList = concepts.get(GenderRaceEthnicityType.ETHNICITY.name()).entrySet().stream()
                .map(e -> new ConceptIdName().conceptId(e.getKey()).conceptName(e.getValue()))
                .collect(Collectors.toList());

        ParticipantDemographics participantDemographics =
                new ParticipantDemographics().genderList(genderList).raceList(raceList).ethnicityList(ethnicityList);
        return ResponseEntity.ok(participantDemographics);
    }

    @Override
    public ResponseEntity<ParticipantCohortAnnotation>
    updateParticipantCohortAnnotation(String workspaceNamespace,
                                      String workspaceId,
                                      Long cohortReviewId,
                                      Long participantId,
                                      Long annotationId,
                                      ModifyParticipantCohortAnnotationRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ParticipantCohortAnnotation());
    }

    /**
     * Helper method to create a new {@link CohortReview} and persist it to the workbench database.
     *
     * @param workspaceNamespace
     * @param workspaceId
     * @param cohortId
     * @param cdrVersionId
     */
    private CohortReview initializeAndSaveCohortReview(String workspaceNamespace,
                                                       String workspaceId,
                                                       Long cohortId,
                                                       Long cdrVersionId) {
        Cohort cohort = cohortReviewService.findCohort(cohortId);
        //this validates that the user is in the proper workspace
        Workspace workspace = cohortReviewService.validateMatchingWorkspace(workspaceNamespace, workspaceId,
            cohort.getWorkspaceId(), WorkspaceAccessLevel.WRITER);
        CdrVersionContext.setCdrVersion(workspace.getCdrVersion());

        SearchRequest request = new Gson().fromJson(getCohortDefinition(cohort), SearchRequest.class);

        codeDomainLookupService.findCodesForEmptyDomains(request.getIncludes());
        codeDomainLookupService.findCodesForEmptyDomains(request.getExcludes());

        QueryResult result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(participantCounter.buildParticipantCounterQuery(request)));
        Map<String, Integer> rm = bigQueryService.getResultMapper(result);
        List<FieldValue> row = result.iterateAll().iterator().next();
        long cohortCount = bigQueryService.getLong(row, rm.get("count"));

        CohortReview cohortReview = createNewCohortReview(cohortId, cdrVersionId, cohortCount);
        return cohortReviewService.saveCohortReview(cohortReview);
    }

    private List<ParticipantCohortStatus> createParticipantCohortStatusesList(Long cohortReviewId,
                                                                              QueryResult result,
                                                                              Map<String, Integer> rm) {
        List<ParticipantCohortStatus> participantCohortStatuses = new ArrayList<>();
        for (List<FieldValue> row : result.iterateAll()) {
            String birthDateTimeString = bigQueryService.getString(row, rm.get("birth_datetime"));
            if (birthDateTimeString == null) {
                throw new BigQueryException(500, "birth_datetime is null at position: " + rm.get("birth_datetime"));
            }
            Date birthDate = Date.from(Instant.ofEpochMilli(Double.valueOf(birthDateTimeString).longValue() * 1000));
            participantCohortStatuses.add(
                    new ParticipantCohortStatus()
                            .participantKey(
                                    new ParticipantCohortStatusKey(
                                            cohortReviewId,
                                            bigQueryService.getLong(row, rm.get("person_id"))))
                            .status(CohortStatus.NOT_REVIEWED)
                            .birthDate(new java.sql.Date(birthDate.getTime()))
                            .genderConceptId(bigQueryService.getLong(row, rm.get("gender_concept_id")))
                            .raceConceptId(bigQueryService.getLong(row, rm.get("race_concept_id")))
                            .ethnicityConceptId(bigQueryService.getLong(row, rm.get("ethnicity_concept_id"))));
        }
        return participantCohortStatuses;
    }

    private String getCohortDefinition(Cohort cohort) {
        String definition = cohort.getCriteria();
        if (definition == null) {
            throw new NotFoundException(
                    String.format("Not Found: No Cohort definition matching cohortId: %s", cohort.getCohortId()));
        }
        return definition;
    }

    private PageRequest createPageRequest(Integer page, Integer pageSize, SortOrder sortOrder, ParticipantCohortStatusColumns sortColumn) {
        int pageParam = Optional.ofNullable(page).orElse(PAGE);
        int pageSizeParam = Optional.ofNullable(pageSize).orElse(PAGE_SIZE);
        SortOrder sortOrderParam = Optional.ofNullable(sortOrder).orElse(SortOrder.ASC);
        ParticipantCohortStatusColumns sortColumnParam = Optional.ofNullable(sortColumn).orElse(sortColumn.PARTICIPANTID);
        return new PageRequest(pageParam, pageSizeParam, sortOrderParam, sortColumnParam);
    }

    private CohortReview createNewCohortReview(Long cohortId, Long cdrVersionId, long cohortCount) {
        CohortReview cohortReview;
        cohortReview = new CohortReview();
        cohortReview.setCohortId(cohortId);
        cohortReview.setCdrVersionId(cdrVersionId);
        cohortReview.matchedParticipantCount(cohortCount);
        cohortReview.setCreationTime(new Timestamp(System.currentTimeMillis()));
        cohortReview.reviewedCount(0L);
        cohortReview.reviewStatus(ReviewStatus.NONE);
        return cohortReview;
    }

    private void lookupGenderRaceEthnicityValues(List<ParticipantCohortStatus> participantCohortStatuses) {
        Map<String, Map<Long, String>> concepts = genderRaceEthnicityConceptProvider.get().getConcepts();
        participantCohortStatuses.forEach(pcs -> {
            pcs.setRace(concepts.get(GenderRaceEthnicityType.RACE.name()).get(pcs.getRaceConceptId()));
            pcs.setGender(concepts.get(GenderRaceEthnicityType.GENDER.name()).get(pcs.getGenderConceptId()));
            pcs.setEthnicity(concepts.get(GenderRaceEthnicityType.ETHNICITY.name()).get(pcs.getEthnicityConceptId()));
        });
    }

}
