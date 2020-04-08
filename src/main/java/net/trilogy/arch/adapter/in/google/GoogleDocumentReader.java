package net.trilogy.arch.adapter.in.google;

import net.trilogy.arch.domain.ArchitectureUpdate;
import net.trilogy.arch.domain.ArchitectureUpdate.P1;
import net.trilogy.arch.domain.ArchitectureUpdate.P2;
import net.trilogy.arch.domain.Jira;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class GoogleDocumentReader {
    private GoogleDocsApiInterface api;

    public GoogleDocumentReader(GoogleDocsApiInterface api) {
        this.api = api;
    }

    public ArchitectureUpdate load(String url) throws IOException {
        var response = api.fetch(url);

        if (isEmpty(response)) {
            return ArchitectureUpdate.blank();
        }

        GoogleDocsJsonParser jsonParser = new GoogleDocsJsonParser(response.asJson());

        return ArchitectureUpdate.builder()
                .milestone(jsonParser.getMilestone().orElse(""))
                .p1(extractP1(jsonParser, url))
                .p2(extractP2(jsonParser))
                .requirements(extractDecisions(jsonParser))
                .build();
    }

    private Map<ArchitectureUpdate.Requirement.Id, ArchitectureUpdate.Requirement> extractDecisions(GoogleDocsJsonParser jsonParser) {
        var map = new HashMap<ArchitectureUpdate.Requirement.Id, ArchitectureUpdate.Requirement>();
        jsonParser.getDecisions().forEach(decisionString -> {
            String[] split = decisionString.split("-", 2);
            if(split.length == 2) {
                var id = new ArchitectureUpdate.Requirement.Id(split[0].trim());
                var requirement = new ArchitectureUpdate.Requirement(split[1].trim());
                map.put(id, requirement);
            }
        });
        return map;
    }

    private P2 extractP2(GoogleDocsJsonParser jsonParser) {
        return P2.builder()
                .link(jsonParser.getP2Link().orElse(""))
                .build();
    }

    private P1 extractP1(GoogleDocsJsonParser jsonParser, String url) {
        return P1.builder()
                .link(url)
                .executiveSummary(jsonParser.getExecutiveSummary().orElse(""))
                .jira(new Jira(
                                jsonParser.getP1JiraTicket().orElse(""),
                                jsonParser.getP1JiraLink().orElse("")
                        )
                ).build();
    }


    private boolean isEmpty(GoogleDocsApiInterface.Response response) {
        return !response.asJson().hasNonNull("body");
    }

}
