package net.nahknarmi.arch.domain.c4;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContainerContext {
    @NonNull
    private String name;
    @NonNull
    private String system;
    @NonNull
    private String description;
    private List<C4Tag> tags;
    private List<C4Entity> entities;
}
