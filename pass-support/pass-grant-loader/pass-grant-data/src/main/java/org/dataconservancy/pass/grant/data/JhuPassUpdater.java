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

package org.dataconservancy.pass.grant.data;

import static org.dataconservancy.pass.grant.data.CoeusFieldNames.C_USER_EMAIL;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.C_USER_EMPLOYEE_ID;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.C_USER_FIRST_NAME;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.C_USER_HOPKINS_ID;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.C_USER_INSTITUTIONAL_ID;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.C_USER_LAST_NAME;
import static org.dataconservancy.pass.grant.data.CoeusFieldNames.C_USER_MIDDLE_NAME;

import java.util.Map;

import org.eclipse.pass.support.client.PassClient;
import org.eclipse.pass.support.client.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for taking the Set of Maps derived from the ResultSet from the database query and
 * constructing a corresponding Collection of Grant or User objects, which it then sends to PASS to update.
 *
 * @author jrm@jhu.edu
 */

public class JhuPassUpdater extends DefaultPassUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(JhuPassUpdater.class);
    private static final String DOMAIN = "johnshopkins.edu";
    private static final String EMPLOYEE_ID_TYPE = "employeeid";
    private static final String HOPKINS_ID_TYPE = "hopkinsid";
    private static final String JHED_ID_TYPE = "jhed";

    public JhuPassUpdater(PassClient passClient) {
        super(new CoeusPassEntityUtil(), passClient);
        super.setDomain(DOMAIN);
    }

    public JhuPassUpdater() {
        super(new CoeusPassEntityUtil());
        super.setDomain(DOMAIN);
    }

    @Override
    User buildUser(Map<String, String> rowMap) {
        User user = new User();
        user.setFirstName(rowMap.get(C_USER_FIRST_NAME));
        if (rowMap.containsKey(C_USER_MIDDLE_NAME)) {
            user.setMiddleName(rowMap.get(C_USER_MIDDLE_NAME));
        }
        user.setLastName(rowMap.get(C_USER_LAST_NAME));
        user.setDisplayName(rowMap.get(C_USER_FIRST_NAME) + " " + rowMap.get(C_USER_LAST_NAME));
        user.setEmail(rowMap.get(C_USER_EMAIL));
        String employeeId = rowMap.get(C_USER_EMPLOYEE_ID);
        String hopkinsId = null;
        if (rowMap.containsKey(C_USER_HOPKINS_ID)) {
            hopkinsId = rowMap.get(C_USER_HOPKINS_ID);
        }
        String jhedId = null;
        if (rowMap.get(C_USER_INSTITUTIONAL_ID) != null) {
            jhedId = rowMap.get(C_USER_INSTITUTIONAL_ID).toLowerCase();
        }
        //Build the List of locatorIds - put the most reliable ids first
        if (employeeId != null) {
            user.getLocatorIds().add(new Identifier(DOMAIN, EMPLOYEE_ID_TYPE, employeeId).serialize());
        }
        if (hopkinsId != null) {
            user.getLocatorIds().add(new Identifier(DOMAIN, HOPKINS_ID_TYPE, hopkinsId).serialize());
        }
        if (jhedId != null) {
            user.getLocatorIds().add(new Identifier(DOMAIN, JHED_ID_TYPE, jhedId).serialize());
        }
        user.getRoles().add(User.Role.SUBMITTER);
        LOG.debug("Built user with employee ID {}", employeeId);
        return user;
    }

}
