package net.nahknarmi.arch.domain;


import com.google.common.collect.ImmutableList;
import lombok.*;
import net.nahknarmi.arch.domain.c4.C4Model;
import net.nahknarmi.arch.domain.c4.C4Tag;
import net.nahknarmi.arch.domain.c4.Entity;
import net.nahknarmi.arch.domain.c4.view.C4ViewContainer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ArchitectureDataStructure {
    @NonNull
    private String name;
    @NonNull
    private String businessUnit;
    @NonNull
    private String description;
    @NonNull
    @Builder.Default
    private List<ImportantTechnicalDecision> decisions = ImmutableList.of();
    @NonNull
    @Builder.Default
    private C4Model model = C4Model.NONE;
    @NonNull
    @Builder.Default
    private C4ViewContainer views = C4ViewContainer.NONE;

    public List<Entity> getAllWithTag(C4Tag tag) {
        return Stream.of(model.getPeople(), model.getSystems(), model.getContainers(), model.getComponents())
                .flatMap(Collection::stream)
                .filter(x -> x.getTags().contains(tag))
                .collect(Collectors.toList());
    }
}
