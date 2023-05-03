/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.pass.object.model;

import java.util.List;
import java.util.Objects;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Abstract method that all PASS model entities inherit from. All entities can include
 * a unique ID, type, and context
 *
 * @author Karen Hanson
 */
@MappedSuperclass
public abstract class PassEntity {
    /**
     * Needed because Hibernate does not correctly implement list equality when ElementCollection annotation is used.
     *
     * @param list1 List to compare
     * @param list2 List to compare
     * @return list equality
     */
    protected static boolean listEquals(List<?> list1, List<?> list2) {
        if (list1 == list2) {
            return true;
        }

        if (list1 == null || list2 == null) {
            return false;
        }

        int size = list1.size();

        if (size != list2.size()) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            if (!Objects.equals(list1.get(i), list2.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Unique id for the resource.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * PassEntity constructor
     */
    protected PassEntity() {
    }

    /**
     * Copy constructor, this will copy the values of the object provided into the new object
     *
     * @param passEntity the PassEntity to copy
     */
    protected PassEntity(PassEntity passEntity) {
        if (passEntity == null) {
            throw new IllegalArgumentException("Null object provided. When creating a copy of "
                                               + "an object, the model object cannot be null");
        }
        this.id = passEntity.id;
    }

    /**
     * Retrieves the unique URI representing the resource.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique ID for an object. Note that when creating a new resource, this should be left
     * blank as the ID will be autogenerated and populated by the repository. When performing a
     * PUT, this URI will be used as the target resource.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PassEntity other = (PassEntity) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
