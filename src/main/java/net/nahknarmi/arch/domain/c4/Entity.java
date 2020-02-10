package net.nahknarmi.arch.domain.c4;

import java.util.List;

public interface Entity extends HasRelation, HasTag {
    String name();

    String getDescription();

    C4Path getPath();

    List<C4Relationship> getRelationships();
}
