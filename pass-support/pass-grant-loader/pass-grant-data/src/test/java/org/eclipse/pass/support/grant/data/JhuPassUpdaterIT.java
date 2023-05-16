/*
 * Copyright 2023 Johns Hopkins University
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
package org.eclipse.pass.support.grant.data;

import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_ABBREVIATED_ROLE;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_DIRECT_FUNDER_LOCAL_KEY;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_DIRECT_FUNDER_NAME;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_DIRECT_FUNDER_POLICY;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_GRANT_AWARD_DATE;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_GRANT_AWARD_NUMBER;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_GRANT_AWARD_STATUS;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_GRANT_END_DATE;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_GRANT_LOCAL_KEY;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_GRANT_PROJECT_NAME;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_GRANT_START_DATE;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_PRIMARY_FUNDER_LOCAL_KEY;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_PRIMARY_FUNDER_NAME;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_PRIMARY_FUNDER_POLICY;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_UPDATE_TIMESTAMP;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_USER_EMAIL;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_USER_EMPLOYEE_ID;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_USER_FIRST_NAME;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_USER_HOPKINS_ID;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_USER_INSTITUTIONAL_ID;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_USER_LAST_NAME;
import static org.eclipse.pass.support.grant.data.CoeusFieldNames.C_USER_MIDDLE_NAME;
import static org.eclipse.pass.support.grant.data.DateTimeUtil.createZonedDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.pass.support.client.PassClient;
import org.eclipse.pass.support.client.PassClientResult;
import org.eclipse.pass.support.client.PassClientSelector;
import org.eclipse.pass.support.client.RSQL;
import org.eclipse.pass.support.client.model.AwardStatus;
import org.eclipse.pass.support.client.model.Grant;
import org.eclipse.pass.support.client.model.Policy;
import org.eclipse.pass.support.client.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JhuPassUpdaterIT {

    private final String[] grantAwardNumber = {"B10000000", "B10000001", "B10000002"};
    private final String[] grantLocalKey =
        {"10000001", "10000001", "10000001"}; //all the same, different from other ITs tho
    private final String[] grantProjectName =
        {"Stupendous Research Project I", "Stupendous Research Project II", "Stupendous Research ProjectIII"};
    private final String[] grantAwardDate = {"01/01/1999", "01/01/2001", "01/01/2003"};
    private final String[] grantStartDate =
        {"07/01/2000", "07/01/2000", "07/01/2000"}; //these appear to ge the same for all awards
    private final String[] grantEndDate =
        {"06/30/2004", "06/30/2004", "06/30/2004"};//these seem to be the same for all awards
    private final String[] grantUpdateTimestamp =
        {"2006-03-11 00:00:00.0", "2010-04-05 00:00:00.0", "2015-11-11 00:00:00.0"};
    private final String[] userEmployeeId = {"31000000", "31000001", "31000002"};
    private final String[] userInstitutionalId = {"arecko1", "sclass1", "jgunn1"};
    private final String[] userHopkinsId = {"DOMNAR", "NROAD", "ROMAND"};
    private final String[] userFirstName = {"Amanda", "Skip", "Janie"};
    private final String[] userMiddleName = {"Bea", "Avery", "Gotta"};
    private final String[] userLastName = {"Reckondwith", "Class", "Gunn"};
    private final String[] userEmail = {"arecko1@jhu.edu", "sclass1@jhu.edu", "jgunn1@jhu.edu"};

    private String primaryFunderPolicyUriString;
    private String directFunderPolicyUriString;

    private final String grantIdPrefix = "johnshopkins.edu:grant:";
    //private final String funderIdPrefix = "johnshopkins.edu:funder:";
    //private final String hopkinsidPrefix = "johnshopkins.edu:hopkinsid:";
    private final String employeeidPrefix = "johnshopkins.edu:employeeid:";
    //String jhedidPrefis = "johnshopkins.edu:jhed:";

    private final PassClient passClient = PassClient.newInstance();
    private final JhuPassUpdater passUpdater = new JhuPassUpdater(passClient);
    private final PassUpdateStatistics statistics = passUpdater.getStatistics();

    @BeforeEach
    public void setup() throws IOException {
        Policy policy1 = new Policy();
        policy1.setTitle("Primary Policy 2");
        policy1.setDescription("BAA");
        passClient.createObject(policy1);
        primaryFunderPolicyUriString = policy1.getId();

        Policy policy2 = new Policy();
        policy2.setTitle("Direct Policy 2");
        policy2.setDescription("BAA");
        passClient.createObject(policy2);
        directFunderPolicyUriString = policy2.getId();
    }

    /**
     * we put an initial award for a grant into PASS, then simulate a pull of all subsequent records
     *
     * We expect to see some fields retained from the initial award, and others updated. The most
     * interesting fields are the investigator fields: all CO-PIs ever on the grant should stay on the
     * co-pi field throughout iterations. If a PI is changed, they should appear on the CO-PI field
     *
     */
    @Test
    public void processGrantIT() throws IOException {
        List<Map<String, String>> resultSet = new ArrayList<>();

        //put in initial iteration as a correct existing record - PI is Reckondwith, Co-pi is Class
        Map<String, String> piRecord0 = makeRowMap(0, 0, "P");
        Map<String, String> coPiRecord0 = makeRowMap(0, 1, "C");

        resultSet.add(piRecord0);
        resultSet.add(coPiRecord0);

        passUpdater.updatePass(resultSet, "grant");

        PassClientSelector<Grant> grantSelector = new PassClientSelector<>(Grant.class);
        grantSelector.setFilter(RSQL.equals("localKey", grantIdPrefix + grantLocalKey[2]));
        grantSelector.setInclude("primaryFunder", "directFunder", "pi", "coPis");
        PassClientResult<Grant> resultGrant = passClient.selectObjects(grantSelector);
        assertEquals(1, resultGrant.getTotal());
        Grant passGrant = resultGrant.getObjects().get(0);

        PassClientSelector<User> user0Selector = new PassClientSelector<>(User.class);
        user0Selector.setFilter(RSQL.hasMember("locatorIds", employeeidPrefix + userEmployeeId[0]));
        PassClientResult<User> resultUser0 = passClient.selectObjects(user0Selector);
        assertEquals(1, resultUser0.getTotal());
        User user0 = resultUser0.getObjects().get(0);

        PassClientSelector<User> user1Selector = new PassClientSelector<>(User.class);
        user1Selector.setFilter(RSQL.hasMember("locatorIds", employeeidPrefix + userEmployeeId[1]));
        PassClientResult<User> resultUser1 = passClient.selectObjects(user1Selector);
        assertEquals(1, resultUser1.getTotal());
        User user1 = resultUser1.getObjects().get(0);

        assertEquals(grantAwardNumber[0], passGrant.getAwardNumber());
        assertEquals(AwardStatus.ACTIVE, passGrant.getAwardStatus());
        assertEquals(grantIdPrefix + grantLocalKey[0], passGrant.getLocalKey());
        assertEquals(grantProjectName[0], passGrant.getProjectName());
        assertEquals(createZonedDateTime(grantAwardDate[0]), passGrant.getAwardDate());
        assertEquals(createZonedDateTime(grantStartDate[0]), passGrant.getStartDate());
        assertEquals(createZonedDateTime(grantEndDate[0]), passGrant.getEndDate());
        assertEquals(user0, passGrant.getPi()); //Reckondwith
        assertEquals(1, passGrant.getCoPis().size());
        assertEquals(user1, passGrant.getCoPis().get(0));

        //check statistics
        assertEquals(1, statistics.getGrantsCreated());
        assertEquals(2, statistics.getUsersCreated());
        assertEquals(1, statistics.getPisAdded());
        assertEquals(1, statistics.getCoPisAdded());

        //now simulate an incremental pull since the initial,  adjust the stored grant
        //we add a new co-pi Jones in the "1" iteration, and change the pi to Einstein in the "2" iteration
        //we drop co-pi jones in the last iteration

        Map<String, String> piRecord1 = makeRowMap(1, 0, "P");
        Map<String, String> coPiRecord1 = makeRowMap(1, 1, "C");
        Map<String, String> newCoPiRecord1 = makeRowMap(1, 2, "C");
        Map<String, String> piRecord2 = makeRowMap(2, 1, "P");

        //add in everything since the initial pull
        resultSet.clear();
        resultSet.add(piRecord1);
        resultSet.add(coPiRecord1);
        resultSet.add(newCoPiRecord1);
        resultSet.add(piRecord2);

        passUpdater.updatePass(resultSet, "grant");

        resultGrant = passClient.selectObjects(grantSelector);
        assertEquals(1, resultGrant.getTotal());
        Grant updatePassGrant = resultGrant.getObjects().get(0);

        PassClientSelector<User> user2Selector = new PassClientSelector<>(User.class);
        user2Selector.setFilter(RSQL.hasMember("locatorIds", employeeidPrefix + userEmployeeId[2]));
        PassClientResult<User> resultUser2 = passClient.selectObjects(user2Selector);
        assertEquals(1, resultUser2.getTotal());
        User user2 = resultUser2.getObjects().get(0);

        assertEquals(grantAwardNumber[0], updatePassGrant.getAwardNumber());//initial
        assertEquals(AwardStatus.ACTIVE, updatePassGrant.getAwardStatus());
        assertEquals(grantIdPrefix + grantLocalKey[0], updatePassGrant.getLocalKey());
        assertEquals(grantProjectName[0], updatePassGrant.getProjectName());//initial
        assertEquals(createZonedDateTime(grantAwardDate[0]), updatePassGrant.getAwardDate());//initial
        assertEquals(createZonedDateTime(grantStartDate[0]), updatePassGrant.getStartDate());//initial
        assertEquals(createZonedDateTime(grantEndDate[2]), updatePassGrant.getEndDate());//latest
        assertEquals(user1, updatePassGrant.getPi());//Class
        assertEquals(2, updatePassGrant.getCoPis().size());
        assertTrue(updatePassGrant.getCoPis().contains(user0));//Reckondwith
        assertTrue(updatePassGrant.getCoPis().contains(user2));//Gunn
    }

    /**
     * utility method to produce data as it would look coming from COEUS
     *
     * @param iteration the iteration of the (multi-award) grant
     * @param user      the user supplied in the record
     * @param abbrRole  the role: Pi ("P") or co-pi (C" or "K")
     * @return row map for pull record
     */
    private Map<String, String> makeRowMap(int iteration, int user, String abbrRole) {
        Map<String, String> rowMap = new HashMap<>();
        rowMap.put(C_GRANT_AWARD_NUMBER, grantAwardNumber[iteration]);
        rowMap.put(C_GRANT_AWARD_STATUS, "Active");
        rowMap.put(C_GRANT_LOCAL_KEY, grantLocalKey[iteration]);
        rowMap.put(C_GRANT_PROJECT_NAME, grantProjectName[iteration]);
        rowMap.put(C_GRANT_AWARD_DATE, grantAwardDate[iteration]);
        rowMap.put(C_GRANT_START_DATE, grantStartDate[iteration]);
        rowMap.put(C_GRANT_END_DATE, grantEndDate[iteration]);

        rowMap.put(C_DIRECT_FUNDER_LOCAL_KEY, "20000000");
        rowMap.put(C_DIRECT_FUNDER_NAME, "Enormous State University");
        rowMap.put(C_PRIMARY_FUNDER_LOCAL_KEY, "20000001");
        rowMap.put(C_PRIMARY_FUNDER_NAME, "J L Gotrocks Foundation");

        rowMap.put(C_USER_FIRST_NAME, userFirstName[user]);
        rowMap.put(C_USER_MIDDLE_NAME, userMiddleName[user]);
        rowMap.put(C_USER_LAST_NAME, userLastName[user]);
        rowMap.put(C_USER_EMAIL, userEmail[user]);
        rowMap.put(C_USER_INSTITUTIONAL_ID, userInstitutionalId[user]);
        rowMap.put(C_USER_EMPLOYEE_ID, userEmployeeId[user]);
        rowMap.put(C_USER_HOPKINS_ID, userHopkinsId[user]);

        rowMap.put(C_UPDATE_TIMESTAMP, grantUpdateTimestamp[iteration]);
        rowMap.put(C_ABBREVIATED_ROLE, abbrRole);

        rowMap.put(C_DIRECT_FUNDER_POLICY, directFunderPolicyUriString);
        rowMap.put(C_PRIMARY_FUNDER_POLICY, primaryFunderPolicyUriString);

        return rowMap;
    }

}
