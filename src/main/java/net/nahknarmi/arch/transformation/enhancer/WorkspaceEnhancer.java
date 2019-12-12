package net.nahknarmi.arch.transformation.enhancer;

import com.structurizr.Workspace;
import net.nahknarmi.arch.model.ArchitectureDataStructure;

public interface WorkspaceEnhancer {
    void enhance(Workspace workspace, ArchitectureDataStructure dataStructure);
}
