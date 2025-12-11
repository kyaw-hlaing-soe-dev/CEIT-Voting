package com.KTU.KTUVotingapp.dto;

import com.KTU.KTUVotingapp.model.Category;

import java.util.List;

public class ResultDTO {

    private Category category;
    private Long totalVotes;
    private List<CandidateResultDTO> candidates;

    public ResultDTO() {
    }

    public ResultDTO(Category category, Long totalVotes, List<CandidateResultDTO> candidates) {
        this.category = category;
        this.totalVotes = totalVotes;
        this.candidates = candidates;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Long getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Long totalVotes) {
        this.totalVotes = totalVotes;
    }

    public List<CandidateResultDTO> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<CandidateResultDTO> candidates) {
        this.candidates = candidates;
    }

    public static class CandidateResultDTO {
        private Long id;
        private Integer candidateNumber;
        private String name;
        private String department;
        private String imageUrl;
        private Long voteCount;
        private Double percentage;

        public CandidateResultDTO() {
        }

        public CandidateResultDTO(Long id, Integer candidateNumber, String name, String department,
                                String imageUrl, Long voteCount, Double percentage) {
            this.id = id;
            this.candidateNumber = candidateNumber;
            this.name = name;
            this.department = department;
            this.imageUrl = imageUrl;
            this.voteCount = voteCount;
            this.percentage = percentage;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getCandidateNumber() {
            return candidateNumber;
        }

        public void setCandidateNumber(Integer candidateNumber) {
            this.candidateNumber = candidateNumber;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public Long getVoteCount() {
            return voteCount;
        }

        public void setVoteCount(Long voteCount) {
            this.voteCount = voteCount;
        }

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }
}

