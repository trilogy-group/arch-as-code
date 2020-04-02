package net.trilogy.arch.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Jira {
    private final String ticket;
    private final String link;

    public Jira(String ticket, String link) {
        this.ticket = ticket;
        this.link = link;
    }
}
