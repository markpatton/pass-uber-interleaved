/*
 *
 *  * Copyright 2018 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dataconservancy.pass.support.messaging.constants;

/**
 * Deposit and Notification Services constants.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public final class Constants {

    /**
     * W3C Prov Constants
     */
    public static final class Prov {

        public static final String SOFTWARE_AGENT = "http://www.w3.org/ns/prov#SoftwareAgent";

    }

    /**
     * JMS header names used by Fedora
     */
    public static final class JmsFcrepoHeader {

        public static final String FCREPO_RESOURCE_TYPE = "org.fcrepo.jms.resourceType";

        public static final String FCREPO_EVENT_TYPE = "org.fcrepo.jms.eventType";

    }

    /**
     * Values of the {@link JmsFcrepoHeader#FCREPO_EVENT_TYPE} header.
     */
    public static final class JmsFcrepoEvent {

        public static final String RESOURCE_CREATION = "http://fedora.info/definitions/v4/event#ResourceCreation";

        public static final String RESOURCE_MODIFICATION = "http://fedora" +
                ".info/definitions/v4/event#ResourceModification";

    }

    /**
     * Values of the {@link JmsFcrepoHeader#FCREPO_RESOURCE_TYPE} header.
     */
    public static final class JmsFcrepoType {

        public static final String REPO_CONTAINER = "http://fedora.info/definitions/v4/repository#Container";

        public static final String REPO_RESOURCE = "http://fedora.info/definitions/v4/repository#Resource";

    }

    /**
     * LDP-related types as represented by URIs in RDF
     */
    public static final class LdpType {

        public static final String LDP_RDFSOURCE = "http://www.w3.org/ns/ldp#RDFSource";

        public static final String LDP_CONTAINER = "http://www.w3.org/ns/ldp#Container";

    }

    /**
     * LDP-related relationships as represented by URIs in RDF
     */
    public static final class LdpRel {

        public static final String LDP_CONTAINS = "http://www.w3.org/ns/ldp#contains";
    }

    /**
     * PASS types as represented by URIs in RDF
     */
    public static final class PassType {

        public static final String SUBMISSION_RESOURCE = "http://oapass.org/ns/pass#Submission";

        public static final String SUBMISSION_EVENT_RESOURCE = "http://oapass.org/ns/pass#SubmissionEvent";

        public static final String DEPOSIT_RESOURCE = "http://oapass.org/ns/pass#Deposit";

    }

    /**
     * Names of PASS fields in the index
     */
    public static final class Indexer {

        /**
         * The field of the {@code org.dataconservancy.pass.model.Deposit} entity that carries
         * {@code Deposit#getDepositStatus() status} information.
         */
        public static final String DEPOSIT_STATUS = "depositStatus";

        /**
         * The field of the {@code org.dataconservancy.pass.model.RepositoryCopy} entity that carries
         * {@code org.dataconservancy.pass.model.RepositoryCopy#copyStatus status} information.
         */
        public static final String REPOSITORYCOPY_STATUS = "copyStatus";

    }

    /**
     * Possible values for the {@link #SWORD_STATE} predicate as represented by URIs.
     */
    public static final class SWORD {

        /**
         * The predicate representing SWORD deposit state
         */
        public static final String SWORD_STATE = "http://purl.org/net/sword/terms/state";

        /**
         * SWORD state indicating the item is archived
         */
        public static final String SWORD_STATE_ARCHIVED = "http://dspace.org/state/archived";

        /**
         * SWORD state indicating the item has been withdrawn
         */
        public static final String SWORD_STATE_WD = "http://dspace.org/state/withdrawn";

        /**
         * SWORD state indicating the item is in progress
         */
        public static final String SWORD_STATE_INPROGRESS = "http://dspace.org/state/inprogress";

        /**
         * SWORD state indicating the item is under review
         */
        public static final String SWORD_STATE_INREVIEW = "http://dspace.org/state/inreview";

    }

    /**
     * Values of JSON keys that represent identifiers.  Useful for parsing {@code id} or {@code @id} fields from
     * JSON or JSON+LD
     */
    public static final class Json {

        /**
         * JSON {@code id}
         */
        public static final String JSON_ID = "id";

        /**
         * JSON {@code @id}
         */
        public static final String JSON_AT_ID = "@id";

        /**
         * JSON {@code etag}
         */
        public static final String ETAG = "etag";

    }

}
