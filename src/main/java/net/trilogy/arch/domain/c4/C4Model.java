package net.trilogy.arch.domain.c4;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PROTECTED;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class C4Model {
    public static final C4Model EMPTY = new C4Model();

    @NonNull
    @Setter(PROTECTED)
    private Set<C4Person> people = new TreeSet<>();
    @NonNull
    @Setter(PROTECTED)
    private Set<C4SoftwareSystem> systems = new TreeSet<>();
    @NonNull
    @Setter(PROTECTED)
    private Set<C4Container> containers = new TreeSet<>();
    @NonNull
    @Setter(PROTECTED)
    private Set<C4Component> components = new TreeSet<>();
    @NonNull
    @Setter(PROTECTED)
    private Set<C4DeploymentNode> deploymentNodes = new TreeSet<>();

    public static C4Model empty() {
        return new C4Model(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of());
    }

    private static <T extends HasIdentity> boolean noEntityWithIdExists(T entity, Set<T> set) {
        return set.stream()
                .noneMatch(e -> e.getId().equals(entity.getId()));
    }

    private static void addChildNodes(Set<C4DeploymentNode> set, C4DeploymentNode deploymentNode) {
        if (deploymentNode.getChildren() != null) {
            deploymentNode.getChildren().forEach(c -> {
                set.add(c);
                addChildNodes(set, c);
            });
        }
    }

    public C4Model addPerson(C4Person person) {
        checkArgument(!personWithNameExists(person), format("Person with name '%s' already exists.", person.getName()));
        checkArgument(noEntityWithIdExists(person, people), format("Person with path '%s' already exists.", person));

        people.add(person);

        return this;
    }

    public C4Model addSoftwareSystem(C4SoftwareSystem softwareSystem) {
        checkArgument(!systemWithNameExists(softwareSystem), format("Software System with name '%s' already exists.", softwareSystem.getName()));
        checkArgument(noEntityWithIdExists(softwareSystem, systems), format("Software System given path '%s' already exists.", softwareSystem));

        systems.add(softwareSystem);

        return this;
    }

    public C4Model addContainer(C4Container container) {
        checkArgument(systemPathExists(container.getPath()), format("System for container (%s) doesn't exist in model.", container.getPath()));

        containers.add(container);

        return this;
    }

    public C4Model addComponent(C4Component component) {
        C4Path path = component.getPath();
        checkArgument(systemPathExists(path) && containerPathExists(path), format("System or Container for component (%s) doesn't exist in model.", path));

        components.add(component);

        return this;
    }

    public C4Model addDeploymentNode(C4DeploymentNode deploymentNode) {
        deploymentNodes.add(deploymentNode);

        return this;
    }

    public Set<C4DeploymentNode> getDeploymentNodesRecursively() {
        Set<C4DeploymentNode> allNodes = new HashSet<>();

        deploymentNodes.forEach(d -> {
            allNodes.add(d);
            addChildNodes(allNodes, d);
        });

        return allNodes;
    }

    // [TODO] [TESTING] Testing gap
    public Set<Entity> allEntities() {
        return Stream.of(getSystems(), getPeople(), getComponents(), getContainers(), getDeploymentNodesRecursively())
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    // [TODO] [TESTING] Testing gap
    public Set<Tuple2<Entity, C4Relationship>> allRelationships() {
        return allEntities().stream()
                .flatMap(entity -> entity.getRelationships().stream()
                        .map(r -> Tuple.of(entity, r)))
                .collect(toSet());
    }

    public C4Person findPersonByName(String name) {
        checkNotNull(name);
        return getPeople()
                .stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to find person with name: " + name));
    }

    public Entity findByPath(C4Path path) {
        checkNotNull(path);
        return allEntities()
                .stream()
                .filter(x -> x.getPath().getPath().equals(path.getPath()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find entity with path " + path));
    }

    public Entity findEntityByReference(C4Reference reference) {
        if (null != reference.getId()) {
            String id = reference.getId();
            return findEntityById(id).orElseThrow(() -> new IllegalStateException("Could not find entity with id: " + id));
        } else if (null != reference.getAlias()) {
            return findEntityByAlias(reference.getAlias());
        } else {
            throw new IllegalStateException("Reference is missing both id and alias: " + reference);
        }
    }

    public Optional<Entity> findEntityById(String id) {
        checkNotNull(id);
        return allEntities()
                .stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }

    public Entity findEntityByAlias(String alias) {
        checkNotNull(alias);
        return allEntities()
                .stream()
                .filter(e -> null != e.getAlias() && e.getAlias().equals(alias))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find entity with alias: " + alias));
    }

    public Entity findEntityByRelationshipWith(C4Relationship relationship) {
        checkNotNull(relationship);

        Entity result;
        if (relationship.getWithId() != null) {
            String id = relationship.getWithId();
            result = findEntityById(id).orElseThrow(() -> new IllegalStateException("Could not find entity with id: " + id));
        } else if (relationship.getWithAlias() != null) {
            result = findEntityByAlias(relationship.getWithAlias());
        } else {
            throw new IllegalStateException("Relationship is missing both withId and withAlias: " + relationship);
        }

        return result;
    }

    public Optional<C4Relationship> findRelationshipById(String id) {
        checkNotNull(id);
        return allRelationships()
                .stream()
                .filter(t -> t._2().getId().equals(id))
                .findFirst()
                .map(t -> t._2);
    }

    public C4Relationship findRelationshipByAlias(String alias) {
        checkNotNull(alias);
        final var foundTuple = allRelationships()
                .stream()
                .filter(t -> t._2().getAlias().equals(alias))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find relationship with alias: " + alias));

        return foundTuple._2();
    }

    public Set<Entity> findWithTag(C4Tag tag) {
        checkNotNull(tag);
        return allEntities()
                .stream()
                .filter(x -> x.getTags().contains(tag))
                .collect(toSet());
    }

    private boolean systemPathExists(C4Path path) {
        return getSystems().stream()
                .map(Entity::getPath)
                .anyMatch(p -> p.equals(path.systemPath()));
    }

    private boolean containerPathExists(C4Path path) {
        return getContainers().stream()
                .map(Entity::getPath)
                .anyMatch(p -> p.equals(path.containerPath()));
    }

    private boolean personWithNameExists(C4Person person) {
        return getPeople().stream()
                .anyMatch(p -> p.getName().equals(person.getName()));
    }

    private boolean systemWithNameExists(C4SoftwareSystem system) {
        return getSystems().stream()
                .anyMatch(s -> s.getName().equals(system.getName()));
    }
}
