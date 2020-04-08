package net.trilogy.arch.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode
public class ArchitectureUpdate {
    private final String name;
    private final String milestone;
    private final List<Person> authors;
    private final List<Person> PCAs;
    private final Map<Requirement.Id, Requirement> requirements;

    @JsonProperty(value = "P2")
    private final P2 p2;

    @JsonProperty(value = "P1")
    private final P1 p1;

    @JsonProperty(value = "useful-links")
    private final List<Link> usefulLinks;

    @JsonProperty(value = "milestone-dependencies")
    private final List<MilestoneDependency> milestoneDependencies;

    @Builder
    public ArchitectureUpdate(String name, String milestone, List<Person> authors, List<Person> PCAs, Map<Requirement.Id, Requirement> requirements, P2 p2, P1 p1, List<Link> usefulLinks, List<MilestoneDependency> milestoneDependencies) {
        this.name = name;
        this.milestone = milestone;
        this.authors = copyList(authors);
        this.PCAs = copyList(PCAs);
        this.requirements = copyMap(requirements);
        this.p2 = p2;
        this.p1 = p1;
        this.usefulLinks = copyList(usefulLinks);
        this.milestoneDependencies = copyList(milestoneDependencies);
    }

    public static ArchitectureUpdate blank() {
        return new ArchitectureUpdate(
                "",
                "",
                List.of(new Person("", "")),
                List.of(new Person("", "")),
                Map.of(new Requirement.Id("ITD 1.1"), new Requirement("requirement")),
                new P2("", new Jira("", "")),
                new P1("", new Jira("", ""), ""),
                List.of(new Link("", "")),
                List.of(new MilestoneDependency("", List.of(new Link("", ""))))
        );
    }

    private <TA, TB> Map<TA, TB> copyMap(Map<TA, TB> toCopy) {
        return toCopy != null ? new LinkedHashMap<>(toCopy) : new LinkedHashMap<>();
    }

    private static <T> ArrayList<T> copyList(List<T> orig) {
        return orig != null ? new ArrayList<>(orig) : new ArrayList<>();
    }

    @Getter
    @EqualsAndHashCode
    public static class P2 {
        private final String link;
        private final Jira jira;

        @Builder
        public P2(String link, Jira jira) {
            this.link = link;
            this.jira = jira;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class P1 {
        private final String link;
        private final Jira jira;

        @JsonProperty(value = "executive-summary")
        private final String executiveSummary;

        @Builder
        public P1(String link, Jira jira, String executiveSummary) {
            this.link = link;
            this.jira = jira;
            this.executiveSummary = executiveSummary;
        }
    }

    @EqualsAndHashCode
    public static class MilestoneDependency {
        private final String description;
        private final List<Link> links;

        @Builder
        public MilestoneDependency(String description, List<Link> links) {
            this.description = description;
            this.links = links;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class Requirement {
        @Getter
        @ToString
        @EqualsAndHashCode
        @AllArgsConstructor
        public static class Id{
            @JsonValue
            private final String id;
        }

        @JsonValue
        private final String requirement;
    }
}
