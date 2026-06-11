package server.demo.service;

import server.demo.entity.MessageKnowledgeEntry;

import java.util.ArrayList;
import java.util.List;

public class MessageKnowledgeMatch {
    private MessageKnowledgeEntry entry;
    private double score;
    private String scopeLevel;
    private List<String> matchReasons = new ArrayList<>();
    private List<String> policyTopics = new ArrayList<>();
    private List<String> reusableFacts = new ArrayList<>();

    public MessageKnowledgeMatch() {
    }

    public MessageKnowledgeMatch(
            MessageKnowledgeEntry entry,
            double score,
            String scopeLevel,
            List<String> matchReasons
    ) {
        this(entry, score, scopeLevel, matchReasons, List.of(), List.of());
    }

    public MessageKnowledgeMatch(
            MessageKnowledgeEntry entry,
            double score,
            String scopeLevel,
            List<String> matchReasons,
            List<String> policyTopics,
            List<String> reusableFacts
    ) {
        this.entry = entry;
        this.score = score;
        this.scopeLevel = scopeLevel;
        setMatchReasons(matchReasons);
        setPolicyTopics(policyTopics);
        setReusableFacts(reusableFacts);
    }

    public MessageKnowledgeEntry getEntry() {
        return entry;
    }

    public void setEntry(MessageKnowledgeEntry entry) {
        this.entry = entry;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getScopeLevel() {
        return scopeLevel;
    }

    public void setScopeLevel(String scopeLevel) {
        this.scopeLevel = scopeLevel;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = matchReasons == null ? new ArrayList<>() : matchReasons;
    }

    public List<String> getPolicyTopics() {
        return policyTopics;
    }

    public void setPolicyTopics(List<String> policyTopics) {
        this.policyTopics = policyTopics == null ? new ArrayList<>() : policyTopics;
    }

    public List<String> getReusableFacts() {
        return reusableFacts;
    }

    public void setReusableFacts(List<String> reusableFacts) {
        this.reusableFacts = reusableFacts == null ? new ArrayList<>() : reusableFacts;
    }
}
