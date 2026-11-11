package com.aquascope.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "analysis_reviews")
public class AnalysisReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_analysis_id", nullable = false)
    private SampleAnalysis sampleAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id", nullable = false)
    private User reviewerUser;

    @Column(nullable = false, length = 50)
    private String decision; // APPROVED, REJECTED, CORRECTION_REQUESTED

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comments;

    @Column(name = "review_date")
    private Instant reviewDate = Instant.now();

    public AnalysisReview() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SampleAnalysis getSampleAnalysis() { return sampleAnalysis; }
    public void setSampleAnalysis(SampleAnalysis sampleAnalysis) { this.sampleAnalysis = sampleAnalysis; }

    public User getReviewerUser() { return reviewerUser; }
    public void setReviewerUser(User reviewerUser) { this.reviewerUser = reviewerUser; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Instant getReviewDate() { return reviewDate; }
    public void setReviewDate(Instant reviewDate) { this.reviewDate = reviewDate; }
}
