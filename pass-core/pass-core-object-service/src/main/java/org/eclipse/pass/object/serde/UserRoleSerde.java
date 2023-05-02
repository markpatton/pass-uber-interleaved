/*
 * Copyright 2022 Johns Hopkins University
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
package org.eclipse.pass.object.serde;

import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import org.eclipse.pass.object.model.UserRole;

/**
 * Serializer/Deserializer for UserRole. Serializes a UserRole to a String and deserializes a String to a UserRole.
 */
@ElideTypeConverter(type = UserRole.class, name = "UserRole")
public class UserRoleSerde implements Serde<String, UserRole> {

    @Override
    public UserRole deserialize(String val) {
        return UserRole.of(val);
    }

    @Override
    public String serialize(UserRole val) {
        return val.getValue();
    }
}