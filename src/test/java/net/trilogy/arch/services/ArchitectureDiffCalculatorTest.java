package net.trilogy.arch.services;

import static net.trilogy.arch.ArchitectureDataStructureHelper.createPerson;
import static net.trilogy.arch.ArchitectureDataStructureHelper.createRelationship;
import static net.trilogy.arch.ArchitectureDataStructureHelper.createContainer;
import static net.trilogy.arch.ArchitectureDataStructureHelper.createComponent;
import static net.trilogy.arch.ArchitectureDataStructureHelper.createPersonWithRelationshipsTo;
import static net.trilogy.arch.ArchitectureDataStructureHelper.createSystem;
import static net.trilogy.arch.ArchitectureDataStructureHelper.emptyArch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import net.trilogy.arch.domain.ArchitectureDataStructure;
import net.trilogy.arch.domain.Diff;
import net.trilogy.arch.domain.Diff.Status;
import net.trilogy.arch.domain.c4.C4Action;
import net.trilogy.arch.domain.c4.C4Component;
import net.trilogy.arch.domain.c4.C4Container;
import net.trilogy.arch.domain.c4.C4DeploymentNode;
import net.trilogy.arch.domain.c4.C4Model;
import net.trilogy.arch.domain.c4.C4Person;
import net.trilogy.arch.domain.c4.C4Relationship;
import net.trilogy.arch.domain.c4.C4SoftwareSystem;

public class ArchitectureDiffCalculatorTest {
    @Rule
    public final ErrorCollector collector = new ErrorCollector();

    @Test
    public void shouldDiffEmptyArchitectures() {
        final ArchitectureDataStructure first = emptyArch().build();
        final ArchitectureDataStructure second = emptyArch().build();

        assertThat(ArchitectureDiffCalculator.diff(first, second), equalTo(Set.of()));
    }

    @Test
    public void shouldDiffEntities() {
        var arch = emptyArch();
        final C4Person personInFirst = createPerson("1");
        final C4Person personInSecond = createPerson("3");
        final C4Person commonPersonNameToBeChanged = createPerson("2");
        final C4Person commonPersonNameChanged = createPerson("2");
        commonPersonNameChanged.setName("new-name");
        final C4Person commonPersonNoChange = createPerson("4");

        var first = getArchWithPeople(arch, Set.of(personInFirst, commonPersonNameToBeChanged, commonPersonNoChange));
        var second = getArchWithPeople(arch, Set.of(personInSecond, commonPersonNameChanged, commonPersonNoChange));
        Set<Diff> expected = Set.of(
                new Diff(personInFirst, null),
                new Diff(null, personInSecond),
                new Diff(commonPersonNoChange, commonPersonNoChange),
                new Diff(commonPersonNameToBeChanged, commonPersonNameChanged)
        );

        var actual = ArchitectureDiffCalculator.diff(first, second);

        expected.forEach(e -> collector.checkThat(actual, hasItem(e)));
        collector.checkThat(actual.size(), equalTo(expected.size()));
    }

    @Test
    public void shouldDisregardEntityRelationships() {
        var arch = emptyArch();

        final C4Person personBefore = createPersonWithRelationshipsTo(
                "1", 
                Set.of(createRelationship("3", "2"), createRelationship("4", "7"))
        );
        final C4Person personAfter = createPersonWithRelationshipsTo(
                "1", 
                Set.of(createRelationship("2", "1"))
        );

        var first = getArchWithPeople(arch, Set.of(personBefore));
        var second = getArchWithPeople(arch, Set.of(personAfter));

        var actual = ArchitectureDiffCalculator.diff(first, second);

        collector.checkThat(
                actual.stream()
                    .filter(it -> it.getAfter() != null)
                    .filter(it -> it.getAfter().getId() == "1")
                    .findAny()
                    .orElseThrow()
                    .getStatus(), 
                equalTo(Diff.Status.NO_UPDATE)
        );
    }

    @Test
    public void shouldDiffRelationships() {
        var arch = emptyArch();
        final C4SoftwareSystem system2 = createSystem("2");
        final C4SoftwareSystem system3 = createSystem("3");

        final C4Relationship relationToSys2 = new C4Relationship("10", null, C4Action.USES, null, "2", "desc", null);
        final C4Relationship relationToSys3 = new C4Relationship("10", null, C4Action.USES, null, "3", "desc", null);
        final C4Person personWithRelationshipsToSys2 = createPersonWithRelationshipsTo("1", Set.of(relationToSys2));
        final C4Person personWithRelationshipsToSys3 = createPersonWithRelationshipsTo("1", Set.of(relationToSys3));

        var first = getArch(arch, Set.of(personWithRelationshipsToSys2), Set.of(system2, system3), Set.of(), Set.of(), Set.of());
        var second = getArch(arch, Set.of(personWithRelationshipsToSys3), Set.of(system2, system3), Set.of(), Set.of(), Set.of());

        Diff expected = new Diff(relationToSys2, relationToSys3);
        var actual = ArchitectureDiffCalculator.diff(first, second);

        collector.checkThat(actual, hasItem(expected));
    }

    @Test
    public void shouldDiffWithCorrectDescendants() {
        var arch = emptyArch();
        
        var system1 = createSystem("1");
        var container1 = createContainer("2", "1");
        var component1 = createComponent("3", "2");

        var system2 = createSystem("1");
        var container2 = createContainer("2", "1");
        var component2 = createComponent("4", "2");
        
        var first = getArch(arch, Set.of(), Set.of(system1), Set.of(container1), Set.of(component1), Set.of());
        var second = getArch(arch, Set.of(), Set.of(system2), Set.of(container2), Set.of(component2), Set.of());

        var diff = ArchitectureDiffCalculator.diff(first, second);

        collector.checkThat(
                diff.stream().filter(it -> it.getAfter() != null && it.getAfter().getId() == "1").findAny().get().getStatus(),
                equalTo(Status.NO_UPDATE_BUT_CHILDREN_UPDATED)
        );
    }

    private ArchitectureDataStructure getArchWithPeople(ArchitectureDataStructure.ArchitectureDataStructureBuilder arch, Set<C4Person> people) {
        return arch.model(
                new C4Model(people, Set.of(), Set.of(), Set.of(), Set.of()
                )
        ).build();
    }

    private ArchitectureDataStructure getArch(ArchitectureDataStructure.ArchitectureDataStructureBuilder arch,
                                              Set<C4Person> people,
                                              Set<C4SoftwareSystem> systems,
                                              Set<C4Container> containers,
                                              Set<C4Component> components,
                                              Set<C4DeploymentNode> deploymentNodes) {
        return arch.model(
                new C4Model(people, systems, containers, components, deploymentNodes)
        ).build();
    }
}
