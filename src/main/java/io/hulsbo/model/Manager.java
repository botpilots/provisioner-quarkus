package io.hulsbo.model;

import com.fasterxml.jackson.databind.ser.Serializers;
import java.util.UUID;
import io.hulsbo.util.model.baseclass.ChildWrapper;

import java.lang.reflect.InaccessibleObjectException;
import java.util.*;
import java.util.stream.Collectors;

public class Manager {
    static final Map<UUID, BaseClass> baseClassIndex = new HashMap<>();
    static final Map<UUID, CrewMember> crewMemberIndex = new HashMap<>();

    static void register(UUID id, BaseClass baseClass) {
        baseClassIndex.put(id, baseClass);
    }
    static void register(UUID id, CrewMember crewMember) {
        crewMemberIndex.put(id, crewMember);
    }
    public static Set<BaseClass> findParents(UUID id) {
        Set<UUID> keys = baseClassIndex.keySet();
        Set<BaseClass> parents = new HashSet<>();

        for (UUID key : keys) {
            ChildWrapper elementOwnChild = baseClassIndex.get(key).childMap.get(id);

            if (elementOwnChild != null) {
                parents.add(baseClassIndex.get(key));
            }
        }
        return parents;
    }


//    TODO: Add parameter stating what Class is expected, or make getter for each one.
    public static BaseClass getBaseClass(UUID id) {
        return baseClassIndex.get(id);
    }

    public static CrewMember getCrewMember(UUID id) {
        return crewMemberIndex.get(id);
    }


    public static String removeBaseClassObject(UUID id) {

        BaseClass baseClass = baseClassIndex.remove(id);

        if (baseClass != null) {
            return baseClass.getClass().getSimpleName() + " \"" +  baseClass.getName() + "\" " + " was successfully removed from index.";
        } else {
            throw new InaccessibleObjectException("Object with id " + id + " could not be found in baseClassIndex. Already deleted?");
        }
    }

    static String removeCrewMember(UUID id) {


        CrewMember crewMember = crewMemberIndex.remove(id);

        if (crewMember != null) {
            return crewMember.getName() + " was successfully removed from index.";
        } else {
            throw new InaccessibleObjectException("CrewMember with id " + id + " could not be found in crewMemberIndex. Already deleted?");
        }
    }

    public static List<Adventure> getAllAdventures() {
        return baseClassIndex.values().stream()
                .filter(obj -> obj instanceof Adventure)
                .map(obj -> (Adventure) obj)
                .sorted(Comparator.comparing(Adventure::getCreationTime))
                .collect(Collectors.toList());
    }

    public static <T extends BaseClass> List<T> getAllOf(Class<T> subclass) {
        return baseClassIndex.values().stream()
                .filter(subclass::isInstance)
                .sorted(Comparator.comparing(BaseClass::getCreationTime))
                .map(subclass::cast)
                .collect(Collectors.toList());
    }


}

